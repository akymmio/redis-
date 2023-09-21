package com.hmdp.utils;

import ch.qos.logback.core.util.DefaultInvocationGate;
import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * redis分布式锁
 * 项目中不使用，改用Redisson
 */
public class RedisLock implements ILock{

    private final StringRedisTemplate stringRedisTemplate;
    private final String lockName;
    private static final String KEY_PREFIX="lock:";
    private final String ID_PREFIX= UUID.randomUUID().toString(true)+'-';

    public RedisLock(StringRedisTemplate stringRedisTemplate, String lockName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockName = lockName;
    }

    //加载lua脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static{
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));//设置脚本位置
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(Long timeOut) {
        //用线程id作为线程标识
        String threadId =ID_PREFIX+Thread.currentThread().getId();
        Boolean res= stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + lockName, threadId, timeOut, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(res);//空指针拆箱，要防止指针为空
    }

    @Override
   public void unclock() {
       //调用lua脚本
       stringRedisTemplate.execute(UNLOCK_SCRIPT
               ,Collections.singletonList(KEY_PREFIX + lockName)
               ,ID_PREFIX+Thread.currentThread().getId()
       );
   }
}
