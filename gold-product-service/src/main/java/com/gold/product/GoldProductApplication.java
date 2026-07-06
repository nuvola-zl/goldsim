package com.gold.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// TODO [服务注册] 缺少 @EnableDiscoveryClient，同 gold-order-service
@EnableFeignClients(basePackages = "com.gold.api.feign")
@ComponentScan("com.gold.common")
@EnableScheduling
@MapperScan("com.gold.product.mapper")
public class GoldProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoldProductApplication.class, args);
    }
}