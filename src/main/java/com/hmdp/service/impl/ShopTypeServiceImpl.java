package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LIST_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询首页店铺分类信息
     * @return
     */
    @Override
    public Result queryShopList() {
        //从redis中查询店铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(LIST_SHOP_KEY);
        //命中，返回
        if(StrUtil.isNotBlank(shopJson)){
            List<ShopType> shopList= JSONUtil.toList(shopJson,ShopType.class);
            return Result.success(shopList);
        }
        //未命中，查询数据库
        List<ShopType> shopList=query().orderByAsc("sort").list();
        //不存在
        if(CollectionUtils.isEmpty(shopList)){
            return Result.fail("");
        }
        //存在，写入redis
        stringRedisTemplate.opsForValue().set(LIST_SHOP_KEY,JSONUtil.toJsonStr(shopList));
        return Result.success(shopList);
    }
//    @Override
//    public Result queryShopList() {
//        // 1. 从redis中查询商铺类型列表
//        String jsonArray = stringRedisTemplate.opsForValue().get("shop-type");
//        // json转list
//        List<ShopType> jsonList = JSONUtil.toList(jsonArray,ShopType.class);
//        System.out.println("json"+jsonList);
//        // 2. 命中，返回redis中商铺类型信息
//        if (!CollectionUtils.isEmpty(jsonList)) {
//            return Result.success(jsonList);
//        }
//        // 3. 未命中，从数据库中查询商铺类型,并根据sort排序
//        List<ShopType> shopTypesByMysql = query().orderByAsc("sort").list();
//        System.out.println("mysql"+shopTypesByMysql);
//        // 4. 将商铺类型存入到redis中
//        stringRedisTemplate.opsForValue().set("shop-type",JSONUtil.toJsonStr(shopTypesByMysql));
//        // 5. 返回数据库中商铺类型信息
//        return Result.success(shopTypesByMysql);
//    }
}
