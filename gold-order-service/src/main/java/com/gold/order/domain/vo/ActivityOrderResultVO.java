package com.gold.order.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ActivityOrderResultVO {
    /** 0:排队中  1:抢购成功  2:抢购失败 */
    private Integer status;
    private String orderNo;
    private String failReason;
    private BigDecimal payAmount;
    private LocalDateTime createTime;
}