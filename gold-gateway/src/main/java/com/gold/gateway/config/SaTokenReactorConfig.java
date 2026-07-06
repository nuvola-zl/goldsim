package com.gold.gateway.config;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.gold.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.stp.StpUtil;
import com.gold.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SaTokenReactorConfig {

    @Bean
    public SaReactorFilter saReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                .addExclude("/api/user/register")
                .addExclude("/api/user/login")
                .addExclude("/api/user/captcha")
                .addExclude("/api/market/price/**")
                .addExclude("/api/news/list")
                .addExclude("/api/news/detail/**")
                .addExclude("/api/store/list")
                .addExclude("/api/mall/goods/**")
                .addExclude("/api/merchant/login")
                .addExclude("/ws/**")
                // TODO [认证错误] 此排除路径不匹配实际的商户登录 URL。MerchantController 路径是 /merchant，网关 route 是 /api/user/** 且 StripPrefix=1，实际完整 URL 是 /api/user/merchant/login。应改为 .addExclude("/api/user/merchant/login")，否则商户登录永远返回 401
                .setAuth(obj -> {
                    StpUtil.checkLogin();
                })
                // 正确签名：只接收 Throwable，返回 Object（会被框架转成 JSON）
                .setError(e -> {
                    log.error("Token鉴权失败：{}", e.getMessage());
                    // 返回你的 Result 对象，框架自动序列化为 JSON
                    return Result.error("A00001","登录已过期，请重新登录");
                });
    }
}