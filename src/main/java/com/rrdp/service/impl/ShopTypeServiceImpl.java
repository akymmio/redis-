package com.rrdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.rrdp.dto.Result;
import com.rrdp.entity.ShopType;
import com.rrdp.mapper.ShopTypeMapper;
import com.rrdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;

import static com.rrdp.utils.RedisConstants.LIST_SHOP_KEY;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询首页店铺分类信息
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
}
