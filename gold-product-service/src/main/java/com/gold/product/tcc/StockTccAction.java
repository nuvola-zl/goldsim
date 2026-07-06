package com.gold.product.tcc;

import com.gold.api.domain.dto.DeductStockRequest;
import com.gold.common.result.Result;
import com.gold.common.utils.RedisCacheUtil;
import com.gold.product.service.StockService;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@LocalTCC
@RequiredArgsConstructor
public class StockTccAction {

    private final StockService stockService;
    private final RedisCacheUtil redisCacheUtil;



    /**
     * Try：冻结库存
     * &#064;TwoPhaseBusinessAction  标记这是 TCC 的 Try 方法
     * name：全局唯一，Seata 用这个名字注册资源
     * commitMethod：全局事务提交后调用的方法名
     * rollbackMethod：全局事务回滚后调用的方法名
     * &#064;BusinessActionContextParameter：把参数透传给  Confirm 和 Cancel
     * 因为 Confirm/Cancel 是异步回调，需要这些参数知道要操作哪条库存
     */

    @TwoPhaseBusinessAction(
            name = "stockTccFreeze",
            commitMethod = "commit",
            rollbackMethod = "rollback"
    )
    public Result<Void> tryFreeze(
            BusinessActionContext context,
            @BusinessActionContextParameter(paramName = "specId") Long specId,
            @BusinessActionContextParameter(paramName = "quantity") Integer quantity,
            @BusinessActionContextParameter(paramName = "orderNo") String orderNo
    ) {
        // 【已修复】TCC Try 幂等
        // 原问题(todo):
        // Seata 超时重试会重复调用 freezeStock
        // 修复(2024-07-10): Redis 原子标记，同一 xid 只处理一次

        String idempKey = "tcc:try:" + context.getXid();
        boolean isNew = redisCacheUtil.trySet(idempKey, "1", 5, TimeUnit.MINUTES);
        if (!isNew) {
            log.warn("TCC Try 重复调用，xid={}", context.getXid());
            return Result.success(); // 已处理过，直接返回
        }
        log.info("【TCC-Try】冻结库存，xid={}, specId={}, quantity={}, orderNo={}", 
                context.getXid(), specId, quantity, orderNo);
        
        DeductStockRequest request = new DeductStockRequest();
        request.setSpecId(specId);
        request.setQuantity(quantity);
        request.setOrderNo(orderNo);
        
        // 调用你现有的 freezeStock，逻辑不变
        stockService.freezeStock(request);
        return Result.success();
    }

    /**
     * Confirm：全局事务提交后执行
     * 
     * 什么时候触发？createOrder 方法正常返回，没有抛异常
     * 做什么？空操作。因为冻结的库存继续保留，等支付成功后走 MQ 扣减
     * 
     * 为什么空操作？
     * 如果这里做真实扣减，那下单成功库存就扣了，用户还没付钱，不合理
     */
    public Result<Void> commit(BusinessActionContext context) {
        log.info("【TCC-Confirm】下单成功，冻结库存保持有效，xid={}", context.getXid());
        // 空操作：冻结继续保留，支付成功后走你现有的 MQ/Feign 真实扣减
        return Result.success();
    }

    /**
     * Rollback：全局事务回滚时执行
     * 
     * 什么时候触发？createOrder 抛异常，Spring 事务回滚
     * 做什么？立即解冻库存，解决库存泄漏问题
     * 
     * 注意：Seata 可能重复调用 Rollback，但 unFreezeStock SQL 有幂等性
     * （frozen_stock >= num 才扣减，重复执行第二次返回 0 行，不会负数）
     */
    public Result<Void> rollback(BusinessActionContext context) {
        String xid = context.getXid();

        // 安全获取参数
        Object specIdObj = context.getActionContext("specId");
        Object quantityObj = context.getActionContext("quantity");

        if (specIdObj == null || quantityObj == null) {
            log.error("【TCC-Rollback】参数为空，xid={}，可能Try阶段就失败了", xid);
            return Result.success();
        }

        Long specId = Long.valueOf(specIdObj.toString());
        Integer quantity = Integer.valueOf(quantityObj.toString());

        log.info("【TCC-Rollback】下单失败，立即解冻库存，xid={}, specId={}, quantity={}",
                xid, specId, quantity);

        // 【已修复】补全 orderNo
        // 原问题(todo):
        // rollback 阶段未设置 orderNo
        // 修复(2024-07-10): 从 context 获取 orderNo，补全请求
        Object orderNoObj = context.getActionContext("orderNo");
        String orderNo = orderNoObj != null ? orderNoObj.toString() : null;

        DeductStockRequest request = new DeductStockRequest();
        request.setSpecId(specId);
        request.setQuantity(quantity);
        request.setOrderNo(orderNo); // 【新增】
        try {
            stockService.unFreezeStock(request);
            log.info("【TCC-Rollback】解冻成功，xid={}", xid);
        } catch (Exception e) {
            log.error("【TCC-Rollback】解冻失败，xid={}，可能需要人工介入", xid, e);
        }

        return Result.success();
    }
}