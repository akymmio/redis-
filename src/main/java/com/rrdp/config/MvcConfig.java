package com.rrdp.config;

import com.rrdp.utils.LoginInterceptor;
import com.rrdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * 拦截器
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry){
        //拦截器，刷新token时间
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).order(0);

        //登录拦截器
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/shop/**",
                        "/upload/**",
                        "/blog/hot",
                        "/shop-type/**",
                        "/voucher/**",
                        "/user/code",
                        "/user/login"
                ).order(1);
    }

}
