package com.gold.common.message;

import lombok.Data;
import java.io.Serializable;

@Data
public class ActivityOrderMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String token;
    private Long userId;
    private Long activityId;
    private Long specId;
    private Integer buyNum;
    private Long storeId;
    private String remark;
}