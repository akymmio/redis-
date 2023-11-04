package com.rrdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rrdp.dto.LoginFormDTO;
import com.rrdp.dto.Result;
import com.rrdp.entity.User;

import javax.servlet.http.HttpSession;


public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result userSign();

    Result countSign();
}
