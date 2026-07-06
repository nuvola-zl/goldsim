package com.gold.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * 【已修复】Bean 冲突风险
 *
 * 原问题(todo):
 * - gold-simulation-service 等模块依赖了 redisson-spring-boot-starter
 * - Starter 自动创建 RedissonClient，与手动 Config 冲突
 * - 两套机制可能导致 Bean 冲突、行为不一致
 *
 * 修复内容(2024-07-10):
 * - gold-common 中 redisson-spring-boot-starter 改为 redisson（核心库）
 * - 统一使用手动 Config 创建 RedissonClient
 * - 所有服务行为一致，配置集中管理
 */
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        
        config.useSingleServer()
                .setAddress(address)
                .setPassword(password.isEmpty() ? null : password)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(10)
                .setConnectionPoolSize(64)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(3000);

        return Redisson.create(config);
    }
}