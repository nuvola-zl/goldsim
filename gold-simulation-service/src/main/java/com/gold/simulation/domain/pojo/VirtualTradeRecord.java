package com.gold.simulation.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模拟盘每一笔交易流水记录表
 * 不可删除、不可修改，永久留存用于资金对账、历史查询
 * request_id唯一索引，防止前端重复提交下单造成重复扣钱
 */
@Data
@Builder
@TableName("virtual_trade_record")
public class VirtualTradeRecord {

    /**
     * 流水主键自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 全局唯一请求ID（幂等防重）
     * 前端下单时生成UUID传递后端，相同request_id仅执行一次交易
     * 唯一索引，重复请求直接拦截不执行业务
     */
    private String requestId;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 交易类型
     * BUY = 买入贵金属
     * SELL = 卖出贵金属
     */
    private String tradeType;

    /**
     * 交易贵金属类型 AU/AG/PT
     */
    private String metalType;

    /**
     * 本次交易克重，保留4位小数
     * 买入=增加持仓，卖出=减少持仓
     */
    private BigDecimal amount;

    /**
     * 本次成交单价，元/克，保留2位小数
     * 取自market-service实时行情价格
     */
    private BigDecimal price;

    /**
     * 成交总金额 = 交易克重 × 成交单价
     * 未扣除手续费的原始交易额
     */
    private BigDecimal tradeValue;

    /**
     * 本次交易手续费，统一0.1%
     * 买入：总金额 × 0.001
     * 卖出：总金额 × 0.001
     */
    private BigDecimal fee;

    /**
     * 卖出已实现盈亏，买入时为null
     * 计算公式：卖出总金额 - 持仓对应成本 - 本次手续费
     * 交易完成后同步累加至virtual_account.totalProfit
     */
    private BigDecimal realizedProfit;

    /**
     * 交易执行前用户虚拟账户余额
     * 用于资金对账，出现余额错乱时逐笔核对
     */
    private BigDecimal balanceBefore;

    /**
     * 交易执行后用户虚拟账户余额
     */
    private BigDecimal balanceAfter;

    /**
     * 交易创建时间
     * 即下单成交时间，用于按日/周/月统计盈亏
     */
    private LocalDateTime createTime;
}