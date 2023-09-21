package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * 封装缓存工具类
 */
@Slf4j
@Component
public class CacheUtils {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 存入redis
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    /**
     * 加入逻辑过期字段
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void setLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 缓存击穿
     * @param id
     * @param keyPreix
     * @param type
     * @param dbFallBack
     * @param time
     * @param unit
     * @return
     * @param <R>
     * @param <ID>
     */
    public <R,ID> R queryWithPassThrough(ID id, String keyPreix, Class<R> type, Function<ID,R> dbFallBack,Long time, TimeUnit unit){
        String key =CACHE_SHOP_KEY + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(json,type);
        }
        // 判断命中的是否是空值
        if (json != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.不存在，根据id查询数据库
        R r=dbFallBack.apply(id);
        // 5.不存在，返回错误
        if (r== null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 6.存在，写入redis
        this.set(key,r,time,unit);
        return r;
    }

    //缓存重建的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    /**
     * 获取锁
     * @param key
     * @return
     */
    private boolean lock(String key) {
        //setnx
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(b);//防止拆箱，空指针异常
    }
    /**
     * 释放锁
     * @param key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     *缓存击穿-逻辑过期实现
     * @param id
     * @param keyPrefix
     * @param type
     * @param dbFallBack 方法引用
     * @param time
     * @param unit
     * @return
     * @param <R>
     * @param <ID>
     */
    public <R,ID> R queryWithlogicalExpire(ID id,String keyPrefix,Class<R> type, Function<ID,R> dbFallBack,Long time,TimeUnit unit){
        String key =keyPrefix + id;
        //从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //判断是否存在
        if (StrUtil.isBlank(shopJson)) {
            //未命中，返回
            return null;
        }
        //命中，将data反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        R r = JSONUtil.toBean((JSONObject)redisData.getData(), type); //?
        LocalDateTime expireTime = redisData.getExpireTime();

        //判断expire字段是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期，返回店铺信息
            return r;
        }
        //已过期，缓存重建
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        //获取锁成功或失败
        boolean getlock = lock(lockKey);
        if (getlock) {
            //成功，开启新线程，重建缓存
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //查询数据库
                    R r1=dbFallBack.apply(id);
                    //写入redis
                    this.setLogicalExpire(key,r1,time,unit);
                } finally {
                    //释放锁
                    unlock(lockKey);
                }
            });
        }
        return r;
    }


}
