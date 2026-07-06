package com.gold.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存扣减失败记录表（死信队列兜底）
 *
 * 什么时候会产生记录？
 * - 库存扣减消息消费异常，进入死信队列
 * - 定时任务扫描并补偿重试
 */
@Data
@TableName("stock_deduct_fail_record")
public class StockDeductFailRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 商品规格ID */
    private Long specId;

    /** 扣减数量 */
    private Integer quantity;

    /**
     * 状态
     * 0=待处理 1=已补偿 2=已退款 3=已忽略
     */
    private Integer status;

    /** 失败原因 */
    private String failReason;

    /** 重试次数 */
    private Integer retryCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}