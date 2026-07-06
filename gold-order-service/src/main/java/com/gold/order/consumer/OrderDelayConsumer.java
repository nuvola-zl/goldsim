package com.gold.order.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gold.api.domain.dto.DeductStockRequest;
import com.gold.api.domain.dto.OrderDelayMessage;
import com.gold.api.feign.ProductFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.exception.BusinessException;
import com.gold.common.result.Result;
import com.gold.order.domain.po.MallOrder;
import com.gold.order.domain.po.MallOrderItem;
import com.gold.order.mapper.MallOrderItemMapper;
import com.gold.order.mapper.MallOrderMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j // 新增日志
@Component
@RequiredArgsConstructor
public class OrderDelayConsumer {

    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final ProductFeignApi productFeignApi;

    /**
     * 延迟关单消费者
     * ackMode = MANUAL：手动确认，业务成功才 ack
     * &#064;Transactional：本地数据库操作（改订单状态）在事务内
     * 注意：Feign 调用（unFreezeStock）不在事务内，如果失败抛异常
     *       事务回滚（订单状态不改），MQ 消息进死信队列，由死信消费者兜底
     */

    // 关键1：开启手动ACK ackMode = "MANUAL"
    @RabbitListener(queues = "order.delay.queue", ackMode = "MANUAL")
    @Transactional(rollbackFor = Exception.class)
    // 关键2：新增 Channel channel, Message mqMsg 两个入参
    public void listenDelayClose(OrderDelayMessage msg, Channel channel, Message mqMsg) {
        long deliveryTag = mqMsg.getMessageProperties().getDeliveryTag();
        String orderNo = msg.getOrderNo();
        try {
            LambdaQueryWrapper<MallOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(MallOrder::getOrderNo, orderNo);
            MallOrder order = orderMapper.selectOne(orderWrapper);
            if (order == null) {
                // 订单不存在，直接确认丢弃消息
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 已支付直接跳过关单
            if (order.getStatus() == 1) {
                log.info("订单{}已支付，跳过关单", orderNo);
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 仅待支付订单执行取消逻辑
            if (order.getStatus() == 0) {
                LambdaQueryWrapper<MallOrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(MallOrderItem::getOrderId, order.getId());
                List<MallOrderItem> itemList = orderItemMapper.selectList(itemWrapper);
                // 循环解冻所有商品库存
                for (MallOrderItem item : itemList) {
                    DeductStockRequest unFreezeReq = new DeductStockRequest();
                    unFreezeReq.setSpecId(item.getSpecId());
                    unFreezeReq.setQuantity(item.getQuantity());
                    Result<Void> unFreezeResult = productFeignApi.unFreezeStock(unFreezeReq);
                    if (unFreezeResult.isError()) {
                        throw new BusinessException(ErrorCode.REMOTE_CALL_ERROR,"解冻库存失败，远程调用异常");
                    }
                }
                // 更新订单状态为已取消
                order.setStatus(5);
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
                log.info("超时未支付，关闭订单{}成功", orderNo);
            }
            // 全部业务正常，确认消息，彻底移除队列
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("延迟关单消息处理失败，orderNo={}", orderNo, e);
            try {
                // 关键3：第三个参数false，不重回原队列，自动进入死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("消息NACK拒绝IO异常", ioException);
            }
        }
    }
}