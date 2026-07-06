package com.gold.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Configuration
// TODO [Sentinel规则持久化] 限流和熔断规则通过 @PostConstruct 加载到内存，Sentinel Dashboard 中动态修改的规则在网关重启后丢失。生产环境应配置 Nacos 数据源持久化规则（spring.cloud.sentinel.datasource.ds1.nacos）
// TODO [服务间级联保护] Sentinel 只在网关层生效，服务间 Feign 调用无 @SentinelResource 注解。如 order-service → product-service 内部调用链无熔断保护，存在级联故障风险
public class SentinelGatewayConfig {

    @PostConstruct
    public void init() {
         initGatewayRules();
         initDegradeRules();
         initBlockHandler();
    }

    /**
     * 配置网关限流规则
     * 资源名 = 路由ID（和application.yaml里的id一致）
     */
    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 1. 用户服务：登录/注册/验证码，严格限流防暴力破解
        rules.add(new GatewayFlowRule("gold-user-service")
                .setCount(10)           // 每秒10个请求
                .setIntervalSec(1));

        // 2. 订单服务
        rules.add(new GatewayFlowRule("gold-order-service")
                .setCount(30)
                .setIntervalSec(1));

        // 3. 交易服务：买卖接口
        rules.add(new GatewayFlowRule("gold-simulation-service")
                .setCount(50)
                .setIntervalSec(1));

        // 4. 秒杀服务
        rules.add(new GatewayFlowRule("gold-flash-sale-service")
                .setCount(100)
                .setIntervalSec(1));

        // 5. 行情服务：查询频繁
        rules.add(new GatewayFlowRule("gold-market-service")
                .setCount(100)
                .setIntervalSec(1));

        // 6. 商城服务
        rules.add(new GatewayFlowRule("gold-mall-service")
                .setCount(50)
                .setIntervalSec(1));

        // 7. 文件服务：严格限制，防止刷OSS流量
        rules.add(new GatewayFlowRule("gold-file-service")
                .setCount(5)
                .setIntervalSec(1));

        // 8. 新闻服务
        rules.add(new GatewayFlowRule("gold-news-service")
                .setCount(50)
                .setIntervalSec(1));

        // 9. 管理服务
        rules.add(new GatewayFlowRule("gold-admin-service")
                .setCount(20)
                .setIntervalSec(1));

        // 10. 店铺服务
        rules.add(new GatewayFlowRule("gold-store-service")
                .setCount(30)
                .setIntervalSec(1));

        GatewayRuleManager.loadRules(rules);



        log.info("Sentinel Gateway 限流规则加载完成，共 {} 条", rules.size());
    }

    /**
     * 配置熔断降级规则
     * 当后端服务响应慢或频繁报错时，Gateway 直接返回错误，不再等待
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 1. 用户服务：慢调用比例熔断
        // 如果 5 秒内，超过 50% 的请求响应时间 > 500ms，且总请求数 >= 5，熔断 10 秒
        rules.add(new DegradeRule("gold-user-service")
                .setGrade(0)           // 0 = 慢调用比例
                .setCount(500)         // 慢调用阈值：500ms
                .setTimeWindow(10)     // 熔断时长：10秒
                .setMinRequestAmount(5) // 最小请求数（少于5个不熔断）
                .setSlowRatioThreshold(0.5) // 慢调用比例：50%
                .setStatIntervalMs(5000));  // 统计周期：5秒

        // 2. 订单服务：异常比例熔断
        // 如果 5 秒内，异常比例超过 50%，且总请求数 >= 5，熔断 10 秒
        rules.add(new DegradeRule("gold-order-service")
                .setGrade(1)           // 1 = 异常比例
                .setCount(0.5)         // 异常比例阈值：50%
                .setTimeWindow(10)     // 熔断时长：10秒
                .setMinRequestAmount(5));

        // 3. 交易服务：慢调用熔断（交易接口可以容忍1秒）
        rules.add(new DegradeRule("gold-simulation-service")
                .setGrade(0)
                .setCount(1000)        // 慢调用阈值：1000ms
                .setTimeWindow(10)
                .setMinRequestAmount(5)
                .setSlowRatioThreshold(0.5)
                .setStatIntervalMs(5000));

        DegradeRuleManager.loadRules(rules);
        log.info("Sentinel 熔断降级规则加载完成，共 {} 条", rules.size());


    }

    /**
     * 自定义限流拒绝响应（返回JSON而不是空白页面）
     */
    private void initBlockHandler() {
        BlockRequestHandler handler = (exchange, throwable) -> {
            String exceptionName = throwable.getClass().getSimpleName();

            int code;
            String message;

            if (exceptionName.contains("Degrade")) {
                code = 503;
                message = "服务暂时不可用，请稍后再试";
            } else {
                code = 429;
                message = "请求过于频繁，请稍后再试";
            }

            // 【已修复】响应格式错误
            // 原问题(todo)
            // BodyInserters.fromValue(String) 会把 JSON 字符串二次序列化
            // 实际返回字符串包裹的JSON，客户端解析失败
            // 修复(2024-07-10): 使用 Map 让 Spring 自动序列化为正确 JSON
            return ServerResponse.status(code)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("code", code, "message", message));
        };

        GatewayCallbackManager.setBlockHandler(handler);
    }

}