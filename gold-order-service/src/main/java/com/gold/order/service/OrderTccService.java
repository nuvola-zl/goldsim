package com.gold.order.service;

import com.gold.api.domain.dto.SpecInfoDTO;
import com.gold.order.domain.dto.OrderCreateDTO;

/**
 * TCC分布式事务专用Service
 * 单独抽出，确保@GlobalTransactional通过Spring代理生效
 */
public interface OrderTccService {

    /**
     * 创建订单（TCC模式）
     * 有@GlobalTransactional，调用product-service的TCC冻结库存
     */
    String createOrderWithTcc(Long userId, Long specId, Integer buyNum, 
                               Long storeId, String remark, SpecInfoDTO spec, boolean isActivity);
}