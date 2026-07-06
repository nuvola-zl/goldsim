package com.gold.order.job;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gold.order.domain.po.MqMessageLog;
import com.gold.order.mapper.MqMessageLogMapper;
import com.gold.order.service.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqMessageRetryJob {

    private final MqMessageLogMapper messageLogMapper;
    private final OrderServiceImpl orderService;

    /**
     * 每300秒扫描一次待发送消息(可以修改成小一点）
     */
    @Scheduled(fixedRate = 300000)
    public void retryPendingMessages() {
        // 1. 处理待发送消息
        List<MqMessageLog> pendingList = messageLogMapper.selectPendingMessages();

        if (!pendingList.isEmpty()) {
            log.info("扫描到{}条待发送消息", pendingList.size());

            for (MqMessageLog msg : pendingList) {
                // 增加重试次数
                messageLogMapper.increaseRetry(msg.getId());

                // 重试发送
                orderService.sendStockDeductMessage(msg.getBizKey());
            }
        }

        // 2. 【新增】处理死信消息（超过最大重试次数）
        List<MqMessageLog> deadList = messageLogMapper.selectDeadMessages();

        if (!deadList.isEmpty()) {
            log.error("【严重告警】扫描到{}条死信消息，需人工介入", deadList.size());

            for (MqMessageLog msg : deadList) {
                // 标记为失败终态
                messageLogMapper.markDead(msg.getId());

                log.error("死信消息详情：bizKey={}, bizType={}, retry_count={}, max_retry={}",
                        msg.getBizKey(), msg.getBizType(), msg.getRetryCount(), msg.getMaxRetry());

                // TODO: 接入钉钉/企微告警
                // TODO: 自动退款或人工介入补偿
            }
        }
    }
}