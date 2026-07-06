package com.gold.mallApi.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityUpdateDTO {
    private Long id;

    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100, message = "活动名称长度不能超过100")
    private String activityName;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    // 活动商品单独维护，不在这里修改
}