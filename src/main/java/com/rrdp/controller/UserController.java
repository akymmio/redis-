package com.rrdp.controller;


import cn.hutool.core.bean.BeanUtil;
import com.rrdp.dto.LoginFormDTO;
import com.rrdp.dto.Result;
import com.rrdp.dto.UserDTO;
import com.rrdp.entity.User;
import com.rrdp.entity.UserInfo;
import com.rrdp.service.IUserInfoService;
import com.rrdp.service.IUserService;
import com.rrdp.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        //发送短信验证码并保存验证码
        return userService.sendCode(phone,session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        //实现登录功能
        return userService.login(loginForm,session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        //实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result me(){
        //获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.success(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.success();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.success(info);
    }

    /**
     * 根据id查询用户
     * @param id 用户id
     * @return
     */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id")Long id){
        User user = userService.getById(id);
        if(user==null){
            return Result.success();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.success(userDTO);
    }

    /**
     * 签到功能
     * @return 无
     */
    @PostMapping("/sign")
    public Result userSign(){
        return userService.userSign();
    }

    /**
     * 签名统计
     * @return 无
     */
    @GetMapping("/sign/count")
    public Result countSign(){
        return userService.countSign();
    }

}
