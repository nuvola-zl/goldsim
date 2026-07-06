package com.gold.common.context;

public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    // 设置当前线程用户ID
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    // 获取当前线程用户ID
    public static Long getUserId() {
        return USER_ID.get();
    }

    // 清空，防止线程池内存泄漏
    public static void clear() {
        USER_ID.remove();
    }
}