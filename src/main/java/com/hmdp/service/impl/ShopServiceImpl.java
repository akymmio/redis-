package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheUtils;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.DEFAULT_PAGE_SIZE;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheUtils cacheUtils;
    @Override
    public Result queryById(Long id) throws InterruptedException {
        //缓存空值解决缓存穿透
        Shop shop = cacheUtils.queryWithPassThrough(id, CACHE_SHOP_KEY, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);

        //逻辑过期解决缓存击穿
        //Shop shop =cacheUtils.queryWithlogicalExpire(id, CACHE_SHOP_KEY, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.SECONDS);

        if (shop == null) return Result.fail("");
        return Result.success(shop);
    }
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

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
/*        //判断是否需要根据坐标查询
        if(x==null || y==null){
            // 根据类型分页查询
            Page<Shop> page =query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.success(page.getRecords());
        }
        //根据地理坐标查询
        int from=(current-1)*DEFAULT_PAGE_SIZE;
        int end=current*DEFAULT_PAGE_SIZE;
        String key=SHOP_GEO_KEY+typeId;
        //查询redis
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
        );
        //实现分页
        if(results==null){ return Result.success();}
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        if(content.size()<=from){
            //没有下一页
            return Result.success();
        }
        //获取商铺id
        List<Long> ids=new ArrayList<>(content.size());
        Map<String,Distance> distanceMap=new HashMap<>(content.size());
        content.stream().skip(from).forEach(res->{
            String shopIdStr = res.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            Distance distance = res.getDistance();
            distanceMap.put(shopIdStr,distance);
        });
        *//*String idstr = StrUtil.join(",", ids);
        List<Shop> shopList =  query().in("id",ids).last("order by field(id,"+idstr+")").list();*//*
        List<Shop> shopList =  query().in("id", ids).last("order by id desc").list();
        for (Shop shop : shopList) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.success(shopList);*/
        return Result.success();
    }
}
