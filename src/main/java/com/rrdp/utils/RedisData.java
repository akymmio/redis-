package com.rrdp.utils;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
public class RedisData{
    //逻辑过期时间
    private LocalDateTime expireTime;
    //存储数据
    @Getter
    private Object data;
}
