package com.hmdp.utils;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
public class RedisData{
    private LocalDateTime expireTime;//逻辑过期时间
    @Getter
    private Object data;//存储数据
}
