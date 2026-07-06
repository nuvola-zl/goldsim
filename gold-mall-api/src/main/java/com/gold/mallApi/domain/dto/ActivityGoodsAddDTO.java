package com.gold.mallApi.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


import java.math.BigDecimal;

@Data
public class ActivityGoodsAddDTO {

    @NotNull(message = "规格ID不能为空")
    private Long specId;

    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.01", message = "原价必须大于0")
    private BigDecimal originalPrice;

    @NotNull(message = "活动价不能为空")
    @DecimalMin(value = "0.01", message = "活动价必须大于0")
    private BigDecimal activityPrice;

    @NotNull(message = "活动库存不能为空")
    @Min(value = 1, message = "活动库存至少为1")
    private Integer activityStock;

    @NotNull(message = "限购数量不能为空")
    @Min(value = 1, message = "限购数量至少为1")
    private Integer limitPerUser;
}