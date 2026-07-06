package com.gold.market.service;

import com.gold.api.domain.dto.MarketPriceDTO;
import com.gold.market.domain.po.Kline1m;

import java.util.List;

public interface MarketPriceService {

    /** @return true=真实API调用成功, false=降级Mock */
    boolean syncAllMetalPrice();

    MarketPriceDTO getLatestPrice(String metalType);

    void generateMockPrice();

    List<Kline1m> selectTodayKline(String metalType);

    /** 每分钟基于基准价随机波动 */
    void fluctuatePrice();

    /** 每分钟自动生成1分钟K线 */
    void generateMinuteKline();

    /** 每分钟心跳：校准 → 波动 → K线 → 推送 */
    void heartbeat();
}