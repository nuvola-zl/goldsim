package com.gold.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// TODO [服务注册] 缺少 @EnableDiscoveryClient。虽然 Spring Cloud 2023.0 可能自动发现，但不可靠。项目中 6 个服务有注解、6 个缺失，应统一添加确保向 Nacos 注册
@ComponentScan(basePackages = {"com.gold.common", "com.gold.order"})
@EnableFeignClients(basePackages = "com.gold.api.feign")
@MapperScan("com.gold.order.mapper")
@EnableScheduling
public class GoldOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoldOrderApplication.class, args);
    }
}