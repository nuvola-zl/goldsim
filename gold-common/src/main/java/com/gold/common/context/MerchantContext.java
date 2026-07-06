package com.gold.common.context;

public class MerchantContext {
    private static final ThreadLocal<Long> MERCHANT_ID = new ThreadLocal<>();

    public static void setMerchantId(Long merchantId) {
        MERCHANT_ID.set(merchantId);
    }

    public static Long getMerchantId() {
        return MERCHANT_ID.get();
    }

    public static void clear() {
        MERCHANT_ID.remove();
    }
}