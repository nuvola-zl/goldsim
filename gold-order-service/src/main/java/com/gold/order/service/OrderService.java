package com.gold.order.service;

import com.gold.common.message.ActivityOrderMessage;
import com.gold.common.result.PageResult;
import com.gold.order.domain.dto.ActivityOrderCreateDTO;
import com.gold.order.domain.dto.OrderCreateDTO;
import com.gold.order.domain.vo.ActivityOrderResultVO;
import com.gold.order.domain.vo.OrderDetailVO;
import com.gold.order.domain.vo.OrderListVO;
import com.gold.order.domain.vo.OrderValidVO;

public interface OrderService {

    /**
     * 创建订单
     * @param createDTO 下单参数封装
     */
    String createOrder(Long userId,OrderCreateDTO createDTO);

    // 新增：根据订单号生成支付页面
    String generatePayHtml(String orderNo, Long loginUserId);

    // 新增：支付成功后更新订单、扣库存
    void handleOrderPaySuccess(String orderNo, String tradeNo);

    // 新增：订单详情（含门店信息）
    OrderDetailVO getOrderDetail(String orderNo);

    // 抢购下单
    String getActivityToken(Long userId, Long activityId, Long specId);
    String createActivityOrder(Long userId, ActivityOrderCreateDTO dto);
    ActivityOrderResultVO queryActivityOrderResult(Long userId, String token);
    void asyncCreateActivityOrder(ActivityOrderMessage message);

    // 补偿方法（供死信队列调用）
    void rollbackActivityStock(Long activityId, Long specId, Long userId, Integer buyNum);
    void cacheActivityResult(String token, int status, String orderNo, String failReason);

    /**
     * 我的订单列表
     */
    PageResult<OrderListVO> listOrders(Long userId, Integer status, Integer page, Integer size);

    /**
     * 手动取消订单
     */
    void cancelOrder(String orderNo, Long userId);

    /**
     * 支付前校验
     */
    OrderValidVO validateOrder(String orderNo, Long userId);
}