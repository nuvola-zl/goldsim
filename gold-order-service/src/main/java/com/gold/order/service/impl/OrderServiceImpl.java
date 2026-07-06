package com.gold.order.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.api.domain.dto.*;
import com.gold.api.feign.ProductFeignApi;
import com.gold.api.feign.StoreFeignApi;
import com.gold.api.feign.UserFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.context.UserContext;
import com.gold.common.exception.BusinessException;
import com.gold.common.message.ActivityOrderMessage;
import com.gold.common.mq.RabbitMqConstants;
import com.gold.common.result.PageResult;
import com.gold.common.result.Result;
import com.gold.common.utils.RedisCacheUtil;
import com.gold.common.utils.SnowflakeIdGenerator;
import com.gold.order.domain.dto.ActivityOrderCreateDTO;
import com.gold.order.domain.dto.OrderCreateDTO;
import com.gold.order.domain.po.MallOrder;
import com.gold.order.domain.po.MallOrderItem;
import com.gold.order.domain.po.MqMessageLog;
import com.gold.order.domain.vo.ActivityOrderResultVO;
import com.gold.order.domain.vo.OrderDetailVO;
import com.gold.order.domain.vo.OrderListVO;
import com.gold.order.domain.vo.OrderValidVO;
import com.gold.order.mapper.MallOrderItemMapper;
import com.gold.order.mapper.MallOrderMapper;
import com.gold.order.mapper.MqMessageLogMapper;
import com.gold.order.producer.OrderMqProducer;
import com.gold.order.service.OrderService;
import com.gold.order.service.OrderTccService;
import feign.FeignException;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // ==================== 依赖注入 ====================

    private final ProductFeignApi productFeignApi;
    private final UserFeignApi userFeignApi;
    private final StoreFeignApi storeFeignApi;
    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final OrderMqProducer orderMqProducer;
    private final AlipayService alipayService;
    private final RedisCacheUtil redisCacheUtil;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final MqMessageLogMapper messageLogMapper;
    private final RabbitTemplate rabbitTemplate;
    private final OrderTccService orderTccService;


    // ═══════════════════════════════════════════════════════
    //  一、普通下单（同步）
    // ═══════════════════════════════════════════════════════

    @Override
    public String createOrder(Long userId, OrderCreateDTO dto) {
        // 【新增】requestId 幂等校验
        String requestId = dto.getRequestId();
        String idempKey = "order:create:" + requestId;

        boolean isNew = redisCacheUtil.trySet(idempKey, "1", 15, TimeUnit.MINUTES);
        if (!isNew) {
            log.warn("重复下单请求，requestId={}", requestId);
            throw new BusinessException(ErrorCode.REQUEST_PROCESSED, "订单已创建，请勿重复提交");
        }

        // 原来的逻辑不变
        SpecInfoDTO spec = getGoodsSpecInfo(dto.getSpecId());
        if (spec.getActivityId() != null) {
            throw new BusinessException(ErrorCode.ACTIVITY_IS_RUNNING, "该商品正在参加活动，请从活动入口购买");
        }

        return orderTccService.createOrderWithTcc(
                userId,
                dto.getSpecId(),
                dto.getBuyNum(),
                dto.getStoreId(),
                dto.getRemark(),
                spec,
                false
        );
    }


    // ═══════════════════════════════════════════════════════
    //  二、抢购 Token（防刷 + 库存预热）
    // ═══════════════════════════════════════════════════════

    @Override
    public String getActivityToken(Long userId, Long activityId, Long specId) {
        // ── 1. 校验库存（Redis兜底，管理端应提前预热） ──
        String stockKey = "activity:stock:" + activityId + ":" + specId;
        String stockStr = redisCacheUtil.get(stockKey);

        if (stockStr == null) {
            String lockKey = "activity:init-lock:" + activityId + ":" + specId;
            RLock lock = redisCacheUtil.getLock(lockKey);
            try {
                if (lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                    try {
                        stockStr = redisCacheUtil.get(stockKey);
                        if (stockStr == null) {
                            SpecInfoDTO spec = getGoodsSpecInfo(specId);
                            if (spec.getActivityId() == null || !spec.getActivityId().equals(activityId)) {
                                throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND,"活动还未开始或者已结束");
                            }
                            int stock = spec.getActivityStock();
                            if (stock <= 0) {
                                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT,"活动库存已售罄");
                            }
                            redisCacheUtil.set(stockKey, String.valueOf(stock), 1, TimeUnit.DAYS);
                        }
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_BUSY, "系统繁忙，请稍后再试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_BUSY, "系统繁忙");
            }
        }

        stockStr = redisCacheUtil.get(stockKey);
        if (stockStr == null || Integer.parseInt(stockStr) <= 0) {
            throw new RuntimeException("活动库存已售罄");
        }

        // ── 2. 生成Token ──
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("userId", userId);
        tokenMap.put("activityId", activityId);
        tokenMap.put("specId", specId);
        tokenMap.put("timestamp", System.currentTimeMillis());
        redisCacheUtil.set("activity:token:" + token, JSON.toJSONString(tokenMap), 5, TimeUnit.MINUTES);

        // ── 3. 设置防刷（确保前面都成功了） ──
        String limitKey = "activity:limit:" + activityId + ":" + userId;
        redisCacheUtil.set(limitKey, "1", 1, TimeUnit.SECONDS);

        return token;
    }


    // ═══════════════════════════════════════════════════════
    //  三、创建抢购订单（同步入口，立即返回排队中）
    // ═══════════════════════════════════════════════════════

    @Override
    public String createActivityOrder(Long userId, ActivityOrderCreateDTO dto) {
        String token = dto.getToken();

        // ── 1. 校验Token ──
        String tokenJson = redisCacheUtil.get("activity:token:" + token);
        if (tokenJson == null) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "抢购Token已过期，请重新获取");
        }

        // 【已修复】Token 防重 - 使用 trySet 原子判断
        // 原问题(todo):
        // Token 校验后未删除，同一 Token 可被重复使用
        // 修复(2024-07-10): 用原子操作标记 Token 已使用，而不是删除
        String usedTokenKey = "activity:token:used:" + token;
        boolean isFirstUse = redisCacheUtil.trySet(usedTokenKey, "1", 5, TimeUnit.MINUTES);
        if (!isFirstUse) {
            log.warn("Token 已被使用，重复提交，token={}", token);
            throw new BusinessException(ErrorCode.REQUEST_PROCESSED, "请求已处理，请勿重复提交");
        }

        Map<String, Object> tokenMap = JSON.parseObject(tokenJson, Map.class);
        Long tokenUserId = Long.valueOf(tokenMap.get("userId").toString());
        Long activityId = Long.valueOf(tokenMap.get("activityId").toString());
        Long tokenSpecId = Long.valueOf(tokenMap.get("specId").toString());

        if (!tokenUserId.equals(userId)) {
            throw new RuntimeException("Token与用户不匹配");
        }
        if (!tokenSpecId.equals(dto.getSpecId())) {
            throw new RuntimeException("Token与规格不匹配");
        }

        // ── 2. 查规格 ──
        SpecInfoDTO spec = getGoodsSpecInfo(dto.getSpecId());
        if (spec.getActivityId() == null || !spec.getActivityId().equals(activityId)) {
            throw new RuntimeException("活动信息已变更，请刷新页面");
        }

        // ── 3. Redisson分布式锁扣库存 + 发送MQ ──
        // 【已修复】MQ 发送竞态条件
        // 原问题(todo):
        // 锁在 MQ 发送前释放，发送失败后回滚需要重新获取锁
        // 间隙期间其他请求可能读取了"被扣减后的库存"，导致错误判断
        // 修复(2024-07-10): 把 MQ 发送放到锁内，失败时直接在锁内回滚
        String lockKey = "activity:lock:" + activityId + ":" + dto.getSpecId();
        RLock lock = redisCacheUtil.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.SYSTEM_BUSY, "系统繁忙，请稍后再试");
            }

            String stockKey = "activity:stock:" + activityId + ":" + dto.getSpecId();
            String userKey = "activity:user:" + activityId + ":" + userId;

            String stockStr = redisCacheUtil.get(stockKey);
            int stock = stockStr == null ? 0 : Integer.parseInt(stockStr);
            if (stock < dto.getBuyNum()) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存不足");
            }

            String userBoughtStr = redisCacheUtil.get(userKey);
            int alreadyBought = userBoughtStr == null ? 0 : Integer.parseInt(userBoughtStr);
            if (alreadyBought + dto.getBuyNum() > spec.getLimitPerUser()) {
                throw new RuntimeException("超出限购数量，本场活动限购" + spec.getLimitPerUser() + "件");
            }

            // 扣减缓存库存、用户已购数量
            redisCacheUtil.set(stockKey, String.valueOf(stock - dto.getBuyNum()), 1, TimeUnit.DAYS);
            redisCacheUtil.set(userKey, String.valueOf(alreadyBought + dto.getBuyNum()), 1, TimeUnit.DAYS);

            // 组装 MQ 消息
            ActivityOrderMessage message = new ActivityOrderMessage();
            message.setToken(token);
            message.setUserId(userId);
            message.setActivityId(activityId);
            message.setSpecId(dto.getSpecId());
            message.setBuyNum(dto.getBuyNum());
            message.setStoreId(dto.getStoreId());
            message.setRemark(dto.getRemark());

            // MQ 发送放到锁内
            try {
                orderMqProducer.sendActivityOrder(message);
            } catch (Exception e) {
                log.error("MQ发送失败，token={}", token, e);
                // 锁内直接回滚，无竞态条件
                redisCacheUtil.set(stockKey, String.valueOf(stock), 1, TimeUnit.DAYS);
                redisCacheUtil.set(userKey, String.valueOf(alreadyBought), 1, TimeUnit.DAYS);
                throw new BusinessException(ErrorCode.SYSTEM_BUSY, "系统繁忙，请稍后再试");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "系统异常，请稍后再试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        // ── 4. 缓存"排队中"（MQ 已成功，不需要回滚了）──
        cacheActivityResult(token, 0, null, null);

        return token;
    }


    // ═══════════════════════════════════════════════════════
    //  四、轮询查询结果（增加数据库兜底）
    // ═══════════════════════════════════════════════════════

    @Override
    public ActivityOrderResultVO queryActivityOrderResult(Long userId, String token) {
        String key = "activity:result:" + token;
        String json = redisCacheUtil.get(key);
        ActivityOrderResultVO vo = new ActivityOrderResultVO();

        if (json == null) {
            // 【修正4】Redis无缓存：查数据库兜底（防止Redis缓存丢失）
            String orderNo = queryOrderNoByToken(token);
            if (orderNo != null) {
                vo.setStatus(1);
                vo.setOrderNo(orderNo);
                // 异步补偿缓存
                cacheActivityResult(token, 1, orderNo, null);
                return vo;
            }
            vo.setStatus(0);
            return vo;
        }

        Map<String, Object> resultMap = JSON.parseObject(json, Map.class);
        int status = Integer.parseInt(resultMap.get("status").toString());
        vo.setStatus(status);

        if (status == 1) {
            vo.setOrderNo(resultMap.get("orderNo") != null ? resultMap.get("orderNo").toString() : null);
            if (resultMap.get("payAmount") != null) {
                vo.setPayAmount(new BigDecimal(resultMap.get("payAmount").toString()));
            }
        } else if (status == 2) {
            vo.setFailReason(resultMap.get("failReason") != null
                    ? resultMap.get("failReason").toString()
                    : "抢购失败");
        }

        return vo;
    }


    // ═══════════════════════════════════════════════════════
    //  五、MQ 消费者：异步创建订单
    // ═══════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void asyncCreateActivityOrder(ActivityOrderMessage message) {
        String token = message.getToken();

        // ── 幂等检查 ──
        String resultKey = "activity:result:" + token;
        String existResult = redisCacheUtil.get(resultKey);
        if (existResult != null) {
            Map<String, Object> map = JSON.parseObject(existResult, Map.class);
            int status = Integer.parseInt(map.get("status").toString());
            if (status == 1 || status == 2) {
                log.info("抢购订单已处理，跳过重复消费，token={}", token);
                return;
            }
        }

        SpecInfoDTO spec = getGoodsSpecInfo(message.getSpecId());

        // ── 先写数据库（事务内） ──
        String orderNo;
        try {
            orderNo = orderTccService.createOrderWithTcc(
                    message.getUserId(),
                    message.getSpecId(),
                    message.getBuyNum(),
                    message.getStoreId(),
                    message.getRemark(),
                    spec,
                    true
            );
        } catch (Exception e) {
            log.error("抢购订单数据库创建失败，token={}", token, e);
            // 数据库失败：回滚Redis库存 + 缓存失败结果
            rollbackActivityStock(message.getActivityId(), message.getSpecId(), message.getUserId(), message.getBuyNum());
            cacheActivityResult(token, 2, null, e.getMessage());
            return;
        }

        // 【修正3】数据库成功：事务提交后，再写Redis缓存
        // 如果Redis挂了，不影响已提交的订单，只是用户轮询时走数据库兜底
        final String finalOrderNo = orderNo;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    cacheActivityResult(token, 1, finalOrderNo, null);
                    log.info("抢购订单创建成功，token={}, orderNo={}", token, finalOrderNo);
                } catch (Exception e) {
                    log.error("抢购结果缓存失败，token={}，但不影响订单，用户轮询会走数据库兜底", token, e);
                }
            }
        });
    }


    // ═══════════════════════════════════════════════════════
    //  六、补偿方法（供死信队列调用）
    // ═══════════════════════════════════════════════════════

    @Override
    public void rollbackActivityStock(Long activityId, Long specId, Long userId, Integer buyNum) {
        String lockKey = "activity:lock:" + activityId + ":" + specId;
        RLock lock = redisCacheUtil.getLock(lockKey);
        try {
            if (lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                try {
                    String stockKey = "activity:stock:" + activityId + ":" + specId;
                    String userKey = "activity:user:" + activityId + ":" + userId;

                    String stockStr = redisCacheUtil.get(stockKey);
                    int stock = stockStr == null ? 0 : Integer.parseInt(stockStr);
                    redisCacheUtil.set(stockKey, String.valueOf(stock + buyNum), 1, TimeUnit.DAYS);

                    String userBoughtStr = redisCacheUtil.get(userKey);
                    int userBought = userBoughtStr == null ? 0 : Integer.parseInt(userBoughtStr);
                    if (userBought >= buyNum) {
                        redisCacheUtil.set(userKey, String.valueOf(userBought - buyNum), 1, TimeUnit.DAYS);
                    }

                    log.info("Redis抢购库存回滚成功，activityId={}, specId={}, userId={}, num={}",
                            activityId, specId, userId, buyNum);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                // 【新增】获取锁失败，回滚未执行
                throw new RuntimeException("获取分布式锁失败，库存回滚未执行");
            }
        } catch (Exception e) {
            log.error("Redis抢购库存回滚失败，activityId={}, specId={}, userId={}, num={}",
                    activityId, specId, userId, buyNum, e);
            // 【已修复】异常不再吞掉，抛出让调用方感知
            // 原问题(todo):
            //  异常被静默吞掉，调用方认为回滚成功，Redis 故障时库存永久泄漏
            // 修复(7-10): 抛异常让调用方知道回滚失败，触发告警和人工介入
            throw new RuntimeException("库存回滚失败，activityId=" + activityId + ", specId=" + specId, e);
        }
    }

    @Override
    public void cacheActivityResult(String token, int status, String orderNo, String failReason) {
        String key = "activity:result:" + token;
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("orderNo", orderNo);
        map.put("failReason", failReason);
        map.put("timestamp", System.currentTimeMillis());
        // todo 把三十分钟修改成两个小时
        redisCacheUtil.set(key, JSON.toJSONString(map), 2, TimeUnit.HOURS);
    }





    // ═══════════════════════════════════════════════════════
    //  八、轮询兜底：通过 snapshot_json 反查订单
    // ═══════════════════════════════════════════════════════

    private String queryOrderNoByToken(String token) {
        // todo
        // try {
        //     // 通过 snapshot_json 里的 token 反查订单
        //     // 生产环境建议：mall_order 表加 activity_token 字段 + 索引
        //     LambdaQueryWrapper<MallOrderItem> wrapper = new LambdaQueryWrapper<>();
        //     // 这里假设 snapshot_json 格式固定，实际应该用 JSON 查询或加字段
        //     // 简化：遍历最近10分钟的订单（实际生产请优化）
        //     wrapper.like(MallOrderItem::getSnapshotJson, "\"token\":\"" + token + "\"");
        //     List<MallOrderItem> items = orderItemMapper.selectList(wrapper);
        //     if (!items.isEmpty()) {
        //         MallOrder order = orderMapper.selectById(items.get(0).getOrderId());
        //         return order != null ? order.getOrderNo() : null;
        //     }
        // } catch (Exception e) {
        //     log.error("通过token反查订单失败，token={}", token, e);
        // }
        // return null;

        // ── 1. 先查 Redis（快） ──
        String json = redisCacheUtil.get("activity:result:" + token);
        if (json != null) {
            Map<String, Object> map = JSON.parseObject(json, Map.class);
            if (map.get("orderNo") != null) {
                return map.get("orderNo").toString();
            }
        }

        // ── 2. Redis 没有，查数据库兜底（慢但可靠） ──
        // TODO: 给 mall_order 表加 activity_token 字段 + 索引，然后按 token 查
        // 临时方案：遍历最近 10 分钟的订单（性能差，仅过渡）
        // TODO [性能隐患] 当前为全表扫描 + 逐条查 order_item + String.contains JSON 搜索。活动订单量增大后极慢。必须按上方 TODO 所述给 mall_order 加 activity_token 字段 + 索引
        try {
            LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
            LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(MallOrder::getCreateTime, tenMinutesAgo)
                    .eq(MallOrder::getOrderType, 2); // 活动订单
            List<MallOrder> orders = orderMapper.selectList(wrapper);
            for (MallOrder order : orders) {
                // 查订单明细的 snapshot_json 里是否有这个 token
                LambdaQueryWrapper<MallOrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(MallOrderItem::getOrderId, order.getId());
                MallOrderItem item = orderItemMapper.selectOne(itemWrapper);
                if (item != null && item.getSnapshotJson() != null
                        && item.getSnapshotJson().contains("\"token\":\"" + token + "\"")) {
                    return order.getOrderNo();
                }
            }
        } catch (Exception e) {
            log.error("通过token兜底查询订单失败，token={}", token, e);
        }

        return null;
    }


    // ═══════════════════════════════════════════════════════
    //  九、支付相关
    // ═══════════════════════════════════════════════════════

    @Override
    public String generatePayHtml(String orderNo, Long loginUserId) {
        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallOrder::getOrderNo, orderNo);
        MallOrder order = orderMapper.selectOne(wrapper);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(loginUserId)) {
            throw new RuntimeException("无权操作该订单");
        }
        if (!order.getStatus().equals(0)) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "订单已支付/已关闭，不可重复支付");
        }

        LambdaQueryWrapper<MallOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MallOrderItem::getOrderId, order.getId());
        List<MallOrderItem> itemList = orderItemMapper.selectList(itemWrapper);
        String goodsName = itemList.get(0).getGoodsName();
        if (itemList.size() > 1) {
            goodsName += " 等多件贵金属商品";
        }

        return alipayService.createPayForm(orderNo, order.getPayAmount(), goodsName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderPaySuccess(String orderNo, String tradeNo) {
        // ── 1. 幂等控制 ──
        String idempKey = "order:paid:" + orderNo;
        boolean firstDeal = redisCacheUtil.trySet(idempKey, "1", 2, TimeUnit.HOURS);
        if (!firstDeal) {
            log.warn("支付回调重复处理，orderNo={}", orderNo);
            return;
        }

        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallOrder::getOrderNo, orderNo);
        MallOrder order = orderMapper.selectOne(wrapper);

        if (order == null) {
            log.error("支付回调找不到对应订单，orderNo={}", orderNo);
            return;
        }
        if (order.getStatus() == 1) {
            log.warn("订单已支付，重复回调 orderNo={}", orderNo);
            return;
        }

        // 【已修复】支付 vs 取消竞态条件
        // 原问题(todo):
        // 只检查 status==1，不检查 status==5
        // 延时取消消费者解冻库存后、改状态前，支付回调可能插入
        // 导致对已取消订单标记已支付并扣库存
        // 修复(2024-07-10): 增加 status==5 检查，已取消订单直接忽略支付回调
        if (order.getStatus() == 5) {
            log.warn("订单已取消，忽略支付回调，orderNo={}", orderNo);
            return;
        }

        // ── 2. 修改订单状态 ──
        order.setStatus(1);
        order.setPayTradeNo(tradeNo);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        LambdaQueryWrapper<MallOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MallOrderItem::getOrderId, order.getId());
        List<MallOrderItem> itemList = orderItemMapper.selectList(itemWrapper);

        // ── 3. 【关键】写入本地消息表（同一事务内） ──
        for (MallOrderItem item : itemList) {
            String bizKey = orderNo + ":" + item.getSpecId();

            DeductStockMqMessage mqMessage = new DeductStockMqMessage();
            mqMessage.setOrderNo(orderNo);
            mqMessage.setSpecId(item.getSpecId());
            mqMessage.setQuantity(item.getQuantity());
            mqMessage.setBizKey(bizKey);

            MqMessageLog logRecord = MqMessageLog.builder()
                    .bizType("DEDUCT_STOCK")
                    .bizKey(bizKey)
                    .exchange(RabbitMqConstants.STOCK_EXCHANGE)
                    .routingKey(RabbitMqConstants.ROUTE_KEY_DEDUCT_STOCK)
                    .payload(JSON.toJSONString(mqMessage))
                    .status(0) // PENDING
                    .retryCount(0)
                    .maxRetry(5)
                    .build();

            messageLogMapper.insert(logRecord);
        }

        // ── 4. 【关键】事务提交后，立即尝试发送MQ ──
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (MallOrderItem item : itemList) {
                    String bizKey = orderNo + ":" + item.getSpecId();
                    sendStockDeductMessage(bizKey);
                }
            }
        });

        // TODO 【分布式事务优化 - 库存扣减补偿】
        // 当前：支付成功后同步 Feign 扣减库存，失败只打日志不抛异常。
        // 问题：如果库存扣减失败，订单已支付但库存未扣，数据不一致。
        // 优化方向：
        //   1. 引入本地消息表，定时任务异步补偿扣减库存
        //   2. 接入 Seata TCC 模式，确保库存扣减和订单状态原子性
        //   3. 发告警通知，人工介入补偿

        // TODO: 发告警，人工补偿

        // ── 5. 取消延迟关单 ──
        orderMqProducer.sendCancelDelayOrder(orderNo);
        log.info("订单{}支付成功，已发送库存扣减消息", orderNo);
    }


    // ═══════════════════════════════════════════════════════
    //  十、发送库存扣减消息
    // ═══════════════════════════════════════════════════════

    /**
     * 根据本地消息表记录发送MQ
     */
    public void sendStockDeductMessage(String bizKey) {
        MqMessageLog logRecord = messageLogMapper.selectOne(
                new LambdaQueryWrapper<MqMessageLog>()
                        .eq(MqMessageLog::getBizKey, bizKey)
        );

        if (logRecord == null || logRecord.getStatus() != 0) {
            return;
        }

        try {
            rabbitTemplate.convertAndSend(
                    logRecord.getExchange(),
                    logRecord.getRoutingKey(),
                    logRecord.getPayload(),
                    message -> {
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setMessageId(logRecord.getBizKey());
                        return message;
                    }
            );

            logRecord.setStatus(1);
            logRecord.setSendTime(LocalDateTime.now());
            messageLogMapper.updateById(logRecord);

            log.info("库存扣减消息发送成功，bizKey={}", bizKey);

        } catch (Exception e) {
            log.error("【网络/MQ异常】库存扣减消息发送失败，bizKey={}，等待定时任务重试", bizKey, e);
        }
    }


    // ═══════════════════════════════════════════════════════
    //  十一、订单查询
    // ═══════════════════════════════════════════════════════

    @Override
    public OrderDetailVO getOrderDetail(String orderNo) {
        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getUserId, UserContext.getUserId());
        MallOrder order = orderMapper.selectOne(wrapper);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        }

        LambdaQueryWrapper<MallOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MallOrderItem::getOrderId, order.getId());
        MallOrderItem item = orderItemMapper.selectOne(itemWrapper);

        // 【已修复】空指针风险
        // 原问题(todo):
        // 未校验 storeResult 是否为 null，直接调用 getData()
        // Feign 配置错误时可能 NPE
        // 修复(2024-07-10): 增加 null 检查，与 getUserInfo/getGoodsSpecInfo 保持一致
        Result<StoreInfoDTO> storeResult = storeFeignApi.getStoreInfo(order.getStoreId());
        if (storeResult == null) {
            throw new RuntimeException("调用门店服务无返回");
        }
        if (storeResult.isError()) {
            throw new RuntimeException("查询门店信息失败：" + storeResult.getMsg());
        }
        StoreInfoDTO store = storeResult.getData();

        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setPayAmount(order.getPayAmount());
        vo.setCreateTime(order.getCreateTime());
        vo.setGoodsName(item.getGoodsName());
        vo.setGoodsImage(item.getGoodsImage());
        vo.setWeight(item.getWeight());
        vo.setQuantity(item.getQuantity());
        vo.setUnitPrice(item.getUnitPrice());

        if (store != null) {
            vo.setStoreName(store.getStoreName());
            vo.setStoreAddress(
                    store.getProvince()
                            + store.getCity()
                            + store.getDistrict()
                            + store.getAddress()
            );
            vo.setStorePhone(store.getPhone());
            vo.setBusinessHours(store.getBusinessHours());
        }

        return vo;
    }


    // ═══════════════════════════════════════════════════════
    //  十二、Feign 调用封装
    // ═══════════════════════════════════════════════════════

    public UserInfoDTO getUserInfo(Long userId) {
        Result<UserInfoDTO> result = userFeignApi.getUserInfo(userId);
        if (result == null) {
            throw new RuntimeException("调用用户服务无返回");
        }
        if (result.isError()) {
            throw new RuntimeException("查询用户信息失败：" + result.getMsg());
        }
        UserInfoDTO userInfoDTO = result.getData();
        if (userInfoDTO == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        return userInfoDTO;
    }

    public SpecInfoDTO getGoodsSpecInfo(Long specId) {
        Result<SpecInfoDTO> result = productFeignApi.getSpecInfo(specId);
        if (result == null) {
            throw new RuntimeException("调用商品服务查询规格无返回结果");
        }
        if (result.isError()) {
            throw new RuntimeException("查询商品规格失败：" + result.getMsg());
        }
        SpecInfoDTO specInfoDTO = result.getData();
        if (specInfoDTO == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品规格数据不存在");
        }
        return specInfoDTO;
    }

    // ==================== 订单列表查询 ====================

    @Override
    public PageResult<OrderListVO> listOrders(Long userId, Integer status, Integer page, Integer size) {
        // 分页参数兜底防护
        page = page == null || page < 1 ? 1 : page;
        size = size == null || size < 1 ? 10 : Math.min(size, 100);

        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallOrder::getUserId, userId);
        wrapper.eq(status != null, MallOrder::getStatus, status);
        wrapper.orderByDesc(MallOrder::getCreateTime);

        Page<MallOrder> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);
        List<MallOrder> orderRecords = orderPage.getRecords();
        if (orderRecords.isEmpty()) {
            return PageResult.build(Collections.emptyList(), orderPage.getTotal(), page, size);
        }

        // 1. 提取所有订单ID，批量一次性查明细，解决N+1
        List<Long> orderIdList = orderRecords.stream()
                .map(MallOrder::getId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<MallOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(MallOrderItem::getOrderId, orderIdList);
        List<MallOrderItem> allItems = orderItemMapper.selectList(itemWrapper);
        // 按orderId分组
        Map<Long, List<MallOrderItem>> itemGroupMap = allItems.stream()
                .collect(Collectors.groupingBy(MallOrderItem::getOrderId));

        // 2. 组装VO
        List<OrderListVO> voList = orderRecords.stream().map(order -> {
            List<MallOrderItem> items = itemGroupMap.getOrDefault(order.getId(), Collections.emptyList());
            String goodsName = "未知商品";
            String goodsImage = "";
            Integer quantity = 0;
            if (!items.isEmpty()) {
                MallOrderItem firstItem = items.get(0);
                goodsName = firstItem.getGoodsName();
                goodsImage = firstItem.getGoodsImage();
                quantity = firstItem.getQuantity();
                if (items.size() > 1) {
                    goodsName += " 等" + items.size() + "件商品";
                }
            }

            return OrderListVO.builder()
                    .orderNo(order.getOrderNo())
                    .status(order.getStatus())
                    .statusName(getStatusName(order.getStatus()))
                    .payAmount(order.getPayAmount())
                    .goodsName(goodsName)
                    .goodsImage(goodsImage)
                    .quantity(quantity)
                    .createTime(order.getCreateTime())
                    .payTime(order.getPayTime())
                    .build();
        }).collect(Collectors.toList());

        // 统一使用全局PageResult构建方法，废弃new构造器
        return PageResult.build(voList, orderPage.getTotal(), page, size);
    }

    // 订单状态映射常量，统一管理，不再零散switch
    private static final Map<Integer, String> ORDER_STATUS_MAP = Map.of(
            0, "待支付",
            1, "已支付",
            2, "待核销",
            3, "已核销",
            4, "已完成",
            5, "已取消",
            6, "退款中",
            7, "已退款"
    );

    private String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return ORDER_STATUS_MAP.getOrDefault(status, "未知");
    }

