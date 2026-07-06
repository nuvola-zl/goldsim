package com.gold.common.exception;

import com.gold.common.code.ErrorCode;
import com.gold.common.result.Result;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局统一异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 获取请求追踪ID
     */
    private String getRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    // ===================== 1. 自定义业务异常 =====================
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String requestId = getRequestId();
        log.warn("业务异常 | RequestId: {} | URI: {} | Code: {} | Msg: {}",
                requestId, request.getRequestURI(), e.getCode(), e.getMessage());
        Result<Object> result = Result.error(e.getCode(), e.getMessage());
        result.setRequestId(requestId);
        // 适配新增extraData，替换原setData
        result.setExtraData(e.getData());
        return result;
    }

    // ===================== 2. 自定义系统内部异常 =====================
    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleSystemException(SystemException e, HttpServletRequest request) {
        String requestId = getRequestId();
        log.error("系统自定义异常 | RequestId: {} | URI: {} | Msg: {}",
                requestId, request.getRequestURI(), e.getMessage(), e);
        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        result.setRequestId(requestId);
        return result;
    }

    // ===================== 3. @RequestBody JSON实体参数校验 =====================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String requestId = getRequestId();
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String firstError = errors.values().iterator().next();
        log.warn("JSON参数校验异常 | RequestId: {} | URI: {} | Errors: {}",
                requestId, request.getRequestURI(), errors);
        Result<Map<String, String>> result = Result.error(ErrorCode.PARAM_ERROR, "参数验证失败: " + firstError);
        result.setRequestId(requestId);
        // 适配新增extraData，替换原setData
        result.setExtraData(errors);
        return result;
    }

    // ===================== 4. 表单提交参数校验 =====================
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String requestId = getRequestId();
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ":" + fe.getDefaultMessage())
                .collect(Collectors.joining("，"));
        log.warn("表单参数校验异常 | RequestId: {} | URI: {} | Msg: {}",
                requestId, request.getRequestURI(), msg);
        Result<Void> result = Result.error(ErrorCode.PARAM_ERROR, msg);
        result.setRequestId(requestId);
        return result;
    }

    // ===================== 5. @RequestParam / @PathVariable 单个参数校验 =====================
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        String requestId = getRequestId();
        String msg = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("路径/请求参数校验异常 | RequestId: {} | URI: {} | Msg: {}",
                requestId, request.getRequestURI(), msg);
        Result<Void> result = Result.error(ErrorCode.PARAM_ERROR, msg);
        result.setRequestId(requestId);
        return result;
    }

    // ===================== 6. JSON请求体格式错误（传非JSON、空body） =====================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleJsonReadError(HttpMessageNotReadableException e, HttpServletRequest request) {
        String requestId = getRequestId();
        log.warn("请求JSON格式错误 | RequestId: {} | URI: {} | 详情:{}",
                requestId, request.getRequestURI(), e.getMessage());
        Result<Void> result = Result.error(ErrorCode.PARAM_ERROR, "请求数据格式错误，请检查JSON参数");
        result.setRequestId(requestId);
        return result;
    }

    // ===================== 7. Feign远程调用微服务异常 =====================
    @ExceptionHandler(FeignException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> handleFeignException(FeignException e, HttpServletRequest request) {
        String requestId = getRequestId();
        log.error("Feign远程服务调用失败 | RequestId: {} | URI: {} | 状态码:{} | 异常:{}",
                requestId, request.getRequestURI(), e.status(), e.getMessage());
        Result<Void> result = Result.error(ErrorCode.SYSTEM_BUSY, "下游服务繁忙，请稍后重试");
        result.setRequestId(requestId);
        return result;
    }

    // ===================== 8. 数据库唯一索引冲突（重复提交） =====================
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        String requestId = getRequestId();
        log.warn("数据库唯一索引冲突，重复提交 | RequestId: {} | URI: {} | 详情:{}",
                requestId, request.getRequestURI(), e.getMessage());
        Result<Void> result = Result.error(ErrorCode.REQUEST_PROCESSED, "该请求已处理，请勿重复提交");
        result.setRequestId(requestId);
        return result;
    }

    // ===================== 9. 兜底：所有未捕获的未知异常 =====================
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String requestId = getRequestId();
        String ip = getClientIp(request);
        log.error("系统未知内部错误 | RequestId: {} | URI: {} | IP: {}",
                requestId, request.getRequestURI(), ip, e);
        Result<Void> result = Result.error(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
        result.setRequestId(requestId);
        return result;
    }
}