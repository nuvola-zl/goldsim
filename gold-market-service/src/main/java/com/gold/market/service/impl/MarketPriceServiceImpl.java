package com.gold.market.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gold.api.domain.dto.MarketPriceDTO;
import com.gold.market.domain.po.Kline1m;
import com.gold.market.domain.po.MarketPriceSnapshot;
import com.gold.market.mapper.Kline1mMapper;
import com.gold.market.mapper.MarketPriceSnapshotMapper;
import com.gold.market.service.MarketPriceService;
import com.gold.market.websocket.MarketWebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketPriceServiceImpl implements MarketPriceService {

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final MarketPriceSnapshotMapper snapshotMapper;
    private final Kline1mMapper kline1mMapper;

    // ========== Redis Key ==========
    private static final String REDIS_KEY_PREFIX = "market:price:";
    private static final String BASE_KEY_PREFIX = "market:price:base:";
    private static final long CACHE_EXPIRE = 35;

    // ========== API ==========
    private static final String JUHE_URL = "https://web.juhe.cn/finance/gold/shgold";

    // ========== 波动率 ==========
    private static final BigDecimal AU_VOLATILITY = new BigDecimal("0.0015");
    private static final BigDecimal AG_VOLATILITY = new BigDecimal("0.0030");
    private static final BigDecimal PT_VOLATILITY = new BigDecimal("0.0020");
    private static final double MINUTES_PER_DAY = 240.0;

    // ========== yaml兜底价 ==========
    @Value("${market.fallback.au:725.32}")
    private BigDecimal fallbackAu;
    @Value("${market.fallback.ag:8.42}")
    private BigDecimal fallbackAg;
    @Value("${market.fallback.pt:320.15}")
    private BigDecimal fallbackPt;

    @Value("${market.api.juhe-key:}")
    private String juheKey;

    // 内存缓存：上一分钟收盘价
    private final ConcurrentHashMap<String, BigDecimal> lastClosePrices = new ConcurrentHashMap<>();

    // ==================== 启动恢复 ====================

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("========== 行情服务启动，恢复行情种子 ==========");
        for (String metal : Arrays.asList("AU", "AG", "PT")) {
            MarketPriceDTO dto = recoverFromDbOrFallback(metal);
            // 启动时同时写 priceKey 和 baseKey，保证后续波动和K线都能工作
            saveToRedis(dto, true);
            log.info("[{}] 启动恢复完成，初始价: {} (mock={})",
                    metal, dto.getLatestPrice(), dto.isMockData());
        }
        log.info("========== 行情服务就绪 ==========");
    }

    private MarketPriceDTO recoverFromDbOrFallback(String metal) {
        MarketPriceSnapshot snap = snapshotMapper.selectOne(
                new LambdaQueryWrapper<MarketPriceSnapshot>()
                        .eq(MarketPriceSnapshot::getMetalType, metal)
                        .orderByDesc(MarketPriceSnapshot::getQuoteTime)
                        .last("LIMIT 1"));

        if (snap != null && snap.getLatestPrice() != null
                && snap.getLatestPrice().compareTo(BigDecimal.ZERO) > 0) {
            return convertSnapshotToDto(snap, true);
        }
        return buildFallbackDto(metal);
    }

    private MarketPriceDTO convertSnapshotToDto(MarketPriceSnapshot snap, boolean mock) {
        MarketPriceDTO dto = new MarketPriceDTO();
        dto.setMetalType(snap.getMetalType());
        dto.setMetalName(snap.getMetalName());
        dto.setLatestPrice(snap.getLatestPrice());
        dto.setOpenPrice(snap.getOpenPrice());
        dto.setHighPrice(snap.getHighPrice());
        dto.setLowPrice(snap.getLowPrice());
        dto.setPreClosePrice(snap.getPreClosePrice());
        dto.setChangePercent(snap.getChangePercent());
        dto.setUpdateTime(snap.getQuoteTime());
        dto.setMockData(mock);
        return dto;
    }

    private MarketPriceDTO buildFallbackDto(String metal) {
        BigDecimal price;
        String name;
        switch (metal) {
            case "AG": price = fallbackAg; name = "上海白银"; break;
            case "PT": price = fallbackPt; name = "上海铂金"; break;
            default:   price = fallbackAu; name = "上海黄金"; break;
        }
        MarketPriceDTO dto = new MarketPriceDTO();
        dto.setMetalType(metal);
        dto.setMetalName(name);
        dto.setLatestPrice(price);
        dto.setOpenPrice(price);
        dto.setHighPrice(price);
        dto.setLowPrice(price);
        dto.setPreClosePrice(price);
        dto.setChangePercent("0.00%");
        dto.setUpdateTime(LocalTime.now());
        dto.setMockData(true);
        return dto;
    }

    // ==================== 心跳（供定时任务调用） ====================

    @Override
    public void heartbeat() {
        LocalDateTime now = LocalDateTime.now();

        // 整点校准：一次API请求处理所有品种
        if (now.getMinute() == 0) {
            calibrateAllMetals();
        }

        // 每个品种独立心跳（波动 + K线 + 推送）
        for (String metal : Arrays.asList("AU", "AG", "PT")) {
            try {
                doHeartbeat(metal, now);
            } catch (Exception e) {
                log.error("[{}] 心跳异常", metal, e);
            }
        }
    }

    /**
     * 整点校准：一次API请求，更新所有匹配的品种
     */
    private void calibrateAllMetals() {
        log.info("整点校准所有品种...");
        if (!StringUtils.hasText(juheKey)) {
            log.warn("JuheKey未配置，跳过校准");
            return;
        }
        try {
            String url = JUHE_URL + "?key=" + juheKey + "&v=1";
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSON.parseObject(response);

            if (!"200".equals(json.getString("resultcode"))) {
                log.warn("API异常: {}", json.getString("reason"));
                return;
            }
            JSONArray result = json.getJSONArray("result");
            if (result == null || result.isEmpty()) {
                log.warn("API无数据，跳过校准");
                return;
            }

            JSONObject data = result.getJSONObject(0);
            for (String key : data.keySet()) {
                JSONObject item = data.getJSONObject(key);
                String variety = item.getString("variety");
                String metalType = mapVariety(variety);
                if (metalType == null) continue;

                BigDecimal latestPrice = getSafeBigDecimal(item, "latestpri");
                BigDecimal openAllDay = getSafeBigDecimal(item, "openpri");
                BigDecimal highAllDay = getSafeBigDecimal(item, "maxpri");
                BigDecimal lowAllDay = getSafeBigDecimal(item, "minpri");
                BigDecimal preClose = getSafeBigDecimal(item, "yespri");
                String changePercent = item.getString("limit");
                LocalTime quoteTime = parseTime(item.getString("time"));

                MarketPriceDTO dto = new MarketPriceDTO();
                dto.setMetalType(metalType);
                dto.setMetalName(variety);
                dto.setLatestPrice(latestPrice);
                dto.setOpenPrice(openAllDay);
                dto.setHighPrice(highAllDay);
                dto.setLowPrice(lowAllDay);
                dto.setPreClosePrice(preClose);
                dto.setChangePercent(changePercent);
                dto.setUpdateTime(quoteTime);
                dto.setMockData(false);

                saveToRedis(dto, true);
                saveSnapshot(metalType, variety, latestPrice, openAllDay,
                        highAllDay, lowAllDay, preClose, changePercent, quoteTime);

                log.info("[校准][{}] 真实价: {}", variety, latestPrice);
                MarketWebSocketServer.sendMarketData(metalType, dto);
            }
        } catch (Exception e) {
            log.error("整点校准失败", e);
        }
    }

    private void doHeartbeat(String metal, LocalDateTime now) {
        // 1. 读当前价
        MarketPriceDTO dto = getCurrentPrice(metal);
        BigDecimal basePrice = dto.getLatestPrice();

        // 2. 波动
        double change = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.006;
        BigDecimal newPrice = basePrice
                .multiply(BigDecimal.valueOf(1 + change))
                .setScale(metal.equals("AG") ? 3 : 2, RoundingMode.HALF_UP);

        dto.setLatestPrice(newPrice);
        dto.setUpdateTime(LocalTime.now());

        // 3. 写Redis（不写baseKey，避免污染真实基准；mockData保留原值不覆盖）
        saveToRedis(dto, false);

        // 4. K线
        generateMinuteKline(metal, newPrice, now);

        // 5. 推送
        MarketWebSocketServer.sendMarketData(metal, dto);

        log.debug("[{}] 心跳: {} -> {}", metal, basePrice, newPrice);
    }

    // ==================== 原有方法：syncAllMetalPrice（手动调用/测试用） ====================

    @Override
    public boolean syncAllMetalPrice() {
        log.info("开始拉取聚合数据上海金实时行情");

        if (!StringUtils.hasText(juheKey)) {
            log.error("聚合数据密钥未配置，生成Mock兜底数据");
            generateMockPrice();
            return false;
        }

        try {
            String url = JUHE_URL + "?key=" + juheKey + "&v=1";
            String response = restTemplate.getForObject(url, String.class);
            log.info("聚合接口原始返回: {}", response);

            JSONObject json = JSON.parseObject(response);
            String resultCode = json.getString("resultcode");

            if (!"200".equals(resultCode)) {
                log.warn("聚合接口调用失败，code={} msg={}", resultCode, json.getString("reason"));
                generateMockPrice();
                return false;
            }

            JSONArray result = json.getJSONArray("result");
            if (result == null || result.isEmpty()) {
                log.warn("当前休市，生成Mock兜底行情");
                generateMockPrice();
                return false;
            }

            JSONObject data = result.getJSONObject(0);
            LocalDateTime now = LocalDateTime.now();

            for (String key : data.keySet()) {
                JSONObject item = data.getJSONObject(key);
                String variety = item.getString("variety");
                String metalType = mapVariety(variety);
                if (metalType == null) {
                    log.debug("未匹配的品种：{}", variety);
                    continue;
                }

                BigDecimal latestPrice = getSafeBigDecimal(item, "latestpri");
                BigDecimal openAllDay = getSafeBigDecimal(item, "openpri");
                BigDecimal highAllDay = getSafeBigDecimal(item, "maxpri");
                BigDecimal lowAllDay = getSafeBigDecimal(item, "minpri");
                BigDecimal preClose = getSafeBigDecimal(item, "yespri");
                String changePercent = item.getString("limit");
                LocalTime quoteTime = parseTime(item.getString("time"));

                MarketPriceDTO dto = new MarketPriceDTO();
                dto.setMetalType(metalType);
                dto.setMetalName(variety);
                dto.setLatestPrice(latestPrice);
                dto.setOpenPrice(openAllDay);
                dto.setHighPrice(highAllDay);
                dto.setLowPrice(lowAllDay);
                dto.setPreClosePrice(preClose);
                dto.setChangePercent(changePercent);
                dto.setUpdateTime(quoteTime);
                dto.setMockData(false);

                saveToRedis(dto, true);
                saveSnapshot(metalType, variety, latestPrice, openAllDay, highAllDay,
                        lowAllDay, preClose, changePercent, quoteTime);

                processKline(metalType, latestPrice, openAllDay, now);

                log.info("[{}] 现价:{} 涨跌幅:{}", variety, latestPrice, changePercent);
                MarketWebSocketServer.sendMarketData(metalType, dto);
            }

            return true;

        } catch (Exception e) {
            log.error("拉取聚合行情接口异常", e);
            generateMockPrice();
            return false;
        }
    }

    // ==================== 原有方法：fluctuatePrice（可手动调用） ====================

    @Override
    public void fluctuatePrice() {
        for (String metal : Arrays.asList("AU", "AG", "PT")) {
            String baseKey = BASE_KEY_PREFIX + metal;
            String priceKey = REDIS_KEY_PREFIX + metal;

            String baseJson = redisTemplate.opsForValue().get(baseKey);
            if (!StringUtils.hasText(baseJson)) {
                MarketPriceDTO dbDto = getLatestPrice(metal);
                if (dbDto != null && dbDto.getLatestPrice() != null
                        && dbDto.getLatestPrice().compareTo(BigDecimal.ZERO) > 0) {
                    saveToRedis(dbDto, true);
                    baseJson = JSON.toJSONString(dbDto);
                    log.info("[{}] Redis基准价缺失，已从DB快照恢复", metal);
                } else {
                    log.warn("[{}] 基准行情缺失且DB快照无数据，跳过波动", metal);
                    continue;
                }
            }

            MarketPriceDTO baseDto = JSON.parseObject(baseJson, MarketPriceDTO.class);
            BigDecimal basePrice = baseDto.getLatestPrice();
            if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            double change = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.006;
            BigDecimal newPrice = basePrice
                    .multiply(BigDecimal.valueOf(1 + change))
                    .setScale(2, RoundingMode.HALF_UP);

            MarketPriceDTO newDto = new MarketPriceDTO();
            BeanUtils.copyProperties(baseDto, newDto);
            newDto.setLatestPrice(newPrice);
            newDto.setUpdateTime(LocalTime.now());
            newDto.setMockData(false);

            redisTemplate.opsForValue().set(
                    priceKey,
                    JSON.toJSONString(newDto),
                    CACHE_EXPIRE,
                    TimeUnit.MINUTES
            );

            MarketWebSocketServer.sendMarketData(metal, newDto);
            log.debug("[{}] 行情波动: {} -> {}", metal, basePrice, newPrice);
        }
    }

    // ==================== 原有方法：generateMinuteKline（可手动调用） ====================

    @Override
    public void generateMinuteKline() {
        LocalDateTime now = LocalDateTime.now();
        for (String metalType : Arrays.asList("AU", "AG", "PT")) {
            try {
                generateMinuteKline(metalType, null, now);
            } catch (Exception e) {
                log.error("[{}] 生成分钟K线失败", metalType, e);
            }
        }
    }

    // ==================== 内部K线生成（心跳和手动调用共用） ====================

    private void generateMinuteKline(String metal, BigDecimal currentPrice, LocalDateTime now) {
        completePrevMinuteKline(metal, currentPrice, now);
        createCurrentMinuteKline(metal, currentPrice, now);
    }

    private void completePrevMinuteKline(String metal, BigDecimal closePrice, LocalDateTime now) {
        LocalDateTime prevMin = now.minusMinutes(1).withSecond(0).withNano(0);
        Kline1m prev = kline1mMapper.selectByMetalAndTime(metal, prevMin);
        if (prev != null && prev.getClosePrice() == null) {
            // 如果传入closePrice为null，从Redis读
            if (closePrice == null) {
                closePrice = getCurrentPriceFromRedis(metal);
            }
            if (closePrice != null) {
                prev.setClosePrice(closePrice);
                prev.setUpdateTime(now);
                kline1mMapper.updateById(prev);
                lastClosePrices.put(metal, closePrice);

                JSONObject msg = new JSONObject();
                msg.put("msgType", "KLINE");
                msg.put("metalType", metal);
                msg.put("timestamp", prevMin.toString());
                msg.put("openPrice", prev.getOpenPrice());
                msg.put("highPrice", prev.getHighPrice());
                msg.put("lowPrice", prev.getLowPrice());
                msg.put("closePrice", closePrice);
                msg.put("volume", prev.getVolume());
                MarketWebSocketServer.sendRaw(metal, msg.toJSONString());
            }
        }
    }

    private void createCurrentMinuteKline(String metal, BigDecimal currentPrice, LocalDateTime now) {
        LocalDateTime currMin = now.withSecond(0).withNano(0);
        Kline1m exist = kline1mMapper.selectByMetalAndTime(metal, currMin);
        if (exist != null) return;

        BigDecimal openPrice = lastClosePrices.get(metal);
        if (openPrice == null) {
            openPrice = currentPrice != null ? currentPrice : getCurrentPriceFromRedis(metal);
        }

        BigDecimal closePrice = currentPrice != null ? currentPrice : getCurrentPriceFromRedis(metal);
        if (closePrice == null) {
            closePrice = openPrice;
        }

        BigDecimal high = (openPrice != null && closePrice != null)
                ? openPrice.max(closePrice) : openPrice;
        BigDecimal low = (openPrice != null && closePrice != null)
                ? openPrice.min(closePrice) : openPrice;

        int scale = metal.equals("AG") ? 3 : 2;
        if (high != null) {
            high = high.multiply(BigDecimal.valueOf(1 + Math.random() * 0.002))
                    .setScale(scale, RoundingMode.HALF_UP);
        }
        if (low != null) {
            low = low.multiply(BigDecimal.valueOf(1 - Math.random() * 0.002))
                    .setScale(scale, RoundingMode.HALF_UP);
        }

        long volume = 50 + (long) (Math.random() * 200);

        Kline1m kline = new Kline1m();
        kline.setMetalType(metal);
        kline.setTimestamp(currMin);
        kline.setOpenPrice(openPrice);
        kline.setHighPrice(high);
        kline.setLowPrice(low);
        kline.setClosePrice(closePrice);
        kline.setVolume(volume);
        kline.setCreateTime(now);
        kline.setUpdateTime(now);
        kline1mMapper.insert(kline);

        lastClosePrices.put(metal, closePrice);

        JSONObject msg = new JSONObject();
        msg.put("msgType", "KLINE");
        msg.put("metalType", metal);
        msg.put("timestamp", currMin.toString());
        msg.put("openPrice", openPrice);
        msg.put("highPrice", high);
        msg.put("lowPrice", low);
        msg.put("closePrice", closePrice);
        msg.put("volume", volume);
        MarketWebSocketServer.sendRaw(metal, msg.toJSONString());

        log.debug("[{}] K线 {} O:{} H:{} L:{} C:{} V:{}",
                metal, currMin, openPrice, high, low, closePrice, volume);
    }

    // ==================== 原有方法：generateMockPrice（手动调用/测试用） ====================

    @Override
    public void generateMockPrice() {
        log.info("生成Mock行情数据兜底");
        LocalDateTime now = LocalDateTime.now();

        generateMockForMetal("AU", "上海黄金", readBaseOrDefault("AU", fallbackAu), now);
        generateMockForMetal("AG", "上海白银", readBaseOrDefault("AG", fallbackAg), now);
        generateMockForMetal("PT", "上海铂金", readBaseOrDefault("PT", fallbackPt), now);

        log.info("Mock行情写入完成");
    }

    private BigDecimal readBaseOrDefault(String metalType, BigDecimal fallback) {
        String baseKey = BASE_KEY_PREFIX + metalType;
        String json = redisTemplate.opsForValue().get(baseKey);
        if (StringUtils.hasText(json)) {
            try {
                MarketPriceDTO dto = JSON.parseObject(json, MarketPriceDTO.class);
                if (dto.getLatestPrice() != null && dto.getLatestPrice().compareTo(BigDecimal.ZERO) > 0) {
                    log.info("[{}] 使用Redis基准价兜底: {}", metalType, dto.getLatestPrice());
                    return dto.getLatestPrice();
                }
            } catch (Exception e) {
                log.warn("[{}] 解析Redis基准价失败，使用yaml兜底", metalType);
            }
        }
        log.info("[{}] Redis基准价不存在，使用yaml兜底: {}", metalType, fallback);
        return fallback;
    }

    private void generateMockForMetal(String metalType, String metalName,
                                      BigDecimal basePrice, LocalDateTime now) {
        double random = (Math.random() - 0.5) * 0.01;
        BigDecimal latestPrice = basePrice.multiply(BigDecimal.valueOf(1 + random))
                .setScale(2, RoundingMode.HALF_UP);

        MarketPriceDTO dto = new MarketPriceDTO();
        dto.setMetalType(metalType);
        dto.setMetalName(metalName);
        dto.setLatestPrice(latestPrice);
        dto.setOpenPrice(basePrice);
        dto.setHighPrice(latestPrice.max(basePrice));
        dto.setLowPrice(latestPrice.min(basePrice));
        dto.setPreClosePrice(basePrice);
        dto.setChangePercent(String.format("%+.2f%%", random * 100));
        dto.setUpdateTime(LocalTime.now());
        dto.setMockData(true);

        saveToRedis(dto, false);
        saveSnapshot(metalType, metalName, latestPrice, basePrice,
                latestPrice.max(basePrice), latestPrice.min(basePrice),
                basePrice, dto.getChangePercent(), dto.getUpdateTime());

        processKline(metalType, latestPrice, basePrice, now);
        MarketWebSocketServer.sendMarketData(metalType, dto);
    }

    // ==================== 原有方法：getLatestPrice（Feign/Controller调用） ====================

    @Override
    public MarketPriceDTO getLatestPrice(String metalType) {
        if (!StringUtils.hasText(metalType)) return null;

        String key = REDIS_KEY_PREFIX + metalType;
        String json = redisTemplate.opsForValue().get(key);

        if (StringUtils.hasText(json)) {
            return JSON.parseObject(json, MarketPriceDTO.class);
        }

        log.warn("Redis 无 [{}] 行情，从数据库兜底读取", metalType);
        MarketPriceSnapshot snapshot = snapshotMapper.selectOne(
                new LambdaQueryWrapper<MarketPriceSnapshot>()
                        .eq(MarketPriceSnapshot::getMetalType, metalType)
                        .orderByDesc(MarketPriceSnapshot::getQuoteTime)
                        .last("LIMIT 1"));

        if (snapshot == null) {
            log.error("数据库也无 [{}] 行情数据", metalType);
            return null;
        }

        MarketPriceDTO dto = new MarketPriceDTO();
        dto.setMetalType(snapshot.getMetalType());
        dto.setMetalName(snapshot.getMetalName());
        dto.setLatestPrice(snapshot.getLatestPrice());
        dto.setOpenPrice(snapshot.getOpenPrice());
        dto.setHighPrice(snapshot.getHighPrice());
        dto.setLowPrice(snapshot.getLowPrice());
        dto.setPreClosePrice(snapshot.getPreClosePrice());
        dto.setChangePercent(snapshot.getChangePercent());
        dto.setUpdateTime(snapshot.getQuoteTime());

        saveToRedis(dto, false);
        return dto;
    }

    // ==================== 原有方法：selectTodayKline（Controller调用） ====================

    @Override
    public List<Kline1m> selectTodayKline(String metalType) {
        if (!StringUtils.hasText(metalType)) {
            return new ArrayList<>();
        }

        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);

        return kline1mMapper.selectTodayKline(metalType, today, tomorrow);
    }

    // ==================== 内部工具方法 ====================

    private void saveToRedis(MarketPriceDTO dto, boolean updateBase) {
        String redisKey = REDIS_KEY_PREFIX + dto.getMetalType();
        String json = JSON.toJSONString(dto);

        redisTemplate.opsForValue().set(redisKey, json, CACHE_EXPIRE, TimeUnit.MINUTES);

        if (updateBase) {
            String baseKey = BASE_KEY_PREFIX + dto.getMetalType();
            redisTemplate.opsForValue().set(baseKey, json, 24, TimeUnit.HOURS);
        }
    }

    private void saveSnapshot(String metalType, String metalName, BigDecimal latestPrice,
                              BigDecimal openAllDay, BigDecimal highAllDay, BigDecimal lowAllDay,
                              BigDecimal preClose, String changePercent, LocalTime quoteTime) {
        LambdaUpdateWrapper<MarketPriceSnapshot> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MarketPriceSnapshot::getMetalType, metalType)
                .set(MarketPriceSnapshot::getMetalName, metalName)
                .set(MarketPriceSnapshot::getLatestPrice, latestPrice)
                .set(MarketPriceSnapshot::getOpenPrice, openAllDay)
                .set(MarketPriceSnapshot::getHighPrice, highAllDay)
                .set(MarketPriceSnapshot::getLowPrice, lowAllDay)
                .set(MarketPriceSnapshot::getPreClosePrice, preClose)
                .set(MarketPriceSnapshot::getChangePercent, changePercent)
                .set(MarketPriceSnapshot::getQuoteTime, quoteTime);
        int affected = snapshotMapper.update(null, updateWrapper);

        if (affected == 0) {
            MarketPriceSnapshot snapshot = new MarketPriceSnapshot();
            snapshot.setMetalType(metalType);
            snapshot.setMetalName(metalName);
            snapshot.setLatestPrice(latestPrice);
            snapshot.setOpenPrice(openAllDay);
            snapshot.setHighPrice(highAllDay);
            snapshot.setLowPrice(lowAllDay);
            snapshot.setPreClosePrice(preClose);
            snapshot.setChangePercent(changePercent);
            snapshot.setQuoteTime(quoteTime);
            try {
                snapshotMapper.insert(snapshot);
            } catch (DuplicateKeyException e) {
                log.warn("行情快照并发插入冲突，metalType={}", metalType, e);
            }
        }
    }

    private BigDecimal getCurrentPriceFromRedis(String metalType) {
        String json = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + metalType);
        if (!StringUtils.hasText(json)) {
            MarketPriceDTO dto = getLatestPrice(metalType);
            if (dto != null && dto.getLatestPrice() != null) {
                return dto.getLatestPrice();
            }
            return null;
        }
        MarketPriceDTO dto = JSON.parseObject(json, MarketPriceDTO.class);
        return dto.getLatestPrice();
    }

    private MarketPriceDTO getCurrentPrice(String metal) {
        String json = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + metal);
        if (StringUtils.hasText(json)) {
            return JSON.parseObject(json, MarketPriceDTO.class);
        }
        return recoverFromDbOrFallback(metal);
    }

    private void processKline(String metalType, BigDecimal latestPrice,
                              BigDecimal dayOpenPrice, LocalDateTime now) {
        completeLastMinKline(metalType, latestPrice, now);
        createCurrentMinKline(metalType, latestPrice, dayOpenPrice, now);
    }

    private void completeLastMinKline(String metalType, BigDecimal closePrice, LocalDateTime now) {
        LocalDateTime lastMin = now.minusMinutes(1).withSecond(0).withNano(0);
        Kline1m kline = kline1mMapper.selectByMetalAndTime(metalType, lastMin);
        if (kline != null && kline.getClosePrice() == null) {
            kline.setClosePrice(closePrice);
            kline1mMapper.updateById(kline);
            lastClosePrices.put(metalType, closePrice);

            JSONObject msg = new JSONObject();
            msg.put("msgType", "KLINE");
            msg.put("metalType", metalType);
            msg.put("timestamp", lastMin.toString());
            msg.put("openPrice", kline.getOpenPrice());
            msg.put("highPrice", kline.getHighPrice());
            msg.put("lowPrice", kline.getLowPrice());
            msg.put("closePrice", closePrice);
            msg.put("volume", kline.getVolume());
            MarketWebSocketServer.sendRaw(metalType, msg.toJSONString());
            log.debug("[{}] 补全上一分钟K线[{}]收盘价:{}", metalType, lastMin, closePrice);
        }
    }

    private BigDecimal getLastClosePrice(String metalType, BigDecimal dayOpenPrice) {
        BigDecimal cached = lastClosePrices.get(metalType);
        if (cached != null) {
            return cached;
        }
        try {
            LocalDateTime lastMin = LocalDateTime.now().minusMinutes(1).withSecond(0).withNano(0);
            Kline1m kline = kline1mMapper.selectByMetalAndTime(metalType, lastMin);
            if (kline != null && kline.getClosePrice() != null) {
                lastClosePrices.put(metalType, kline.getClosePrice());
                return kline.getClosePrice();
            }
        } catch (Exception e) {
            log.warn("[{}] 查询上一分钟K线失败: {}", metalType, e.getMessage());
        }
        if (dayOpenPrice != null && dayOpenPrice.compareTo(BigDecimal.ZERO) > 0) {
            return dayOpenPrice;
        }
        return null;
    }

    private void createCurrentMinKline(String metalType, BigDecimal latestPrice,
                                       BigDecimal dayOpenPrice, LocalDateTime now) {
        LocalDateTime currMin = now.withSecond(0).withNano(0);
        Kline1m exist = kline1mMapper.selectByMetalAndTime(metalType, currMin);
        if (exist != null) {
            return;
        }

        BigDecimal basePrice = getLastClosePrice(metalType, dayOpenPrice);
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            basePrice = latestPrice;
        }

        SimulatedKline simulated = simulateMinuteKline(metalType, basePrice);
        simulated.close = latestPrice;
        if (simulated.high.compareTo(latestPrice) < 0) simulated.high = latestPrice;
        if (simulated.low.compareTo(latestPrice) > 0) simulated.low = latestPrice;

        Kline1m kline = new Kline1m();
        kline.setMetalType(metalType);
        kline.setTimestamp(currMin);
        kline.setOpenPrice(simulated.open);
        kline.setHighPrice(simulated.high);
        kline.setLowPrice(simulated.low);
        kline.setClosePrice(simulated.close);
        kline.setVolume(simulated.volume);
        kline1mMapper.insert(kline);

        lastClosePrices.put(metalType, simulated.close);

        log.debug("[{}] 新建分钟K线[{}] O:{} H:{} L:{} C:{} V:{}",
                metalType, currMin, simulated.open, simulated.high,
                simulated.low, simulated.close, simulated.volume);
    }

    private SimulatedKline simulateMinuteKline(String metalType, BigDecimal basePrice) {
        BigDecimal dailyVol = getDailyVolatility(metalType);
        double minVol = dailyVol.doubleValue() / Math.sqrt(MINUTES_PER_DAY);

        int steps = 60;
        double[] prices = new double[steps];
        prices[0] = basePrice.doubleValue();

        java.util.Random random = new java.util.Random(System.nanoTime());

        for (int i = 1; i < steps; i++) {
            double z = random.nextGaussian();
            double dt = 1.0 / steps;
            double change = minVol * Math.sqrt(dt) * z;
            prices[i] = prices[i - 1] * (1 + change);
            double meanReversion = 0.02 * (basePrice.doubleValue() - prices[i]) / basePrice.doubleValue();
            prices[i] = prices[i] * (1 + meanReversion);
        }

        double open = prices[0];
        double close = prices[steps - 1];
        double high = Arrays.stream(prices).max().getAsDouble();
        double low = Arrays.stream(prices).min().getAsDouble();

        high = Math.max(high, Math.max(open, close));
        low = Math.min(low, Math.min(open, close));

        double priceRange = (high - low) / open;
        long volume = (long) (100 + random.nextInt(900) * (1 + priceRange * 100));

        int scale = metalType.equals("AG") ? 3 : 2;

        SimulatedKline result = new SimulatedKline();
        result.open = BigDecimal.valueOf(open).setScale(scale, RoundingMode.HALF_UP);
        result.high = BigDecimal.valueOf(high).setScale(scale, RoundingMode.HALF_UP);
        result.low = BigDecimal.valueOf(low).setScale(scale, RoundingMode.HALF_UP);
        result.close = BigDecimal.valueOf(close).setScale(scale, RoundingMode.HALF_UP);
        result.volume = volume;

        return result;
    }

    private BigDecimal getDailyVolatility(String metalType) {
        switch (metalType) {
            case "AU": return AU_VOLATILITY;
            case "AG": return AG_VOLATILITY;
            case "PT": return PT_VOLATILITY;
            default: return AU_VOLATILITY;
        }
    }

    private static class SimulatedKline {
        BigDecimal open;
        BigDecimal high;
        BigDecimal low;
        BigDecimal close;
        long volume;
    }

    private BigDecimal getSafeBigDecimal(JSONObject item, String field) {
        String val = item.getString(field);
        if (!StringUtils.hasText(val) || "-".equals(val.trim())) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(val.trim());
        } catch (NumberFormatException e) {
            log.warn("行情字段{}非法数值[{}]，置0", field, val);
            return BigDecimal.ZERO;
        }
    }

    private String mapVariety(String variety) {
        if (!StringUtils.hasText(variety)) return null;
        if ("Au99.99".equals(variety)) return "AU";
        if ("Ag(T+D)".equals(variety)) return "AG";
        if ("Pt99.95".equals(variety)) return "PT";
        return null;
    }

    private LocalTime parseTime(String timeStr) {
        if (!StringUtils.hasText(timeStr) || timeStr.length() < 19) {
            return LocalTime.now();
        }
        try {
            return LocalTime.parse(timeStr.substring(11, 19));
        } catch (Exception e) {
            return LocalTime.now();
        }
    }
}