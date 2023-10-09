package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheUtils;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 测试逻辑过期，添加逻辑时间字段
     * @throws InterruptedException
     */
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

    @Test
    void loadShopData(){
        List<Shop> list = shopService.list();
        //相同id分为一组
        //Map<Long,List<Shop>> map=new HashMap<>();
        Map<Long, List<Shop>> map=list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            Long typeId=entry.getKey();
            String key="shop:geo:"+typeId;
            //同类型店铺集合
            List<Shop> shopList=entry.getValue();
            /*for(Shop shop:shopList){
                //写入redis
                stringRedisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
            }*/

            //泛型为member类型,封装了point和member,先存储好一次性存入redis，而不是一条一条地写入redis
            List<RedisGeoCommands.GeoLocation<String>> locations=new ArrayList<>();
            for(Shop shop:shopList){
                //写入redis
                locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),new Point(shop.getX(),shop.getY())));
            }
            stringRedisTemplate.opsForGeo().add(key,locations);
        }
    }
}
