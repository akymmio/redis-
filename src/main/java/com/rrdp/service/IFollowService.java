package com.rrdp.service;

import com.rrdp.dto.Result;
import com.rrdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IFollowService extends IService<Follow> {

    Result follow(Long id, Boolean isFollow);

    Result isFollow(Long id);

    Result followCommon(Long id);
}
