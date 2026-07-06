package com.gold.mallApi.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GoodsAddDTO {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过100")
    private String goodsName;

    @NotBlank(message = "商品分类不能为空")
    private String category;

    @NotBlank(message = "金属类型不能为空")
    private String metalType;

    private Long storeId;
    private Integer sort;

    @NotEmpty(message = "规格列表不能为空")
    @Valid
    private List<SpecAddDTO> specList;  // 直接复用 SpecAddDTO，字段完全对齐
}