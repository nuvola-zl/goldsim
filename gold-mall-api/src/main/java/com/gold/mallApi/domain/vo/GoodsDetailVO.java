package com.gold.mallApi.domain.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GoodsDetailVO {
    private Long goodsId;
    private String goodsName;
    private String category;
    private String metalType;
    private StoreInfo storeInfo;
    private List<SpecDetailVO> specList;

    // ==================== 【核心改动】前端区分展示 ====================
    // specList[i].activityInfo  -> 当前进行中的活动（用户现在就能以活动价购买）
    // upcomingActivities        -> 该商品下即将开始的活动（前端展示"倒计时/敬请期待"）
    // 注意：进行中的活动挂在规格上，因为活动是绑定到具体克重规格的；
    //       即将开始的活动挂在商品上，作为整品的预告。
    // =================================================================
    private List<ActivityInfo> upcomingActivities;

    @Data
    @Builder
    public static class StoreInfo {
        private Long storeId;
        private String storeName;
        private String storeAddress;
        private String phone;
        private String businessHours;
        private BigDecimal rating;
    }

    @Data
    @Builder
    public static class SpecDetailVO {
        private Long specId;
        private BigDecimal weight;
        private BigDecimal craftFee;
        private BigDecimal pricePerGram;   // 展示参考金价（管理端缓存，仅供参考）
        private BigDecimal totalPrice;     // 展示参考总价（管理端缓存，仅供参考）
        private Integer stock;
        private Integer frozenStock;
        private Integer saleStatus;
        private String coverImage;
        private List<String> detailImages;
        private String description;

        // 该规格当前进行中的活动（可直接购买，null表示无活动）
        private ActivityInfo activityInfo;
    }

    @Data
    @Builder
    public static class ActivityInfo {
        private Long activityId;
        private String activityName;
        private Long specId;               // 关联的规格ID
        private BigDecimal weight;         // 冗余克重，方便前端展示
        private BigDecimal originalPrice;
        private BigDecimal activityPrice;
        private Integer activityStock;     // 活动剩余库存（activityStock - soldCount）
        private Integer soldCount;
        private Integer limitPerUser;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer status;            // 0:待开始  1:进行中  2:已结束
    }
}