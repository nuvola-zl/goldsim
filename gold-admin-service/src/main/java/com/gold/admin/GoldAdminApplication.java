package com.gold.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(
        // 【已修复】排除 DataSource 自动配置
        // 原问题(todo):
        //  admin 模块无数据库依赖，但 gold-common 传递了 MyBatis-Plus，
        //  启动时 DataSourceAutoConfiguration 尝试自动配置数据源，
        //  找不到 spring.datasource.url 导致启动失败
        // 修复(2024-07-10): 显式排除 DataSourceAutoConfiguration，阻止数据源自动装配
        exclude = {DataSourceAutoConfiguration.class}
)
@EnableFeignClients(basePackages = "com.gold.api.feign")
@ComponentScan("com.gold.common")
public class GoldAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoldAdminApplication.class, args);
    }
}