package com.gold.simulation.service;

import com.gold.simulation.domain.dto.GoldBuyDTO;
import com.gold.simulation.domain.dto.GoldSellDTO;
import com.gold.simulation.domain.vo.BuyResultVO;
import com.gold.simulation.domain.vo.SellResultVO;
import jakarta.validation.Valid;

public interface ISimulationTradeService {
    /**
     * 买入
     *
     * @param goldBuyDTO
     * @return
     */
    BuyResultVO buy(@Valid GoldBuyDTO goldBuyDTO);

    /**
     * 卖出
     *
     * @param goldSellDTO
     * @return
     */
    SellResultVO sell(@Valid GoldSellDTO goldSellDTO);
}
