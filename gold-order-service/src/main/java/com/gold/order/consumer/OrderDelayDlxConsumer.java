package com.gold.order.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gold.api.domain.dto.DeductStockRequest;
import com.gold.api.domain.dto.OrderDelayMessage;
import com.gold.api.feign.ProductFeignApi;
import com.gold.order.domain.po.MallOrder;
import com.gold.order.domain.po.MallOrderItem;
import com.gold.order.mapper.MallOrderItemMapper;
import com.gold.order.mapper.MallOrderMapper;
import feign.Feign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayDlxConsumer {
    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final ProductFeignApi productFeignApi;

    @RabbitListener(queues = "order.delay.dlx.queue")
    /**
     * 【已修复】事务缺失问题
     *
     * 原问题(todo):
     * - 无 @Transactional，Feign 调用成功后若数据库更新失败，库存已解冻但订单状态未更新
     * - 消息已被消费无法重试，订单永远卡在待支付状态
     * TODO [事务缺失] 此方法无 @Transactional。
     * Feign 调用 unFreezeStock 成功后，若 orderMapper.updateById 失败，库存已解冻但订单状态未更新（仍是 pending），且消息已被消费无法重试，订单永远卡在待支付状态
     * 修复内容(2024-07-10):
     * - 添加 @Transactional(rollbackFor = Exception.class)
     * - 异常时事务回滚，数据库状态保持一致
     * - 注意：Feign 调用不在本地事务内，极端情况下仍需人工兜底
     */
    @Transactional(rollbackFor = Exception.class)  // 【新增】
    public void handleDelayDlx(OrderDelayMessage msg) {
        String orderNo = msg.getOrderNo();
        log.error("【延迟关单死信告警】订单{}自动关单失败，进入死信队列", orderNo);
        try {
            LambdaQueryWrapper<MallOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(MallOrder::getOrderNo, orderNo);
            MallOrder order = orderMapper.selectOne(orderWrapper);
            // 已支付/已关闭直接跳过
            if (order == null || order.getStatus() == 1 || order.getStatus() == 5) {
                log.info("死信订单{}无需处理", orderNo);
                return;
            }
            // 兜底再次执行关单、解冻库存
            LambdaQueryWrapper<MallOrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(MallOrderItem::getOrderId, order.getId());
            List<MallOrderItem> itemList = orderItemMapper.selectList(itemWrapper);
            for (MallOrderItem item : itemList) {
                DeductStockRequest req = new DeductStockRequest();
                req.setSpecId(item.getSpecId());
                req.setQuantity(item.getQuantity());
                productFeignApi.unFreezeStock(req);
            }
            order.setStatus(5);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            log.info("死信兜底处理成功，订单{}已关闭", orderNo);
        } catch (Exception e) {
            log.error("死信队列兜底处理失败，订单{}需人工核查库存", orderNo, e);
            // todo生产环境此处可对接钉钉/短信告警
        }
    }
}