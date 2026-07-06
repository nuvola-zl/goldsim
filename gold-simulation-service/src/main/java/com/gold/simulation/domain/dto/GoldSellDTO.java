package com.gold.simulation.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoldSellDTO {
    
    @NotBlank(message = "请求号不能为空")
    private String requestId;
    
    @NotBlank(message = "品种不能为空")
    private String metalType;
    
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.0001", message = "数量必须大于0")
    private BigDecimal amount;
}