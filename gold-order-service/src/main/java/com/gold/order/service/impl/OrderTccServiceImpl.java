package com.gold.order.service.impl;

import com.alibaba.fastjson2.JSON;
import com.gold.api.domain.dto.DeductStockRequest;
import com.gold.api.domain.dto.PayNoticeMessage;
import com.gold.api.domain.dto.SpecInfoDTO;
import com.gold.api.domain.dto.UserInfoDTO;
import com.gold.api.feign.ProductFeignApi;
import com.gold.api.feign.StoreFeignApi;
import com.gold.api.feign.UserFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.exception.BusinessException;
import com.gold.common.result.Result;
import com.gold.common.utils.SnowflakeIdGenerator;
import com.gold.order.domain.po.MallOrder;
import com.gold.order.domain.po.MallOrderItem;
import com.gold.order.mapper.MallOrderItemMapper;
import com.gold.order.mapper.MallOrderMapper;
import com.gold.order.producer.OrderMqProducer;
import com.gold.order.service.OrderTccService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTccServiceImpl implements OrderTccService {

    private final ProductFeignApi productFeignApi;
    private final UserFeignApi userFeignApi;
    private final StoreFeignApi storeFeignApi;
    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final OrderMqProducer orderMqProducer;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    @GlobalTransactional(rollbackFor = Exception.class, name = "create-order-tcc")
    @Transactional(rollbackFor = Exception.class)
    public String createOrderWithTcc(Long userId, Long specId, Integer buyNum,
                                      Long storeId, String remark, SpecInfoDTO spec, boolean isActivity) {
        /*
         * TODO 【分布式事务补偿 - TCC/Saga 优化】
         * 当前问题：freezeStock() 是跨服务 Feign 调用，不在 @Transactional 事务内。
         * 如果后续 orderMapper.insert() 或 orderItemMapper.insert() 失败，Spring 事务回滚，
         * 但 mall-service 侧的库存冻结不会自动回滚，造成"库存泄漏"。
         *
         * 现有兜底：15 分钟延迟 MQ（sendDelayCloseOrder）会自动关单并解冻库存。
         * 但如果 MQ 消息发送也失败，冻结的库存无法自动释放。
         *
         * 优化方向：
         *   方案 A（推荐）：引入 Seata TCC 模式
         *     - Try：freezeStock() 冻结库存
         *     - Confirm：支付成功后 deductStock() 确认扣减
         *     - Cancel：订单创建失败/超时取消时 unFreezeStock() 释放冻结
         *
         *   方案 B：本地消息表（最终一致性）
         *     - 事务内写入"库存解冻消息表"，定时任务扫表异步调用 unFreezeStock()
         *     - 不依赖 Seata，侵入小，适合毕业设计后迭代
         *
         *   方案 C：定时任务兜底
         *     - XXL-Job 每 5 分钟扫描 frozen_stock > 0 且无进行中订单的规格，自动解冻
         *     - 实现简单，作为现有 MQ 兜底的补充防线
         *
         * 毕业设计阶段：当前 15 分钟 MQ 超时取消已够用，答辩时提及上述优化方向即可。
         */
        UserInfoDTO user = getUserInfo(userId);


        Result<Boolean> validResult = storeFeignApi.isStoreValid(storeId);
        if (validResult == null || !validResult.getData()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "门店不存在或已停业");
        }

        Long activityId = null;
        BigDecimal unitPrice;
        BigDecimal originalPrice;

        if (isActivity) {
            if (spec.getActivityId() == null) {
                throw new RuntimeException("该商品暂无活动");
            }
            activityId = spec.getActivityId();
            unitPrice = spec.getActivityPrice();
            originalPrice = spec.getOriginalPrice();
        } else {
            unitPrice = spec.getRealTimeTotalPrice();
            originalPrice = unitPrice;
        }

        if (buyNum > spec.getAvailableStock()) {
            throw new RuntimeException("商品库存不足，剩余可售：" + spec.getAvailableStock());
        }

        BigDecimal payAmount = unitPrice.multiply(BigDecimal.valueOf(buyNum)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = originalPrice.multiply(BigDecimal.valueOf(buyNum)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = totalAmount.subtract(payAmount);
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }

        BigDecimal weight = spec.getWeight();
        BigDecimal craftFeePerGram = spec.getCraftFee();
        BigDecimal realTimePricePerGram = spec.getRealTimePricePerGram();

        BigDecimal goldValuePerUnit = weight.multiply(realTimePricePerGram).setScale(2, RoundingMode.HALF_UP);
        BigDecimal craftFeePerUnit = weight.multiply(craftFeePerGram).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalGoldValue = goldValuePerUnit.multiply(BigDecimal.valueOf(buyNum));
        BigDecimal totalCraftFee = craftFeePerUnit.multiply(BigDecimal.valueOf(buyNum));

        // 【关键改动1】先生成订单号，TCC Try 需要 orderNo 做幂等和上下文传递
        LocalDateTime now = LocalDateTime.now();
        String orderNo = snowflakeIdGenerator.nextOrderNo();

        // 【关键改动2】TCC Try 冻结库存（替代原来的普通 freezeStock）
        // 如果这里失败，直接抛异常，@GlobalTransactional 不会开启全局事务
        // 如果下面 insert 失败抛异常，Seata 自动调 TCC Cancel（unFreezeStock）
        DeductStockRequest freezeReq = new DeductStockRequest();
        freezeReq.setSpecId(specId);
        freezeReq.setQuantity(buyNum);
        freezeReq.setOrderNo(orderNo);  // 必须传，TCC 上下文需要

        Result<Void> freezeResult = productFeignApi.tccFreezeStock(freezeReq);
        if (freezeResult == null || freezeResult.isError()) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存不足");
        }

        Map<String, Object> snapshotMap = new HashMap<>();
        snapshotMap.put("priceType", isActivity ? "activity" : "realtime");
        snapshotMap.put("unitPrice", unitPrice);
        snapshotMap.put("originalPrice", originalPrice);
        snapshotMap.put("realTimePricePerGram", realTimePricePerGram);
        snapshotMap.put("referencePricePerGram", spec.getReferencePricePerGram());
        snapshotMap.put("weight", weight);
        snapshotMap.put("craftFeePerGram", craftFeePerGram);
        snapshotMap.put("buyNum", buyNum);
        snapshotMap.put("metalType", spec.getMetalType());
        if (isActivity) {
            snapshotMap.put("activityId", spec.getActivityId());
            snapshotMap.put("activityName", spec.getActivityName());
            snapshotMap.put("activityPrice", spec.getActivityPrice());
            snapshotMap.put("limitPerUser", spec.getLimitPerUser());
        }
        String snapshotJson = JSON.toJSONString(snapshotMap);

        MallOrder order = MallOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .orderType(isActivity ? 2 : 1)
                .activityId(activityId)
                .totalAmount(totalAmount)
                .goldAmount(totalGoldValue)
                .craftFeeAmount(totalCraftFee)
                .discountAmount(discountAmount)
                .payAmount(payAmount)
                .status(0)
                .receiverName(user.getRealName())
                .receiverPhone(user.getPhone())
                .storeId(storeId)
                .remark(remark)
                .createTime(now)
                .updateTime(now)
                .build();
        orderMapper.insert(order);

        MallOrderItem item = MallOrderItem.builder()
                .orderId(order.getId())
                .goodsId(spec.getGoodsId())
                .specId(specId)
                .goodsName(spec.getGoodsName())
                .goodsImage(spec.getCoverImage())
                .weight(weight)
                .quantity(buyNum)
                .goldPrice(realTimePricePerGram)
                .craftFee(craftFeePerGram)
                .unitPrice(unitPrice)
                .totalPrice(payAmount)
                .snapshotJson(snapshotJson)
                .createTime(now)
                .build();
        orderItemMapper.insert(item);

        PayNoticeMessage payMsg = new PayNoticeMessage();
        payMsg.setUserId(userId);
        payMsg.setOrderNo(orderNo);
        payMsg.setPayAmount(payAmount);
        payMsg.setGoodsName(spec.getGoodsName());
        payMsg.setExpireMinute(15);
        orderMqProducer.sendPayNotice(payMsg);
        orderMqProducer.sendDelayCloseOrder(orderNo, 15, TimeUnit.MINUTES);

        return orderNo;
    }

    private UserInfoDTO getUserInfo(Long userId) {
        Result<UserInfoDTO> result = userFeignApi.getUserInfo(userId);
        if (result == null || result.isError() || result.getData() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        return result.getData();
    }
}