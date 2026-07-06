package com.gold.common.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 业务异常
 */
@Getter
@NoArgsConstructor
public class BusinessException extends RuntimeException {

    private String code;  // 业务错误码 (A-BB-CCC)
    private Object data;  // 附加业务数据

    // 不携带业务数据
    public BusinessException(String code, String message) {
        this(code, message, null);
    }

    // 携带业务数据
    public BusinessException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }
}