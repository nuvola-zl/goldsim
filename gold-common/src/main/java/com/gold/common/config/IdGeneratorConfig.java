package com.gold.common.config;


import com.gold.common.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {

    @Value("${snowflake.worker-id:0}")
    private long workerId;
    // TODO [分布式ID冲突] 所有服务默认 worker-id=0，没有任何服务的 application.yaml 覆盖此值。跨服务生成的订单号/交易号等 Snowflake ID 会重复。需为每个服务分配唯一 worker-id（如 order=1, user=2, market=3 等）

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(workerId);
    }
}