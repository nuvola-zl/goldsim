package com.gold.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(
        exclude = {DataSourceAutoConfiguration.class}  // 【新增】排除数据源自动配置
)
/**
 * 【已修复】启动风险
 *
 * 原问题(todo):
 * - gold-mall-service 没有数据库
 * - gold-common 传递依赖了 mybatis-plus-spring-boot3-starter
 * - Spring Boot 自动尝试配置 DataSource，无配置时启动报错
 *
 * 修复内容(2024-07-10):
 * - 添加 exclude = {DataSourceAutoConfiguration.class}
 * - 参考 gold-file-service 的做法
 */
@EnableFeignClients(basePackages = "com.gold.api.feign")
@ComponentScan("com.gold.common")
public class GoldMallApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoldMallApplication.class, args);
    }
}