// ==================== 手动取消订单 ====================

// ==================== 手动取消订单（修正版） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        // 1. 查订单并校验归属
        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getUserId, userId);
        MallOrder order = orderMapper.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        }

        // 2. 仅待支付可取消
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单状态异常，仅待支付订单可取消");
        }

        // 3. 更新状态为已取消
        order.setStatus(5);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 4. 查订单明细，逐条解冻库存
        LambdaQueryWrapper<MallOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MallOrderItem::getOrderId, order.getId());
        List<MallOrderItem> items = orderItemMapper.selectList(itemWrapper);

        for (MallOrderItem item : items) {
            if (order.getOrderType() == 2 && order.getActivityId() != null) {
                // 活动订单：调 Feign 解冻活动库存
                DeductActivityStockRequest req = new DeductActivityStockRequest();
                req.setActivityId(order.getActivityId());
                req.setSpecId(item.getSpecId());
                req.setQuantity(item.getQuantity());

                Result<Void> result = productFeignApi.unFreezeActivityStock(req);
                if (result == null || result.isError()) {
                    log.error("活动库存解冻失败，orderNo={}, activityId={}, specId={}",
                            orderNo, order.getActivityId(), item.getSpecId());
                    // TODO: 记录补偿日志，定时任务重试
                }
            } else {
                // 普通订单：调 Feign 解冻普通库存
                DeductStockRequest req = new DeductStockRequest();
                req.setSpecId(item.getSpecId());
                req.setQuantity(item.getQuantity());
                req.setOrderNo(orderNo);

                Result<Void> result = productFeignApi.unFreezeStock(req);
                if (result == null || result.isError()) {
                    log.error("库存解冻失败，orderNo={}, specId={}", orderNo, item.getSpecId());
                    // TODO: 记录补偿日志，定时任务重试
                }
            }
        }

        // 5. 取消延迟关单MQ（如果还在队列里）
        orderMqProducer.sendCancelDelayOrder(orderNo);

        log.info("订单手动取消成功，orderNo={}, userId={}", orderNo, userId);
    }


