package com.gold.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录/注册接收参数DTO
 */
@Data
public class UserLoginDTO implements Serializable {

        /**
         * 登录用户名（唯一，不可为空）
         */
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度3-50位")
        private String username;

        /**
         * 登录密码（明文，前端传，后端加密存储）
         */
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度6-32位")
        private String password;

        /**
         * 绑定手机号（登录/注册可选，注册时非空）
         */
        @NotBlank(message = "手机号不能为空")
        private String phone;
}