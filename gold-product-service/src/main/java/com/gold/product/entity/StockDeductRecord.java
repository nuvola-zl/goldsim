package com.gold.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_deduct_record")
public class StockDeductRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    /**消息体中的bizKey，orderNo:specId，MQ消息ID也是这个*/
    private String bizKey;
    private String orderNo;
    private Long specId;
    private Integer quantity;
    private LocalDateTime createTime;
}