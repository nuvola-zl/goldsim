package com.gold.common.filter;

import com.gold.common.context.MerchantContext;
import com.gold.common.context.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 用户上下文 Filter
 * 从请求头解析 X-User-Id，保存到 ThreadLocal
 *
 * 注意：不要加 @Component，由 AutoWebConfig 统一注册
 *
 /**
 * 用户上下文 Filter
 * 从请求头解析 X-User-Id，保存到 ThreadLocal
 *
 * 注意：不要加 @Component，由 AutoWebConfig 统一注册
 *
 * 【已修复】Feign 上下文丢失
 *
 * 原问题(todo):
 * - 此 Filter 只能从 HTTP 请求头解析 X-User-Id
 * - 服务间 Feign 调用不会自动传递该头部
 * - product-service 等被调用方 UserContext.getUserId() 始终为 null
 *
 * 修复内容(2024-07-10):
 * - 新增 FeignRequestInterceptor，实现 RequestInterceptor
 * - Feign 请求时自动携带 UserContext/MerchantContext 中的 ID
 * - 被调用方 UserContextFilter 正常解析，上下文无缝传递
 */
public class UserContextFilter implements Filter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_MERCHANT_ID = "X-Merchant-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 优先判断商家
        String merchantIdStr = httpRequest.getHeader(HEADER_MERCHANT_ID);
        if (merchantIdStr != null && !merchantIdStr.isEmpty()) {
            try {
                MerchantContext.setMerchantId(Long.valueOf(merchantIdStr));
            } catch (NumberFormatException e) {
                // 忽略
            }
        }

        // 再判断用户
        String userIdStr = httpRequest.getHeader(HEADER_USER_ID);
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                UserContext.setUserId(Long.valueOf(userIdStr));
            } catch (NumberFormatException e) {
                // 忽略
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
            MerchantContext.clear();
        }
    }
}