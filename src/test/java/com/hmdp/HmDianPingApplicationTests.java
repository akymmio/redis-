package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheUtils;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static java.lang.System.currentTimeMillis;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private CacheUtils cacheUtils;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Test
    void testSaveShop() throws InterruptedException {
        //shopService.saveShopToRedis(1l,10L);
        Shop shop = shopService.getById(1L);
        cacheUtils.setLogicalExpire(CACHE_SHOP_KEY + 1L, shop, 20L, TimeUnit.SECONDS);
    }
    /**
     *测试id生成
     */
    ExecutorService es= Executors.newFixedThreadPool(500);
    @Test
    void idWorker() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(300);
        Runnable task=()->{
            for(int i=0;i<100;i++){
                Long l = redisIdWorker.generateId("order");
                System.out.println(l);
            }
            countDownLatch.countDown();
        };
        long begin = System.currentTimeMillis();
        for(int i=0;i<300;i++){
            es.submit(task);
        }
        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println(end-begin);
    }

}
