package com.gold.order.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActivityOrderCreateDTO {
    @NotBlank(message = "抢购Token不能为空")
    private String token;

    @NotNull(message = "规格ID不能为空")
    private Long specId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer buyNum;

    @NotNull(message = "门店ID不能为空")
    private Long storeId;

    private String remark;
}