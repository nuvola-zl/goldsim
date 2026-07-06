package com.gold.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mall_activity")
public class MallActivity {
    /**
     * 活动自增主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 活动名称，例：618黄金限时抢购
     */
    private String activityName;

    /**
     * 活动开始时间
     */
    private LocalDateTime startTime;

    /**
     * 活动结束时间
     */
    private LocalDateTime endTime;

    /**
     * 活动状态
     * 0：待开始
     * 1：进行中
     * 2：已结束
     * 3：已取消
     */
    private Integer status;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;

    /**
     * 记录更新时间，数据修改自动刷新
     */
    private LocalDateTime updateTime;
}