// ==================== 支付前校验 ====================

    // ==================== 支付前校验（修正版）====================
    @Override
    public OrderValidVO validateOrder(String orderNo, Long userId) {
        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getUserId, userId);
        MallOrder order = orderMapper.selectOne(wrapper);

        if (order == null) {
            return OrderValidVO.builder()
                    .valid(false)
                    .orderNo(orderNo)
                    .failReason("订单不存在")
                    .build();
        }

        if (order.getStatus() == 1) {
            return OrderValidVO.builder()
                    .valid(false)
                    .orderNo(orderNo)
                    .payAmount(order.getPayAmount())
                    .status(order.getStatus())
                    .failReason("订单已支付，请勿重复支付")
                    .build();
        }

        if (order.getStatus() == 5) {
            return OrderValidVO.builder()
                    .valid(false)
                    .orderNo(orderNo)
                    .payAmount(order.getPayAmount())
                    .status(order.getStatus())
                    .failReason("订单已取消，请重新下单")
                    .build();
        }

        if (order.getStatus() != 0) {
            return OrderValidVO.builder()
                    .valid(false)
                    .orderNo(orderNo)
                    .payAmount(order.getPayAmount())
                    .status(order.getStatus())
                    .failReason("订单状态异常，不可支付")
                    .build();
        }

        // 计算剩余支付时间
        long createTimestamp = order.getCreateTime()
                .atZone(java.time.ZoneId.systemDefault())
                .toEpochSecond();
        long nowTimestamp = System.currentTimeMillis() / 1000;
        long expireSeconds = 15 * 60 - (nowTimestamp - createTimestamp);

        // 已超时：只返回状态，不自动取消
        if (expireSeconds <= 0) {
            return OrderValidVO.builder()
                    .valid(false)
                    .orderNo(orderNo)
                    .payAmount(order.getPayAmount())
                    .status(order.getStatus())
                    .failReason("订单已超时，请重新下单")
                    .expireSeconds(0L)
                    .build();
        }

        // 查商品名
        LambdaQueryWrapper<MallOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MallOrderItem::getOrderId, order.getId());
        MallOrderItem item = orderItemMapper.selectOne(itemWrapper);
        String goodsName = item != null ? item.getGoodsName() : "未知商品";

        return OrderValidVO.builder()
                .valid(true)
                .orderNo(orderNo)
                .payAmount(order.getPayAmount())
                .goodsName(goodsName)
                .status(order.getStatus())
                .expireSeconds(expireSeconds)
                .build();
    }
}