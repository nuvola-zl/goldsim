//package com.gold.common.result;
//
//import lombok.Data;
//
//import java.io.Serializable;
//
///**
// * 后端统一返回结果
// * @param <T>
// */
//@Data
//public class Result<T> implements Serializable {
//
//    private Integer code; //编码：1成功，0和其它数字为失败
//    private String msg; //错误信息
//    private T data; //数据
//
//    public static <T> Result<T> success() {
//        Result<T> result = new Result<T>();
//        result.code = 1;
//        return result;
//    }
//
//    public static <T> Result<T> success(T object) {
//        Result<T> result = new Result<T>();
//        result.data = object;
//        result.code = 1;
//        return result;
//    }
//
//    public static <T> Result<T> error(String msg) {
//        return error(0, msg);
//    }
//
//    public static <T> Result<T> error(Integer code, String msg) {
//        Result<T> result = new Result<>();
//        result.code = code;
//        result.msg = msg;
//        return result;
//    }
//
//}
package com.gold.common.result;

import lombok.Data;
import java.io.Serializable;

/**
 * 后端统一返回结果
 * @param <T>
 */
@Data
public class Result<T> implements Serializable {

    public static final String SUCCESS = "A00000";  // 成功码

    private String code;      // 错误码 (A-BB-CCC 格式)
    private String msg;       // 错误信息
    private T data;           // 正常业务主数据
    private String requestId; // 链路追踪ID
    private Object extraData; // 异常扩展附加数据：参数错误Map、业务附加信息

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = SUCCESS;
        return result;
    }

    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<>();
        result.data = object;
        result.code = SUCCESS;
        return result;
    }

    public static <T> Result<T> error(String code, String msg) {
        Result<T> result = new Result<>();
        result.code = code;
        result.msg = msg;
        return result;
    }

    public static <T> Result<T> error(String code, String msg, T data) {
        Result<T> result = new Result<>();
        result.code = code;
        result.msg = msg;
        result.data = data;
        return result;
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return SUCCESS.equals(this.code);
    }

    /**
     * 是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }
}