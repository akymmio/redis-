package com.rrdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * redis自增实现id
 */
@Component
public class RedisIdWorker {

    //利用redis的自增长，通过prefix区分不同的key

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final long BEGIN_TIMESTAMP=1672531200;
    //序列号位数
    private static final int COUNT_BIT=32;
    public Long generateId(String keyPrefix){
        //时间戳
        LocalDateTime now = LocalDateTime.now();
        long second = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp=second-BEGIN_TIMESTAMP;
        //序列号
            //获取当前日期
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //date防止序列号32位被用完，凭借当前时间
        long sequence = stringRedisTemplate.opsForValue().increment("inc" + keyPrefix + ":" + date);
        //拼接
        return timeStamp << COUNT_BIT | sequence;
    }

    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        //秒数
        long second=time.toEpochSecond(ZoneOffset.UTC);
        System.out.println(second);
    }
}
