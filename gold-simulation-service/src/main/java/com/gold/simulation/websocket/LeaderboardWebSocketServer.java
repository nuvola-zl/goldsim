package com.gold.simulation.websocket;

import com.alibaba.fastjson.JSON;
import com.gold.simulation.domain.vo.LeaderboardVO;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
//@ServerEndpoint("/ws/leaderboard")
public class LeaderboardWebSocketServer {

    // 所有看排行榜的客户端
    private static final Set<Session> ONLINE_SESSIONS = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {
        ONLINE_SESSIONS.add(session);
        log.info("排行榜客户端连接，当前在线：{}", ONLINE_SESSIONS.size());
    }

    @OnClose
    public void onClose(Session session) {
        ONLINE_SESSIONS.remove(session);
        log.info("排行榜客户端断开，剩余在线：{}", ONLINE_SESSIONS.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("排行榜WebSocket异常", error);
    }

    /**
     * 向所有在线客户端广播最新排行榜
     */
    public static void broadcast(List<LeaderboardVO> list) {
        if (ONLINE_SESSIONS.isEmpty()) {
            return;
        }
        String json = JSON.toJSONString(list);
        for (Session s : ONLINE_SESSIONS) {
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    log.error("排行榜推送失败", e);
                }
            }
        }
    }
}