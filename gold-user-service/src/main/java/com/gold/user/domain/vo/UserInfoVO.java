package com.gold.user.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String phone;
    private String avatar;
    private String realName;
    private String idCard;          // 脱敏后的身份证号
    private Integer realNameVerified;
    private String alipayAccount;
    private BigDecimal realBalance;
    private Integer status;
    private Integer role;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}