package com.gold.market.config;

import com.gold.market.websocket.MarketWebSocketServer;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.ServletContext;

@Slf4j
@Configuration
public class WebSocketConfig {

    @Bean
    public ApplicationRunner websocketRegisterRunner(ServletContext servletContext) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) throws Exception {
                ServerContainer container = (ServerContainer) servletContext.getAttribute(
                        "jakarta.websocket.server.ServerContainer");

                if (container == null) {
                    log.error(" ServerContainer 为空，WebSocket 无法注册");
                    return;
                }

                try {
                    ServerEndpointConfig config = ServerEndpointConfig.Builder
                            .create(MarketWebSocketServer.class, "/ws/market/{metalType}")
                            .build();
                    container.addEndpoint(config);
                    log.info("WebSocket 端点 /ws/market/{metalType} 注册成功");
                } catch (Exception e) {
                    log.error(" WebSocket 端点注册失败", e);
                }
            }
        };
    }
}