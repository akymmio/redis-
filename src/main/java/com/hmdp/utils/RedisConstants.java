package com.hmdp.utils;

/**
 * redis中使用到的常量
 */
public class RedisConstants {
//    public static final String LOGIN_CODE_KEY = "login:code:";
//    public static final Long LOGIN_CODE_TTL = 2L;
//    public static final String LOGIN_USER_KEY = "login:token:";
//    public static final Long LOGIN_USER_TTL = 36000L;
//
//    public static final Long CACHE_NULL_TTL = 2L;
//
//    public static final Long CACHE_SHOP_TTL = 30L;
//    public static final String CACHE_SHOP_KEY = "cache:shop:";
//
//    public static final String LOCK_SHOP_KEY = "lock:shop:";
//    public static final Long LOCK_SHOP_TTL = 10L;
//
//    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
//    public static final String BLOG_LIKED_KEY = "blog:liked:";
//    public static final String FEED_KEY = "feed:";
//    public static final String SHOP_GEO_KEY = "shop:geo:";
//    public static final String USER_SIGN_KEY = "sign:";

    public static final String LOGIN_CODE_PREFIX="login:phone:";
    public static final Long LOGIN_CODE_TTL=2L;

    public static final String LOGIN_TOKEN="login:token:";

    public static final Long LOGIN_USER_TTL = 3000L;//token存活时间

    public static final String CACHE_SHOP_KEY="cache:shop:";
    public static final Long CACHE_SHOP_TTL=30L;//cache的有效时间
    public static final Long CACHE_NULL_TTL=30L;//空值的有效时间
    public static final String LIST_SHOP_KEY="list:shop:";
    public static final String LOCK_SHOP_KEY="lock:shop:";//店铺锁
    public static final String LOCK_SHOP_TTL="lock:shop";//店铺锁ttl
    public static final String SECKILL_STOCK_KEY="seckill:stock:";//店铺锁ttl


}
