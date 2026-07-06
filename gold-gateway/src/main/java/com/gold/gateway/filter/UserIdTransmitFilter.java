package com.gold.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class UserIdTransmitFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_MERCHANT_ID = "X-Merchant-Id";

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/user/register",
            "/api/user/login",
            "/api/user/captcha",
            "/api/user/merchant/login",
            "/api/market/price/",
            "/api/news/list",
            "/api/news/detail/",
            "/api/store/list",
            "/api/mall/goods/",
            "/api/market/line/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 白名单接口直接放行，不处理token
        for (String prefix : WHITE_LIST) {
            if (path.startsWith(prefix)) {
                return chain.filter(exchange);
            }
        }

        String token = request.getHeaders().getFirst("Authorization");
        if (token != null && !token.isBlank()) {
            Object loginId = StpUtil.getStpLogic().getLoginIdByToken(token);
            if (loginId != null) {
                String loginIdStr = loginId.toString();
                ServerHttpRequest.Builder newReq = request.mutate();
                if (loginIdStr.startsWith("M")) {
                    String merchantId = loginIdStr.substring(1);
                    newReq.header(HEADER_MERCHANT_ID, merchantId);
                } else {
                    String userId = loginIdStr.startsWith("U") ? loginIdStr.substring(1) : loginIdStr;
                    newReq.header(HEADER_USER_ID, userId);
                }
                return chain.filter(exchange.mutate().request(newReq.build()).build());
            }
        }
        return chain.filter(exchange);
    }

    // 改这里！优先级调低，让Sa鉴权先走
    @Override
    public int getOrder() {
        return 10;
    }
}