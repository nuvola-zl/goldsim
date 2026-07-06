package com.gold.simulation.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户贵金属实时持仓表
 * 记录用户当前持有某一类贵金属的总量与加权成本价
 * 唯一索引：userId + metalType，同一用户同一种金属仅一条记录
 */
@Data
@Builder
@TableName("virtual_position")
public class VirtualPosition {

    /**
     * 主键自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 贵金属类型
     * AU = 黄金
     * AG = 白银
     * PT = 铂金
     */
    private String metalType;

    /**
     * 当前持有克重，保留4位小数
     * 买入累加，卖出扣减；扣减至0可删除本条记录
     */
    private BigDecimal amount;

    /**
     * 加权平均持仓成本价，单位：元/克，保留2位小数
     * 多次分批买入时自动重新计算平均成本
     * 浮动盈亏计算公式：amount × (实时市价 - avgCostPrice)
     */
    private BigDecimal avgCostPrice;

    /**
     * 持仓创建时间
     * 用户首次买入该品种时生成记录
     */
    private LocalDateTime createTime;

    /**
     * 持仓更新时间
     * 每次买入/卖出修改持仓数量、成本价自动刷新
     */
    private LocalDateTime updateTime;
}