package com.gold.user.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@Builder
@TableName("user")
public class User {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录用户名（唯一）
     */
    private String username;

    /**
     * BCrypt加密后的密码
     */
    private String password;

    /**
     * 绑定手机号（唯一）
     */
    private String phone;

    /**
     * 头像图片地址url
     */
    private String avatar;

    /**
     * 用户真实姓名
     */
    private String realName;

    /**
     * 身份证号码
     */
    private String idCard;

    /**
     * 是否实名 0=未实名 1=已实名
     */
    private Integer realNameVerified;


    /**
     * 支付宝账号（实物商城收款/支付账号）
     */
    private String alipayAccount;

    /**
     * 支付宝实物账户余额，可用于商城实物兑换
     */
    private BigDecimal realBalance;

    /**
     * 用户状态 0=禁用 1=正常
     */
    private Integer status;

    /**
     * 用户角色 0-普通用户 1-商家 2-管理员
     */
    private Integer role;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间，自动更新
     */
    private LocalDateTime updateTime;
}