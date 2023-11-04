package com.rrdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.rrdp.dto.UserDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.rrdp.utils.RedisConstants.LOGIN_TOKEN;
import static com.rrdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 负责存入ThreadLocal和刷新token
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {


    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //验证token
    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {

        //获取请求头中的token
        String token = request.getHeader("authorization");

        //判断token是否为空
        if(StrUtil.isBlank(token)){
            return true;
        }
        //基于token获取redis中的用户
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_TOKEN + token);
        //判断用户是否存在
        if(userMap.isEmpty()){
            return true;
        }
        //将查询到的Hash数据转为userDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);//false不忽略错误

        //存在，保存到ThreadLocal
        UserHolder.saveUser(userDTO);

        //重新定义过期时间=刷新token有效期,
        stringRedisTemplate.expire(LOGIN_TOKEN+token,LOGIN_USER_TTL, TimeUnit.MINUTES);

        //放行
        return true;
    }

    //销毁
    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
