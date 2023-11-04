package com.rrdp.service.impl;

import com.rrdp.entity.UserInfo;
import com.rrdp.mapper.UserInfoMapper;
import com.rrdp.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
