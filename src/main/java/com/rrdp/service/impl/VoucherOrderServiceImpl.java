package com.rrdp.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.rrdp.dto.Result;
import com.rrdp.entity.VoucherOrder;
import com.rrdp.mapper.VoucherOrderMapper;
import com.rrdp.service.ISeckillVoucherService;
import com.rrdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rrdp.utils.RedisIdWorker;

import com.rrdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;
    private final String queueName="stream.orders";
    //加载lua脚本
    private static DefaultRedisScript<Long> SECKILL_SCRIPT;
    static{
        SECKILL_SCRIPT=new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));//设置脚本位置
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    //秒杀订单处理器
    private static ExecutorService SECKILL_ORDER_EXCUTOR = Executors.newSingleThreadExecutor();
    //内部类实现runnable
    //在类初始化后就执行
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXCUTOR.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while(true){
                try {
                    //获取消息队列中的订单消息 XREADGROUP-list
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                           Consumer.from("g1","c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    //判断获取消息是否成功
                    if(list==null || list.isEmpty()){
                        continue;
                    }
                    //下单操作
                    createOrder(list);
                } catch (RuntimeException e) {
                    log.error("处理订单异常",e);
                    //异常消息
                    handlePendingList();
                }
            }
        }
    }


    private void handlePendingList(){
        while(true){
            try {
                //获取消息队列中的订单消息 XREADGROUP-pendinglist
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(queueName, ReadOffset.from("0"))
                );
                //判断获取消息是否成功
                if(list==null || list.isEmpty()){
                    return;
                }
                //下单操作
               createOrder(list);
            } catch (RuntimeException e) {
                log.error("处理pending-list订单异常",e);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
    //解析消息中的订单，完成下单操作
    private void createOrder(List<MapRecord<String, Object, Object>> list ){
        //解析消息中的订单
        //结构为：{id,map<,>}
        MapRecord<String, Object, Object> record = list.get(0);
        Map<Object, Object> value = record.getValue();
        //将map转为voucherOrder对象
        VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
        //有消息，创建订单
        handleVoucherOrder(voucherOrder);
        //ack确认
        stringRedisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
    }
    //子线程处理订单
    public void handleVoucherOrder(VoucherOrder voucherOrder){
        //获取用户
        Long userId = voucherOrder.getUserId();
        //创建锁对象
        RLock lock = redissonClient.getLock("lock:order" + userId);
        //获取锁
        boolean isLock=lock.tryLock();
        //判断是否成功获取锁
        if(!isLock){
            //获取锁失败
            log.error("不允许重复下单");
            return;
        }
        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            //释放锁
            lock.unlock();
        }
    }

    @Transactional
    public void  createVoucherOrder(VoucherOrder voucherOrder) {
        //查询订单
        Long userId = voucherOrder.getUserId();
        //Long userId = UserHolder.getUser().getId();
        //同一个用户加一把锁,根据id的值来锁定，但是userId每次都不一样，需要转为string
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        //是否存在
        if (count > 0) {
            log.error("同一用户只能下一单");
            return ;
        }
        //库存减少(手写sql)
        boolean success = iSeckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0)
                .update();//？
        if (!success) {
            //扣减失败
            log.error("下单失败,库存不足");
            return ;
        }
        save(voucherOrder);
    }

    private IVoucherOrderService proxy;
    @Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        //订单id
        Long orderId = redisIdWorker.generateId("order");
        //执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT
                , Collections.emptyList()
                , voucherId.toString()
                , userId.toString()
                , String.valueOf(orderId)
        );
        assert result != null;
        int res = result.intValue();
        //判断结构，不为0，没有购买资格
        if(res!=0){
            return Result.fail(res==1?"库存不足":"同一用户不能重复下单");
        }
        //获取代理对象
        proxy =(IVoucherOrderService) AopContext.currentProxy();
        //返回订单id
        return Result.success(orderId);
    }
}
