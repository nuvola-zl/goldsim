package com.gold.mallApi.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityAdminDetailVO {
    private Long activityId;
    private String activityName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<ActivityGoodsAdminVO> goodsList;
}