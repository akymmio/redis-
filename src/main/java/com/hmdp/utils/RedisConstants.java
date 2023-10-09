package com.hmdp.utils;

/**
 * redis中使用到的常量
 */
public class RedisConstants {
    public static final String LOGIN_CODE_PREFIX="login:phone:";//验证码key
    public static final Long LOGIN_CODE_TTL=2L;//验证码TTL

    public static final String LOGIN_TOKEN="login:token:";//token的key

    public static final Long LOGIN_USER_TTL = 3000L;//token存活时间

    public static final String CACHE_SHOP_KEY="cache:shop:";//店铺key
    public static final Long CACHE_SHOP_TTL=30L;//cache的TTL
    public static final Long CACHE_NULL_TTL=30L;//空值的TTL
    public static final String LIST_SHOP_KEY="list:shop:";//店铺类型key
    public static final String LOCK_SHOP_KEY="lock:shop:";//店铺锁
    public static final Long LOCK_SHOP_TTL=30L;//店铺锁ttl
    public static final String SECKILL_STOCK_KEY="seckill:stock:";//秒杀卷key
    public static final String BLOG_LIKED_KEY="blog:liked:";//博客点赞key
    public static final String FOLLOW_BOX_KEY="follow:box:";//用户收件箱key
    public static final String SHOP_GEO_KEY="shop:geo:";//用户收件箱key





}
