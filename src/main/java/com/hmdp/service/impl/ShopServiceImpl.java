package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheUtils;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheUtils cacheUtils;

    @Override
    public Result queryById(Long id) throws InterruptedException {
        //预防缓存穿透
        Shop shop = cacheUtils.queryWithPassThrough(id, CACHE_SHOP_KEY, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //预防缓存击穿
        //Shop shop = queryWithMutex(id);

        //逻辑过期解决缓存击穿
        //Shop shop =cacheUtils.queryWithlogicalExpire(id, CACHE_SHOP_KEY, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.SECONDS);

        //Shop shop=queryWithlogicalExpire(id);
        if (shop == null) return Result.fail("");
        return Result.success(shop);
    }
/*
    //缓存重建的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public Shop queryWithlogicalExpire(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //判断是否存在
        if (StrUtil.isBlank(shopJson)) {
            //未命中，返回
            return null;
        }
        //命中，将data反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        //判断expire字段是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期，返回店铺信息
            return shop;
        }
        //已过期，缓存重建
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        //获取锁失败或失败
        boolean islock = lock(lockKey);
        if (islock) {
            //成功，开启新线程，重建缓存
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveShopToRedis(id, 20L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unlock(lockKey);
                }
            });
        }
        return shop;
    }*/
/*
    public Shop queryWithMutex(Long id) throws InterruptedException {
        String key = CACHE_SHOP_KEY + id;
        //从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //存在，返回
            return JSONUtil.toBean(shopJson, Shop.class);

        }
        //命中的是否为空值(穿透)
        //无值且不为null，说明是空值
        if (shopJson != null) {
            return null;
        }
        //未命中String(击穿)
        String lockkey = "lock:shop:" + id;
        Shop shop = null;
        try {
            boolean islock = lock(lockkey);
            if (!islock) {
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //不存在，查询数据库
            shop = getById(id);
            //模拟高并发
            Thread.sleep(200);

            //不存在，返回
            if (shop == null) {
                //将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            //存在，再写入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockkey);
        }
        return shop;
    }
   private boolean lock(String key) {
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);//setnx
        return BooleanUtil.isTrue(b);//防止拆箱，空指针异常
    }
    *//**
     * 释放锁
     *
     * @param key
     *//*
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }*/
/*    public void saveShopToRedis(Long id, Long expireSeconds) throws InterruptedException {
        //查询
        Shop shop = getById(id);
        Thread.sleep(200);
        //封装逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }*/

    /**
     * @param shop
     * @return
     */

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) return Result.fail("店铺id为空");
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.success();
    }
}
