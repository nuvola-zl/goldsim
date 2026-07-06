package com.gold.common.mq;

public final class RabbitMqConstants {

    private RabbitMqConstants() {}

    // 交换机
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_DLX_EXCHANGE = "order.dlx.exchange";

    // 路由键
    public static final String ROUTE_KEY_ACTIVITY_CREATE = "order.activity.create";
    public static final String ROUTE_KEY_ACTIVITY_CREATE_DLX = "order.activity.create.dlx";
    public static final String ROUTE_KEY_DELAY_CLOSE = "order.delay.close";
    public static final String ROUTE_KEY_DELAY_CANCEL = "order.delay.cancel";

    // ========== 库存扣减消息 ==========
    /** 库存扣减交换机 */
    public static final String STOCK_EXCHANGE = "stock.exchange";
    /** 库存扣减路由键 */
    public static final String ROUTE_KEY_DEDUCT_STOCK = "stock.deduct";
    /** 库存扣减队列 */
    public static final String QUEUE_DEDUCT_STOCK = "stock.deduct.queue";
    /** 库存扣减死信队列 */
    public static final String QUEUE_DEDUCT_STOCK_DLX = "stock.deduct.queue.dlx";

    // ========== 排行榜更新消息 ==========
    /** 排行榜更新交换机 */
    public static final String LEADERBOARD_EXCHANGE = "leaderboard.exchange";
    /** 排行榜更新路由键 */
    public static final String ROUTE_KEY_LEADERBOARD_UPDATE = "leaderboard.update";
    /** 排行榜更新队列 */
    public static final String QUEUE_LEADERBOARD_UPDATE = "leaderboard.update.queue";
}