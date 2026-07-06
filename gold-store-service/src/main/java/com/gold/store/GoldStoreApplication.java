package com.gold.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
// TODO [组件扫描缺失] 与其他服务（如 GoldMallApplication, GoldAdminApplication）不同，此处缺少 @ComponentScan("com.gold.common")，可能导致 gold-common 中的 UserContextFilter 等组件未被加载，UserContext.getUserId() 始终为 null
@EnableFeignClients(basePackages = "com.gold.api.feign")  // 扫描 gold-api 的 Feign 接口
@ComponentScan(basePackages = {"com.gold.store", "com.gold.common"})
public class GoldStoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoldStoreApplication.class, args);
    }
}