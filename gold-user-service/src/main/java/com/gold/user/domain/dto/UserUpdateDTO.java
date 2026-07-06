package com.gold.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserUpdateDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String avatar;


}