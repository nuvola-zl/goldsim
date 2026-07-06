package com.gold.simulation.service;

import com.gold.simulation.domain.vo.LeaderboardVO;
import com.gold.simulation.domain.vo.UserRankVO;

import java.math.BigDecimal;
import java.util.List;

public interface LeaderboardService {

    /** 更新用户排名（卖出后调用） */
    void updateUserScore(Long userId, BigDecimal totalProfit);

    /** 获取 TOP 20 榜单 */
    List<LeaderboardVO> getTop20();

    /** 获取用户个人排名（未上榜返回 null） */
    UserRankVO getUserRank(Long userId);

    /** 兜底：从数据库全量重建排行榜（Redis丢数据时调用） */
    void rebuildFromDb();
}