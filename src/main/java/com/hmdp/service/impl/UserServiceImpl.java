package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static cn.hutool.core.lang.Console.log;
import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.CODE_FIELD;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public Result sendCode(String phone, HttpSession session) {
        //发送短信验证码并保存验证码
        //校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        //生成验证码
        String code= RandomUtil.randomNumbers(6);

        //保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_PREFIX+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);//设置验证码时效为2分钟
        //发送验证码
        log("验证码：{}",code);

        return Result.success();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone=loginForm.getPhone();
        //校验手机号，请求是独立的，获取和登录的手机号可能不同
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        String s = stringRedisTemplate.opsForValue().get(LOGIN_CODE_PREFIX + phone);

        String loginCode = loginForm.getCode();
        if(loginCode==null || !loginCode.equals(s)){
            return Result.fail("验证码错误");
        }
        //校验成功后，查询
        User user = query().eq("phone", phone).one();
        if(user==null){
            //不存在，创建新用户
            user=creteUserByPhone(phone);
        }
        //生成token令牌，将user对象转为hashMap存储
        String token = UUID.randomUUID().toString();
        //DTO（数据传输对象）数据模型转换
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>()
                ,CopyOptions.create().setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
        //存入redis
        stringRedisTemplate.opsForHash().putAll(LOGIN_TOKEN+token,userMap);
        //设置有效期
        stringRedisTemplate.expire(LOGIN_TOKEN+token,LOGIN_USER_TTL,TimeUnit.MINUTES);
        //返回token
        return Result.success(token);
    }


    private User creteUserByPhone(String phone) {
        //创建新用户
        User user=new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));//生成随机用户名称
        //保存用户
        save(user);
        return user;
    }

    @Override
    public Result userSign() {
        UserDTO user = UserHolder.getUser();
        LocalDateTime now = LocalDateTime.now();
        //拼接key字段
        String sufKey = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key="sign:"+user.getId()+sufKey;
        int dayOfMonth = now.getDayOfMonth();
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.success();
    }

    @Override
    public Result countSign() {
        UserDTO user = UserHolder.getUser();
        LocalDateTime now = LocalDateTime.now();
        //拼接key字段
        String sufKey = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key="sign:"+user.getId()+sufKey;
        int dayOfMonth = now.getDayOfMonth();

        List<Long> res = stringRedisTemplate.opsForValue().bitField(key
                , BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0)
        );
        if(res==null || res.isEmpty()) return Result.success(0);
        Long num = res.get(0);
        if(num==null || num==0) return Result.success(0);
        int cnt=0;
        while(true){
            if((num & 1)==1){
                cnt++;
            }
            else{
                break;
            }
            num>>>=1;
        }
        return Result.success(cnt);
    }
}
