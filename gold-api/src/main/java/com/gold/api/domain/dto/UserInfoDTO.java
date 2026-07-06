package com.gold.api.domain.dto;

import lombok.Data;

@Data
public class UserInfoDTO {
    private Long userId;
    private String username;
    private String realName;
    private String phone;
}