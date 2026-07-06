package com.gold.mallApi.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityAdminListVO {
    private Long activityId;
    private String activityName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Integer goodsCount;  // 关联商品数
    private Integer totalStock;  // 活动总库存
    private Integer totalSold;   // 总售出
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}