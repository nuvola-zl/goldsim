package com.gold.user.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("merchant")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String merchantName;

    private String contactPhone;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}