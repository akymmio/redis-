package com.hmdp.service.impl;


import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;

import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
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

    //加载lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static{
        SECKILL_SCRIPT=new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));//设置脚本位置
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    //阻塞队列
    private BlockingQueue<VoucherOrder> orderBlockingQueue=new ArrayBlockingQueue<>(1024*1024);
    //秒杀订单处理器
    private static final ExecutorService SECKILL_ORDER_EXCUTOR = Executors.newSingleThreadExecutor();
    //内部类实现runnable
    //在类初始化后就执行
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXCUTOR.submit(new VoucherOrderHandler());
    }

    public class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while(true){
                try {
                    //获取队列中的订单消息
                    //take:获取和删除队列头部元素，等待直到有元素为止
                    VoucherOrder voucherOrder = orderBlockingQueue.take();
                    //创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (InterruptedException e) {
                    log.error("处理订单异常",e);
                }
            }
        }
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

    private IVoucherOrderService proxy;
    @Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        //执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT
                , Collections.emptyList()
                , voucherId.toString()
                , userId.toString()
        );
        assert result != null;
        int res = result.intValue();
        //判断结构，不为0，没有购买资格
        if(res!=0){
            return Result.fail(res==1?"库存不足":"同一用户不能重复下单");
        }
        //有购买资格，保存到阻塞队列
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //订单id
        Long orderId = redisIdWorker.generateId("order");
        voucherOrder.setId(orderId);
        //用户id
        voucherOrder.setUserId(userId);
        //代金卷id
        voucherOrder.setVoucherId(voucherId);
        //save(voucherOrder);
        //存入阻塞队列
        orderBlockingQueue.add(voucherOrder);
        //获取代理对象
        proxy =(IVoucherOrderService) AopContext.currentProxy();
        //返回订单id
        return Result.success(orderId);
    }

/*    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
        //判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
        //卷库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        //创建锁对象
        //RedisLock lock = new RedisLock(stringRedisTemplate, "order" + userId);
        RLock lock = redissonClient.getLock("lock:order" + userId);
        //获取锁
        boolean isLock=lock.tryLock();
        //判断是否成功获取锁
        if(!isLock){
            //获取锁失败
            return Result.fail("不允许重复下单");
        }
        try {
            //拿到代理对象
            IVoucherOrderService proxy =(IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            //释放锁
            lock.unlock();
        }
    }*/

    @Transactional
    public void  createVoucherOrder(VoucherOrder voucherOrder) {
        //查询订单
        Long userId = voucherOrder.getUserId();
        //Long userId = UserHolder.getUser().getId();
        //同一个用户加一把锁,根据id的值来锁定，但是userId每次都不一样，需要转为string
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        //是否存在
        if (count > 0) {
            log.error("统一用户只能下一单");
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
            log.error("库存不足");
            return ;
        }
        save(voucherOrder);
    }
}
