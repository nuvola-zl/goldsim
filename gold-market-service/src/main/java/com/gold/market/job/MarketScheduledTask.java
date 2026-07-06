package com.gold.market.job;

import com.gold.market.service.MarketPriceService;
import com.gold.market.websocket.MarketWebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 行情定时任务
 * 交易时段（9:00-17:59）每分钟执行心跳，18:00起停止
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketScheduledTask {

    private final MarketPriceService marketPriceService;

    /**
     * 交易时段每分钟心跳：校准 → 波动 → K线 → 推送
     * cron: 9:00-17:59 每分钟执行，18:00 起停止
     */
    @Scheduled(cron = "0 * 9-17 * * *")
    public void heartbeatTask() {
        marketPriceService.heartbeat();
    }

    /**
     * WebSocket心跳保活：每30秒向所有客户端发送PING
     * 防止非交易时段因网络中间件空闲超时断开连接
     */
    @Scheduled(fixedRate = 30000)
    public void wsPingTask() {
        MarketWebSocketServer.sendPing();
    }
}