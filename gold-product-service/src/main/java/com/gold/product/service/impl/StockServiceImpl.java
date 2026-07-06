package com.gold.product.service.impl;

import com.gold.api.domain.dto.DeductActivityStockRequest;
import com.gold.api.domain.dto.DeductStockRequest;
import com.gold.api.domain.dto.MarketPriceDTO;
import com.gold.api.domain.dto.SpecInfoDTO;
import com.gold.api.feign.MarketFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.exception.BusinessException;
import com.gold.common.result.Result;
import com.gold.product.entity.ActivityGoodsInfo;
import com.gold.product.entity.MallGoods;
import com.gold.product.entity.MallGoodsSpec;
import com.gold.product.entity.StockDeductRecord;
import com.gold.product.mapper.*;
import com.gold.product.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final MallGoodsSpecMapper specMapper;
    private final MallGoodsMapper goodsMapper;
    private final ActivityGoodsMapper activityGoodsMapper;
    private final MallActivityGoodsMapper activityGoodsWriteMapper;
    private final MarketFeignApi marketFeignApi;
    private final StockDeductRecordMapper deductRecordMapper;


    @Override
    public SpecInfoDTO getSpecInfoById(Long specId) {
        MallGoodsSpec spec = specMapper.selectById(specId);
        if (spec == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品规格不存在");
        }

        MallGoods goods = goodsMapper.selectById(spec.getGoodsId());
        if (goods == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "对应商品不存在");
        }

        BigDecimal realTimePrice = getReferencePrice(goods.getMetalType());
        BigDecimal realTimeTotalPrice = calculateTotalPrice(spec.getWeight(), realTimePrice, spec.getCraftFee());

        ActivityGoodsInfo activity = activityGoodsMapper.selectOngoingBySpecId(specId, LocalDateTime.now());

        SpecInfoDTO dto = new SpecInfoDTO();
        dto.setGoodsId(goods.getId());
        dto.setGoodsName(goods.getGoodsName());
        dto.setCategory(goods.getCategory());
        dto.setMetalType(goods.getMetalType());
        dto.setStoreId(goods.getStoreId());

        dto.setSpecId(spec.getId());
        dto.setWeight(spec.getWeight());
        dto.setCraftFee(spec.getCraftFee());

        dto.setReferencePricePerGram(spec.getPricePerGram());
        dto.setReferenceTotalPrice(spec.getTotalPrice());

        dto.setRealTimePricePerGram(realTimePrice);
        dto.setRealTimeTotalPrice(realTimeTotalPrice);

        if (activity != null) {
            dto.setActivityId(activity.getActivityId());
            dto.setActivityName(activity.getActivityName());
            dto.setActivityPrice(activity.getActivityPrice());
            dto.setOriginalPrice(activity.getOriginalPrice());
            dto.setActivityStock(activity.getActivityStock() - activity.getSoldCount());
            dto.setLimitPerUser(activity.getLimitPerUser());
        }

        dto.setStock(spec.getStock());
        dto.setFrozenStock(spec.getFrozenStock());
        dto.setAvailableStock(spec.getStock() - spec.getFrozenStock());
        dto.setCoverImage(spec.getCoverImage());
        dto.setSaleStatus(spec.getSaleStatus());

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(DeductStockRequest request) {
        String bizKey = request.getOrderNo() + ":" + request.getSpecId();

        // 【关键改造】幂等：先尝试插入，靠唯一索引防重
        // 而不是先 SELECT COUNT(*) 再 INSERT（并发时可能同时查到0）
        StockDeductRecord record = new StockDeductRecord();
        record.setBizKey(bizKey);
        record.setOrderNo(request.getOrderNo());
        record.setSpecId(request.getSpecId());
        record.setQuantity(request.getQuantity());

        try {
            deductRecordMapper.insert(record);
            log.info("幂等记录插入成功，bizKey={}", bizKey);
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突，说明已经处理过，直接返回
            log.info("库存已扣减，幂等跳过，bizKey={}", bizKey);
            return;
        }

        // 插入成功，继续执行库存扣减
        // 1. 先解冻（下单时冻结的库存）
        int unfreezeRows = specMapper.unFreezeStock(request.getSpecId(), request.getQuantity());
        if (unfreezeRows <= 0) {
            throw new RuntimeException("解冻库存失败，可能库存已变动");
        }

        // 2. 再扣减实际库存
        int deductRows = specMapper.deductStockOnly(request.getSpecId(), request.getQuantity());
        if (deductRows <= 0) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存不足");
        }

        log.info("库存扣减成功，orderNo={}, specId={}, quantity={}",
                request.getOrderNo(), request.getSpecId(), request.getQuantity());
    }

    @Override
    public void freezeStock(DeductStockRequest request) {
        int rows = specMapper.freezeStock(request.getSpecId(), request.getQuantity());
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存不足");
        }
    }

    @Override
    public void unFreezeStock(DeductStockRequest request) {
        int rows = specMapper.unFreezeStock(request.getSpecId(), request.getQuantity());
        if (rows <= 0) {
            throw new RuntimeException("解冻库存失败");
        }
    }

    @Override
    public void deductActivityStock(DeductActivityStockRequest request) {
        int rows = activityGoodsWriteMapper.increaseSoldCount(
                request.getActivityId(), request.getSpecId(), request.getQuantity());
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "活动库存扣减失败");
        }
    }

    @Override
    public void unFreezeActivityStock(DeductActivityStockRequest request) {
        int rows = activityGoodsWriteMapper.decreaseSoldCount(
                request.getActivityId(), request.getSpecId(), request.getQuantity());
        if (rows <= 0) {
            throw new RuntimeException("活动库存回滚失败");
        }
    }

    // ==================== 私有方法 ====================

    private BigDecimal getReferencePrice(String metalType) {
        Result<MarketPriceDTO> result = marketFeignApi.getLatestPrice(metalType);
        if (result == null || result.getData() == null) {
            throw new BusinessException(ErrorCode.MARKET_PRICE_FETCH_FAILED, "获取行情价失败");
        }

        BigDecimal latestPrice = result.getData().getLatestPrice();
        if (latestPrice == null || latestPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.MARKET_PRICE_FETCH_FAILED, "行情价格异常");
        }

        return latestPrice;
    }

    private BigDecimal calculateTotalPrice(BigDecimal weight, BigDecimal pricePerGram, BigDecimal craftFee) {
        if (weight == null || pricePerGram == null || craftFee == null) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "重量、金价、工费不能为空");
        }
        return weight.multiply(pricePerGram).add(weight.multiply(craftFee))
                .setScale(2, RoundingMode.HALF_UP);
    }
}