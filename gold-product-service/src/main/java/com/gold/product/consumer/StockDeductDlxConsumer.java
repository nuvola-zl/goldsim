package com.gold.product.consumer;

import com.alibaba.fastjson2.JSON;
import com.gold.api.domain.dto.DeductStockMqMessage;
import com.gold.common.mq.RabbitMqConstants;
import com.gold.product.entity.StockDeductFailRecord;
import com.gold.product.mapper.StockDeductFailRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockDeductDlxConsumer {

    private final StockDeductFailRecordMapper failRecordMapper;

    /**
     * 死信队列：消费失败3次后的兜底
     *
     * 什么情况下会进死信？
     * 1. 消费者抛异常，basicNack且requeue=false
     * 2. 消息过期（TTL）
     * 3. 队列满了
     *
     * 这里主要是第1种：业务异常导致消费失败
     *
     * 进了死信怎么办？
     * - 记录告警日志
     * - 后续可接入钉钉/企业微信告警
     * - 管理员人工介入：手动补扣库存或退款
     *
     * 【已修复】数据一致性补偿
     *
     * 原问题(todo):
     * - 死信队列消费者仅记录日志，不做任何补偿操作
     * - 此时订单已支付（status=1）但库存永远不被扣减
     * - 无告警通知，管理员无法及时感知
     *
     * 修复内容(2024-07-10):
     * - 将死信消息持久化到 stock_deduct_fail_record 表
     * - 记录订单号、规格ID、数量、失败原因、状态
     * - 支持定时任务扫描并自动补偿重试
     * - 数据库兜底，避免消息丢失导致的数据不一致
     *
     * 毕业设计阶段：持久化到数据库 + 定时重试
     * 生产环境：接入钉钉/企微告警 + 自动退款 + 管理后台人工处理
     */
    @RabbitListener(queues = RabbitMqConstants.QUEUE_DEDUCT_STOCK_DLX)
    public void handleDlxMessage(String message) {
        DeductStockMqMessage mqMessage = JSON.parseObject(message, DeductStockMqMessage.class);

        log.error("【死信告警】库存扣减消息进入死信队列，orderNo={}, specId={}, quantity={}",
                mqMessage.getOrderNo(), mqMessage.getSpecId(), mqMessage.getQuantity());

        // 【新增】保存死信记录到数据库，便于后续补偿重试
        StockDeductFailRecord record = new StockDeductFailRecord();
        record.setOrderNo(mqMessage.getOrderNo());
        record.setSpecId(mqMessage.getSpecId());
        record.setQuantity(mqMessage.getQuantity());
        record.setStatus(0); // 0=待处理
        record.setFailReason("消费异常进入死信队列");
        record.setRetryCount(0);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        failRecordMapper.insert(record);
        log.info("死信记录已保存到数据库，orderNo={}，等待定时任务补偿重试", mqMessage.getOrderNo());
    }
}