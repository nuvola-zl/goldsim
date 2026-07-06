package com.gold.mallApi.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;


import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityAddDTO {

    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100, message = "活动名称长度不能超过100")
    private String activityName;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotEmpty(message = "活动商品不能为空")
    @Valid
    private List<ActivityGoodsAddDTO> goodsList;
}