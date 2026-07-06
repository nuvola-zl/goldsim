package com.gold.product.job;

import com.gold.api.domain.dto.DeductStockMqMessage;
import com.gold.common.mq.RabbitMqConstants;
import com.gold.product.entity.StockDeductFailRecord;
import com.gold.product.mapper.StockDeductFailRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存扣减死信记录定时补偿重试
 *
 * 每5分钟扫描一次 stock_deduct_fail_record 表，
 * 将待处理且重试次数 < 3 的记录重新发送到正常队列。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockDeductRetryJob {

    private final StockDeductFailRecordMapper failRecordMapper;
    private final RabbitTemplate rabbitTemplate;

    // 最大重试次数，超过则不再自动重试，需人工介入
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 每5小时执行一次
     */
    // 改成（1小时 = 60 * 60 * 1000 = 3600000ms）：
    @Scheduled(fixedRate = 3600000)
    public void retryFailRecords() {
        log.info("开始扫描库存扣减死信记录，准备补偿重试...");

        // 查询待处理且重试次数未达上限的记录
        List<StockDeductFailRecord> list = failRecordMapper.selectByStatusAndRetryCount(0, MAX_RETRY_COUNT);

        if (list.isEmpty()) {
            log.info("本次无待补偿的死信记录");
            return;
        }

        log.info("扫描到 {} 条待补偿记录", list.size());

        for (StockDeductFailRecord record : list) {
            try {
                // 重新组装消息
                DeductStockMqMessage message = new DeductStockMqMessage();
                message.setOrderNo(record.getOrderNo());
                message.setSpecId(record.getSpecId());
                message.setQuantity(record.getQuantity());

                // 发送到正常队列重新消费
                rabbitTemplate.convertAndSend(
                        RabbitMqConstants.STOCK_EXCHANGE,
                        RabbitMqConstants.ROUTE_KEY_DEDUCT_STOCK,
                        message
                );

                // 更新状态为"已补偿"
                record.setStatus(1);
                record.setRetryCount(record.getRetryCount() + 1);
                record.setUpdateTime(LocalDateTime.now());
                failRecordMapper.updateById(record);

                log.info("死信记录补偿重试成功，orderNo={}", record.getOrderNo());

            } catch (Exception e) {
                // 补偿发送失败，增加重试次数，下次继续
                record.setRetryCount(record.getRetryCount() + 1);
                record.setUpdateTime(LocalDateTime.now());
                failRecordMapper.updateById(record);

                log.error("死信记录补偿重试失败，orderNo={}，当前重试次数={}",
                        record.getOrderNo(), record.getRetryCount(), e);
            }
        }

        log.info("死信记录补偿重试任务完成");
    }
}