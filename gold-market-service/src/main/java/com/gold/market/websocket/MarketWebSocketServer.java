package com.gold.market.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gold.api.domain.dto.MarketPriceDTO;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
//@ServerEndpoint("/ws/market/{metalType}")
public class MarketWebSocketServer {

    // key: 品种 AU/AG/PT/all   value: 该品种所有在线客户端
    private static final ConcurrentHashMap<String, Set<Session>> ONLINE_MAP = new ConcurrentHashMap<>();

    private Session session;
    private String metalType;


    @OnOpen
    public void onOpen(Session session, @PathParam("metalType") String metalType) {
        this.session = session;
        this.metalType = metalType;
        ONLINE_MAP.computeIfAbsent(metalType, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("客户端连接成功，品种：{}，当前在线总连接数：{}", metalType, getTotalOnline());
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("收到品种{}客户端消息：{}", metalType, message);
    }

    @OnClose
    public void onClose() {
        Set<Session> sessionSet = ONLINE_MAP.get(metalType);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                ONLINE_MAP.remove(metalType);
            }
        }
        log.info("客户端断开，品种：{}，剩余总连接：{}", metalType, getTotalOnline());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket连接异常，品种：{}", metalType, error);
    }

    /**
     * 广播 TICK 行情
     * 推送给目标品种 + 同时推送给订阅了 "all" 的客户端
     */
    public static void sendMarketData(String targetMetalType, MarketPriceDTO dto) {
        // 1. 推给目标品种（如 AU）
        pushToMetal(targetMetalType, dto);

        // 2. 额外推给 all（避免自己推自己时重复）
        if (!"all".equals(targetMetalType)) {
            pushToMetal("all", dto);
        }
    }

    /**
     * 广播原始 JSON（KLINE 等）
     */
    public static void sendRaw(String targetMetalType, String json) {
        doSendRaw(targetMetalType, json);

        if (!"all".equals(targetMetalType)) {
            doSendRaw("all", json);
        }
    }

    // ========== 私有推送方法 ==========

    private static void pushToMetal(String targetMetalType, MarketPriceDTO dto) {
        Set<Session> sessionSet = ONLINE_MAP.get(targetMetalType);
        if (sessionSet == null || sessionSet.isEmpty()) {
            return;
        }
        JSONObject json = (JSONObject) JSON.toJSON(dto);
        json.put("msgType", "TICK");
        String text = json.toJSONString();

        for (Session s : sessionSet) {
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(text);
                } catch (IOException e) {
                    log.error("推送行情失败", e);
                }
            }
        }
    }

    private static void doSendRaw(String targetMetalType, String json) {
        Set<Session> sessionSet = ONLINE_MAP.get(targetMetalType);
        if (sessionSet == null || sessionSet.isEmpty()) {
            return;
        }
        for (Session s : sessionSet) {
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    log.error("推送原始消息失败", e);
                }
            }
        }
    }

    private int getTotalOnline() {
        return ONLINE_MAP.values().stream().mapToInt(Set::size).sum();
    }

    /**
     * WebSocket心跳保活：向所有在线客户端发送PING消息
     * 防止非交易时段因空闲超时断开连接
     */
    public static void sendPing() {
        JSONObject ping = new JSONObject();
        ping.put("msgType", "PING");
        ping.put("timestamp", System.currentTimeMillis());
        String text = ping.toJSONString();

        for (Set<Session> sessionSet : ONLINE_MAP.values()) {
            for (Session s : sessionSet) {
                if (s.isOpen()) {
                    try {
                        s.getBasicRemote().sendText(text);
                    } catch (IOException e) {
                        // 连接已断开，忽略
                    }
                }
            }
        }
    }
}