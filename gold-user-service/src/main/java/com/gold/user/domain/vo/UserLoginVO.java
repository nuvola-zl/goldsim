package com.gold.user.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户登录成功返回VO
 * 携带用户基础信息 + JWT登录令牌
 */
@Data
public class UserLoginVO {
        /**
         * 主键id
         */
        private Long id;

        /**
         * 登录用户名（唯一）
         */
        private String username;

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
         * 创建时间
         */
        private LocalDateTime createTime;

        /**
         * 更新时间，自动更新
         */
        private LocalDateTime updateTime;

        /**
         * JWT登录凭证，前端请求头携带 Authorization: Bearer {token}
         */
        private String token;
}