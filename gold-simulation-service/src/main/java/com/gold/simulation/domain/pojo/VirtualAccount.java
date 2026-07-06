package com.gold.simulation.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模拟盘虚拟账户表
 * 独立存放用户模拟盘资金资产，与user用户基础信息解耦
 * 每个用户仅存在一条账户记录
 */
@Data
@Builder
@TableName("virtual_account")
public class VirtualAccount {

    /**
     * 主键自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联用户ID，关联user-service用户表主键
     * 唯一索引：一个用户只能有一条虚拟账户
     */
    private Long userId;

    /**
     * 模拟盘可用虚拟余额
     * 初始注册赠送100000.00元，单位：元
     * 买入时扣减，卖出时增加，手续费同步扣除
     */
    private BigDecimal balance;

    /**
     * 累计已实现总盈亏
     * 仅卖出成交时更新：每笔卖出realized_profit累加至此
     * 正数=盈利，负数=亏损
     * 排行榜直接依据该字段排序
     */
    private BigDecimal totalProfit;

    /**
     * 模拟盘累计总手续费
     * 每一笔买卖产生的手续费累加存入
     * 用于统计用户交易成本、平台营收统计
     */
    private BigDecimal totalFee;

    /**
     * 账户创建时间
     * 用户注册完成后自动创建虚拟账户时赋值
     */
    private LocalDateTime createTime;

    /**
     * 账户更新时间
     * 余额、盈亏、手续费发生变动自动更新
     */
    private LocalDateTime updateTime;
}