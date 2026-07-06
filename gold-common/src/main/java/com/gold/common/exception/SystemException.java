package com.gold.common.exception;

import com.gold.common.code.ErrorCode;
import lombok.Getter;

@Getter
public class SystemException extends RuntimeException {
    private final String code;  // 改成 String

    public SystemException(String msg) {
        super(msg);
        this.code = ErrorCode.SYSTEM_ERROR;  // 直接赋值字符串常量
    }

    public SystemException(String msg, Throwable cause) {
        super(msg, cause);
        this.code = ErrorCode.SYSTEM_ERROR;
    }
}