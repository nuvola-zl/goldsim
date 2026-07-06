package com.gold.product.service;

import com.gold.api.domain.dto.DeductActivityStockRequest;
import com.gold.api.domain.dto.DeductStockRequest;
import com.gold.api.domain.dto.SpecInfoDTO;

public interface StockService {


    /**
     * 根据规格id查询商品规格详情
     */
    SpecInfoDTO getSpecInfoById(Long specId);

    /**
     * 扣减库存（基础版本，无分布式锁）
     */
    void deductStock(DeductStockRequest request);

    // 下单冻结库存
    void freezeStock(DeductStockRequest request);

    // 超时/取消订单解冻库存
    void unFreezeStock(DeductStockRequest request);


    void deductActivityStock(DeductActivityStockRequest request);

    void unFreezeActivityStock(DeductActivityStockRequest request);
}