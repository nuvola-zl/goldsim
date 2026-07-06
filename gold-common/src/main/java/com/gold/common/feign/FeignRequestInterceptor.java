package com.gold.common.feign;

import com.gold.common.context.MerchantContext;
import com.gold.common.context.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Feign 请求拦截器
 * 
 * 自动将 UserContext/MerchantContext 中的用户ID/商家ID
 * 携带到 Feign 请求头中，解决服务间调用上下文丢失问题
 */
@Slf4j
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 优先传递商家ID（如果有）
        Long merchantId = MerchantContext.getMerchantId();
        if (merchantId != null) {
            template.header("X-Merchant-Id", merchantId.toString());
            log.debug("Feign 请求携带商家ID: {}", merchantId);
        }

        // 传递用户ID（如果有）
        Long userId = UserContext.getUserId();
        if (userId != null) {
            template.header("X-User-Id", userId.toString());
            log.debug("Feign 请求携带用户ID: {}", userId);
        }
    }
}