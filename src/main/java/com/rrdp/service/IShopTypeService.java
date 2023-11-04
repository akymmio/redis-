package com.rrdp.service;

import com.rrdp.dto.Result;
import com.rrdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IShopTypeService extends IService<ShopType> {

    Result queryShopList();
}
