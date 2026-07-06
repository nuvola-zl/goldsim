package com.gold.user.util;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Sa-Token 登录工具类
 */
public class JwtUtil {

    /**
     * 登录生成Token
     * @param userId 用户ID
     * @return token字符串
     */
    public static String createToken(Long userId) {
        StpUtil.login(userId);
        return StpUtil.getTokenValue();
    }

    /**
     * 登录并携带扩展Session信息
     * @param userId 用户ID
     * @param extra 扩展参数
     * @return token
     */
    public static String createToken(Long userId, Map<String, Object> extra) {
        StpUtil.login(userId);
        if (extra != null) {
            extra.forEach((key, value) -> StpUtil.getSession().set(key, value));
        }
        return StpUtil.getTokenValue();
    }

    /**
     * 判断当前请求是否登录
     */
    public static boolean verifyToken() {
        return StpUtil.isLogin();
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId == null ? null : Long.valueOf(loginId.toString());
    }

    /**
     * 获取Session存储的扩展字段
     */
    public static Object getSessionValue(String key) {
        return StpUtil.getSession().get(key);
    }

    /**
     * 获取Token全局剩余过期秒数
     */
    public static long getTokenTimeout() {
        return StpUtil.getTokenTimeout();
    }

    /**
     * 刷新Token活跃时间，重置临时过期计时
     */
    public static void refreshToken() {
        StpUtil.updateLastActiveToNow();
    }

    /**
     * 当前用户登出
     */
    public static void logout() {
        StpUtil.logout();
    }

    /**
     * 根据指定token踢人下线
     */
    public static void logoutByToken(String tokenValue) {
        StpUtil.logoutByTokenValue(tokenValue);
    }

    /**
     * 获取当前请求token
     */
    public static String getTokenValue() {
        return StpUtil.getTokenValue();
    }

    /**
     * 生成业务临时凭证（验证码/邮件）
     */
    public static String createTempToken(Long userId, int expireSeconds) {
        String tempToken = IdUtil.fastSimpleUUID();
        // RedisUtil.set("temp:token:" + tempToken, userId, expireSeconds);
        return tempToken;
    }

    /**
     * 调试：获取当前登录token基础信息
     */
    public static Map<String, Object> getTokenInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("tokenValue", StpUtil.getTokenValue());
        info.put("loginId", StpUtil.getLoginIdDefaultNull());
        info.put("tokenTimeout", StpUtil.getTokenTimeout());
        info.put("sessionTimeout", StpUtil.getSessionTimeout());
        // 删掉不存在的 getTokenActivityTimeout()
        return info;
    }
}