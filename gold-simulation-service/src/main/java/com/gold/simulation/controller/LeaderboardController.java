package com.gold.simulation.controller;

import com.gold.common.context.UserContext;
import com.gold.common.result.Result;
import com.gold.simulation.domain.vo.LeaderboardVO;
import com.gold.simulation.domain.vo.UserRankVO;
import com.gold.simulation.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/simulation/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /** 获取盈亏榜 TOP 20 */
    @GetMapping("/top20")
    public Result<List<LeaderboardVO>> getTop20() {
        return Result.success(leaderboardService.getTop20());
    }

    /** 获取我的排名（未上榜返回 onList=false） */
    @GetMapping("/my-rank")
    public Result<UserRankVO> getMyRank() {
        Long userId = UserContext.getUserId();
        UserRankVO vo = leaderboardService.getUserRank(userId);
        // 兜底：如果getUserRank返回null，也包装成未上榜
        if (vo == null) {
            vo = UserRankVO.builder()
                    .rank(null)
                    .totalProfit(null)
                    .onList(false)
                    .build();
        }
        return Result.success(vo);
    }

    /** 管理员：从数据库全量重建排行榜（Redis丢数据时手动触发） */
    @PostMapping("/rebuild")
    public Result<String> rebuild() {
        leaderboardService.rebuildFromDb();
        return Result.success("排行榜重建完成");
    }
}