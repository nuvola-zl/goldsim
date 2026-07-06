package com.gold.mallApi.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityShowVO {
    private Long activityId;
    private String activityName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status; // 0:待开始  1:进行中

    // 活动下的商品规格列表（简要信息）
    private List<ActivityGoodsVO> goodsList;

    @Data
    public static class ActivityGoodsVO {
        private Long specId;
        private Long goodsId;
        private String goodsName;
        private BigDecimal weight;
        private String coverImage;
        private BigDecimal originalPrice;
        private BigDecimal activityPrice;
        private Integer activityStock;
        private Integer soldCount;
        private Integer limitPerUser;
    }
}