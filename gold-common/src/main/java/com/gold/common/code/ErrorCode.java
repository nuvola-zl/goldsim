package com.gold.common.code;

/**
 * 错误码常量 - A-BB-CCC 模型
 */
public final class ErrorCode {



    private ErrorCode() {}

    // ========== 成功 ==========
    public static final String SUCCESS = "A00000";

    // ========== 通用域 (00) ==========
    /** 参数校验失败 */
    public static final String PARAM_ERROR = "A00001";
    /** 资源不存在 */
    public static final String RESOURCE_NOT_FOUND = "A00002";
    /** 资源已存在/重复 */
    public static final String RESOURCE_DUPLICATE = "A00003";
    /** 资源状态不允许操作 */
    public static final String RESOURCE_STATUS_INVALID = "A00004";
    /** 并发冲突/系统繁忙 */
    public static final String SYSTEM_BUSY = "A00005";
    /**线程中断**/
    public static final String THREAD_INTERRUPTION="A00006";
    /** 请求已处理 */
    public static final String REQUEST_PROCESSED = "A00007";
    /**远程调用异常*/
    public static final String REMOTE_CALL_ERROR = "A00008";
    /**创建异常**/
    public static final String CREATE_MESSAGE_FAILED = "A00009";

    // ========== 用户中心 (01) ==========
    /** 用户名已存在 */
    public static final String USERNAME_EXISTS = "A01001";
    /** 密码错误 */
    public static final String PASSWORD_ERROR = "A01002";
    /** 手机号错误 */
    public static final String PHONE_ERROR = "A01003";
    /** 账户不存在 */
    public static final String ACCOUNT_NOT_FOUND = "A01004";

    // ========== 商品中心 (02) ==========
    /** 商品/规格不存在 */
    public static final String GOODS_NOT_FOUND = "A02001";
    /** 规格已停售 */
    public static final String GOODS_OFF_SALE = "A02002";
    /** 库存不足 */
    public static final String STOCK_INSUFFICIENT = "A02003";
    /** 规格已关联活动 */
    public static final String GOODS_BOUND_ACTIVITY = "A02004";
    /** 库存不能为负数 */
    public static final String STOCK_NEGATIVE = "A02005";

    // ========== 活动中心 (03) ==========
    /** 活动不存在 */
    public static final String ACTIVITY_NOT_FOUND = "A03001";
    /** 活动状态不允许操作 */
    public static final String ACTIVITY_STATUS_INVALID = "A03002";
    /** 活动商品已存在 */
    public static final String ACTIVITY_GOODS_EXISTS = "A03003";
    /** 活动商品已关联其他活动 */
    public static final String ACTIVITY_GOODS_BOUND_OTHER = "A03004";
    /** 活动库存不能低于已售出 */
    public static final String ACTIVITY_STOCK_BELOW_SOLD = "A03005";
    /** 活动库存已售罄 */
    public static final String ACTIVITY_STOCK_SOLD_OUT = "A03006";
    /** 活动未开始或已结束 */
    public static final String ACTIVITY_NOT_AVAILABLE = "A03007";

    /** 活动正在进行 */
    public static final String ACTIVITY_IS_RUNNING = "A03008";
    // ========== 交易中心 (04) ==========
    /** 余额不足 */
    public static final String BALANCE_INSUFFICIENT = "A04001";
    /** 持仓不足 */
    public static final String POSITION_INSUFFICIENT = "A04002";
    /** 行情获取失败 */
    public static final String MARKET_PRICE_FETCH_FAILED = "A04003";
    /** 行情价格异常 */
    public static final String MARKET_PRICE_INVALID = "A04004";
    /** 重量/金价/工费不能为空 */
    public static final String PRICE_PARAM_EMPTY = "A04005";
    /** Token过期 */
    public static final String TOKEN_EXPIRED = "A04006";
    /** 支付创建失败 */
    public static final String PAYMENT_CREATE_FAILED = "A04007";

    // ========== 门店/地址 (05) ==========
    /** 门店不存在 */
    public static final String STORE_NOT_FOUND = "A05001";
    /** 经纬度不能为空 */
    public static final String LOCATION_EMPTY = "A05002";

    // ========== 系统错误 (B) ==========
    /** 系统内部错误 */
    public static final String SYSTEM_ERROR = "B00001";
    /** 数据库操作失败 */
    public static final String DB_ERROR = "B00002";
    /** 分布式锁异常 */
    public static final String LOCK_ERROR = "B00003";

    // ========== 第三方错误 (C) ==========
    /** OSS上传失败 */
    public static final String OSS_UPLOAD_FAILED = "C06001";
    /** 支付宝调用失败 */
    public static final String ALIPAY_ERROR = "C06002";
    /** 支付宝客户端异常 */
    public static final String ALIPAY_CLIENT_ERROR = "C06003";
}