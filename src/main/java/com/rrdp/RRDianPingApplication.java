package com.rrdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.rrdp.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)//暴露代理对象对象
public class RRDianPingApplication {
    public static void main(String[] args) {
        SpringApplication.run(RRDianPingApplication.class, args);
    }

}
