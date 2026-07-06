package com.gold.simulation.service;

import com.gold.simulation.domain.dto.GoldBuyDTO;
import com.gold.simulation.domain.dto.GoldSellDTO;
import com.gold.simulation.domain.vo.BuyResultVO;
import com.gold.simulation.domain.vo.SellResultVO;

public interface TradeCoreService {
    /**
     * 买入
     * @param dto
     * @return
     */
    BuyResultVO doBuy(GoldBuyDTO dto);

    /**
     * 卖出
     * @param dto
     * @return
     */
    SellResultVO doSell(GoldSellDTO dto);
}
