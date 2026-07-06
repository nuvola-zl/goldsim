package com.gold.order.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("mall_order")
public class MallOrder {
    /**
     * 订单自增主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务订单编号，全局唯一，对外展示、支付、退款标识
     */
    private String orderNo;

    /**
     * 下单用户id
     */
    private Long userId;

    /**
     * 订单类型
     * 1：普通购买
     * 2：活动抢购
     */
    private Integer orderType;

    /**
     * 关联抢购活动id，普通订单该字段为null
     */
    private Long activityId;

    /**
     * 优惠前订单总金额（金价+工费合计）
     */
    private BigDecimal totalAmount;

    /**
     * 订单金价总金额，所有商品金价部分相加
     */
    private BigDecimal goldAmount;

    /**
     * 订单工费总金额，所有商品工费部分相加
     */
    private BigDecimal craftFeeAmount;

    /**
     * 优惠抵扣金额，优惠券/活动减免的总价
     */
    private BigDecimal discountAmount;

    /**
     * 用户最终实际支付金额
     */
    private BigDecimal payAmount;

    /**
     * 订单状态
     * 0：待支付
     * 1：已支付
     * 2：待核销
     * 3：已核销
     * 4：已完成
     * 5：已取消
     * 6：退款中
     * 7：已退款
     */
    private Integer status;

    /**
     * 提货人姓名
     */
    private String receiverName;

    /**
     * 提货人手机号
     */
    private String receiverPhone;

    /**
     * 自提门店ID，快递订单该字段为空
     */
    private Long storeId;

    /**
     * 用户下单备注，如开票需求、配送要求、刻字备注
     */
    private String remark;

    /**
     * 用户支付完成时间
     */
    private LocalDateTime payTime;

    /**
     * 支付宝官方交易流水号，支付回调幂等校验核心字段
     */
    private String payTradeNo;

    /**
     * 支付渠道，默认ALIPAY_SANDBOX支付宝沙箱
     */
    private String payChannel;

    /**
     * 订单创建时间
     */
    private LocalDateTime createTime;

    /**
     * 订单更新时间，状态变更自动刷新
     */
    private LocalDateTime updateTime;
}