package com.gold.simulation.config;

import com.gold.simulation.websocket.LeaderboardWebSocketServer;
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
                    log.error(" ServerContainer 为空，排行榜 WebSocket 无法注册");
                    return;
                }

                try {
                    ServerEndpointConfig config = ServerEndpointConfig.Builder
                            .create(LeaderboardWebSocketServer.class, "/ws/leaderboard")
                            .build();
                    container.addEndpoint(config);
                    log.info(" 排行榜 WebSocket 端点 /ws/leaderboard 注册成功");
                } catch (Exception e) {
                    log.error(" 排行榜 WebSocket 端点注册失败", e);
                }
            }
        };
    }
}