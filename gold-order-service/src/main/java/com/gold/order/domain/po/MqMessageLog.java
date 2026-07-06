package com.gold.order.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("mq_message_log")
public class MqMessageLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 业务类型 */
    private String bizType;
    
    /** 业务唯一键 */
    private String bizKey;
    
    /** 交换机 */
    private String exchange;
    
    /** 路由键 */
    private String routingKey;
    
    /** 消息体JSON */
    private String payload;
    
    /** 0-待发送 1-已发送 2-成功 3-失败 */
    private Integer status;
    
    /** 重试次数 */
    private Integer retryCount;
    
    /** 最大重试次数 */
    private Integer maxRetry;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime sendTime;
}