package com.gold.simulation.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GoldBuyDTO {
    /**
     * 全局唯一请求ID（幂等防重）
     * 前端下单时生成UUID传递后端，相同request_id仅执行一次交易
     * 唯一索引，重复请求直接拦截不执行业务
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    /**
     * 交易贵金属类型 AU/AG/PT
     */
    @NotBlank(message = "品种类型不能为空")
    private String metalType;

    /**
     * 本次交易克重，保留4位小数
     * 买入=增加持仓，卖出=减少持仓
     */
    @NotNull(message = "交易数量不能为空")
    @DecimalMin(value = "0.0001", message = "交易数量必须大于0")
    private BigDecimal amount;
}
