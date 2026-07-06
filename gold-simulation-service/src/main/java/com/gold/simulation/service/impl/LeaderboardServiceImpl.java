package com.gold.simulation.service.impl;

import com.gold.simulation.domain.pojo.VirtualAccount;
import com.gold.simulation.domain.vo.LeaderboardVO;
import com.gold.simulation.domain.vo.UserRankVO;
import com.gold.simulation.mapper.VirtualAccountMapper;
import com.gold.simulation.service.LeaderboardService;
import com.gold.simulation.websocket.LeaderboardWebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final StringRedisTemplate redisTemplate;
    private final VirtualAccountMapper accountMapper;

    private static final String RANK_KEY = "sim:leaderboard:profit";
    private static final String USER_NICKNAME_HASH = "sim:user:nickname";

    @Override
    public void updateUserScore(Long userId, BigDecimal totalProfit) {
        if (userId == null || totalProfit == null) {
            return;
        }
        redisTemplate.opsForZSet().add(
                RANK_KEY,
                userId.toString(),
                totalProfit.doubleValue()
        );

        // 调用本服务的 WebSocket
        try {
            List<LeaderboardVO> top20 = getTop20();
            LeaderboardWebSocketServer.broadcast(top20);
            log.debug("排行榜WebSocket推送完成");
        } catch (Exception e) {
            log.error("排行榜WebSocket推送失败", e);
            // 推送失败不影响交易主流程
        }
    }

    @Override
    public List<LeaderboardVO> getTop20() {
        // 从高到低取前20
        Set<ZSetOperations.TypedTuple<String>> top20 = redisTemplate.opsForZSet()
                .reverseRangeWithScores(RANK_KEY, 0, 19);

        if (top20 == null || top20.isEmpty()) {
            return Collections.emptyList();
        }

        List<LeaderboardVO> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : top20) {
            Long userId = Long.valueOf(tuple.getValue());
            BigDecimal profit = BigDecimal.valueOf(tuple.getScore());

            // 从Redis Hash取昵称（用户注册时由用户服务同步写入）
            String nickname = (String) redisTemplate.opsForHash()
                    .get(USER_NICKNAME_HASH, userId.toString());

            if (nickname == null) {
                nickname = "用户" + userId;
            }

            result.add(LeaderboardVO.builder()
                    .rank(rank++)
                    .userId(userId)
                    .nickname(nickname)
                    .totalProfit(profit)
                    .build());
        }
        return result;
    }

    @Override
    public UserRankVO getUserRank(Long userId) {
        if (userId == null) {
            return null;
        }

        Long rank = redisTemplate.opsForZSet().reverseRank(RANK_KEY, userId.toString());
        Double score = redisTemplate.opsForZSet().score(RANK_KEY, userId.toString());

        if (rank == null || score == null) {
            // 未上榜：返回 onList=false，rank和profit为null
            return UserRankVO.builder()
                    .rank(null)
                    .totalProfit(null)
                    .onList(false)
                    .build();
        }

        // 20名以后算未上榜（Redis从0开始，所以rank>=20表示第21名及以后）
        boolean onList = rank < 20;

        return UserRankVO.builder()
                .rank(rank + 1) // 转成人话排名（1开始）
                .totalProfit(BigDecimal.valueOf(score))
                .onList(onList)
                .build();
    }

    @Override
    public void rebuildFromDb() {
        log.info("开始从数据库全量重建排行榜...");
        // 先清空旧榜
        redisTemplate.delete(RANK_KEY);

        // 查所有账户，按totalProfit写入ZSET
        List<VirtualAccount> accounts = accountMapper.selectList(null);
        for (VirtualAccount account : accounts) {
            if (account.getUserId() != null && account.getTotalProfit() != null) {
                redisTemplate.opsForZSet().add(
                        RANK_KEY,
                        account.getUserId().toString(),
                        account.getTotalProfit().doubleValue()
                );
            }
        }
        log.info("排行榜重建完成，共{}人", accounts.size());
    }
}