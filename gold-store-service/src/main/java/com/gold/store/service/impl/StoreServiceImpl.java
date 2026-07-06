package com.gold.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.common.code.ErrorCode;
import com.gold.common.exception.BusinessException;
import com.gold.store.domain.po.Store;
import com.gold.store.domain.vo.StoreDetailVO;
import com.gold.store.domain.vo.StoreListVO;
import com.gold.store.domain.vo.StoreNearbyVO;
import com.gold.store.mapper.StoreMapper;
import com.gold.store.service.StoreService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreMapper storeMapper;

    // 新增Redis注入
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;  // 引入分布式锁

    private static final String STORE_GEO_KEY = "store:geo";
    private static final String STORE_GEO_INIT_LOCK = "lock:store:geo:init";
    private static final long LOCK_WAIT_TIME = 10L;
    private static final long LOCK_LEASE_TIME = 300L;
    /**
     * 【已修复】更完美一点的初始化Redis Geo坐标的方法
     *
     * 原问题：每次启动全量删除重建，多实例并发可能冲突，无坐标合法性校验
     *
     * 修复内容：
     * - 2024-07-10: 改为增量更新，只处理变更的数据
     * - 2024-07-10: 添加 Redisson 分布式锁防止多实例并发
     * - 2024-07-10: 添加经纬度合法性校验
     * - 2024-07-10: 自动清理数据库已停业但Redis残留的坐标
     * - 2024-07-10: 初始化完成后增加数据一致性校验
     *
     * 项目启动自动执行，将所有营业门店坐标加载进Redis Geo
     * 解决重启后Redis坐标丢失问题
     */
    @PostConstruct
    public void initGeoData() {
        RLock lock = redissonClient.getLock(STORE_GEO_INIT_LOCK);
        boolean locked = false;

        try {
            // 尝试获取分布式锁，最多等待10秒，持有5分钟
            locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("未获取到 Redis Geo 初始化锁，跳过本次初始化");
                return;
            }

            // 1. 先获取 Redis 中已有的门店ID集合
            Set<String> existingIds = getExistingGeoMemberIds();

            // 2. 分页扫描数据库，增量处理
            long pageNum = 1;
            long pageSize = 500;
            long totalProcessed = 0;
            long totalAdded = 0;
            long totalUpdated = 0;
            long totalSkipped = 0;
            List<Store> records;

            do {
                Page<Store> page = new Page<>(pageNum, pageSize, false);
                LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<Store>()
                        .eq(Store::getStatus, 1)
                        .orderByAsc(Store::getId);  // 稳定排序，避免分页漂移
                page = storeMapper.selectPage(page, wrapper);
                records = page.getRecords();

                if (records.isEmpty()) break;

                // 批量处理当前页
                for (Store store : records) {
                    // 过滤非法坐标
                    if (!isValidCoordinate(store.getLongitude(), store.getLatitude())) {
                        log.warn("门店[{}]坐标非法，跳过: lng={}, lat={}",
                                store.getId(), store.getLongitude(), store.getLatitude());
                        totalSkipped++;
                        existingIds.remove(store.getId().toString());
                        continue;
                    }

                    String storeIdStr = store.getId().toString();
                    Point point = new Point(
                            store.getLongitude().doubleValue(),
                            store.getLatitude().doubleValue()
                    );

                    if (existingIds.contains(storeIdStr)) {
                        // 已存在则更新坐标（防止DB坐标被修改后Redis未同步）
                        redisTemplate.opsForGeo().remove(STORE_GEO_KEY, storeIdStr);
                        redisTemplate.opsForGeo().add(STORE_GEO_KEY, point, storeIdStr);
                        totalUpdated++;
                    } else {
                        // 新增
                        redisTemplate.opsForGeo().add(STORE_GEO_KEY, point, storeIdStr);
                        totalAdded++;
                    }
                    existingIds.remove(storeIdStr);  // 处理过的移除
                    totalProcessed++;
                }

                log.info("Redis Geo 分页处理第{}页，{}家门店", pageNum, records.size());
                pageNum++;
            } while (records.size() == pageSize);

            // 3. 清理已停业/被删除的门店（Redis中有但DB中不存在或已停业）
            if (!existingIds.isEmpty()) {
                String[] idsToRemove = existingIds.toArray(new String[0]);
                Long removed = redisTemplate.opsForGeo().remove(STORE_GEO_KEY, idsToRemove);
                log.info("清理无效门店坐标 {} 个", removed);
            }

            // 4. 数据一致性校验
            long redisCount = getGeoMemberCount();
            long dbCount = getActiveStoreCount();
            if (redisCount != dbCount) {
                log.error("Redis Geo 数据一致性校验失败: Redis={}, DB={}", redisCount, dbCount);
                // 根据业务要求：可抛异常阻断启动，或仅告警
                // throw new BusinessException(ErrorCode.SYSTEM_ERROR, "门店坐标初始化数据不一致");
            } else {
                log.info("Redis Geo 初始化完成，共处理 {} 家门店(新增{}，更新{}，跳过{})，数据一致性校验通过",
                        totalProcessed, totalAdded, totalUpdated, totalSkipped);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Redis Geo 初始化被中断", e);
        } catch (Exception e) {
            log.error("Redis Geo 初始化失败", e);
            // 建议：关键依赖失败时阻断启动，防止服务带伤运行
            throw new RuntimeException("门店坐标初始化失败，服务启动中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 获取 Redis 中已有的门店ID集合
     */
    private Set<String> getExistingGeoMemberIds() {
        // Redis Geo 没有直接获取所有 member 的命令，需用 zrange
        Set<String> members = redisTemplate.opsForZSet().range(STORE_GEO_KEY, 0, -1);
        return members != null ? new HashSet<>(members) : new HashSet<>();
    }

    /**
     * 获取 Redis Geo 成员数量
     */
    private long getGeoMemberCount() {
        Long count = redisTemplate.opsForZSet().zCard(STORE_GEO_KEY);
        return count != null ? count : 0;
    }

    /**
     * 获取数据库中营业门店总数
     */
    private long getActiveStoreCount() {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Store::getStatus, 1);
        return storeMapper.selectCount(wrapper);
    }

    /**
     * 校验经纬度是否合法
     */
    private boolean isValidCoordinate(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) return false;
        double lng = longitude.doubleValue();
        double lat = latitude.doubleValue();
        return lng >= -180 && lng <= 180 && lat >= -90 && lat <= 90;
    }


    @Override
    public Page<StoreListVO> getStoreList(Integer page, Integer size, String city,String keyword) {
        // 1. 构建查询条件：只查营业中的
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Store::getStatus, 1);

        // 城市筛选
        if (StringUtils.hasText(city)) {
            wrapper.eq(Store::getCity, city);
        }

        // 关键词搜索（门店名称或地址）← 新增
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Store::getStoreName, keyword)
                    .or()
                    .like(Store::getAddress, keyword));
        }
        // 2. 分页查询
        Page<Store> pageParam = new Page<>(page, size);
        Page<Store> storePage = storeMapper.selectPage(pageParam, wrapper);

        // 3. PO转VO
        List<StoreListVO> voList = storePage.getRecords().stream().map(store -> {
            StoreListVO vo = new StoreListVO();
            vo.setStoreId(store.getId());
            vo.setStoreName(store.getStoreName());
            // 地址拼接：省 + 市 + 区 + 详细地址
            vo.setAddress(store.getProvince() + store.getCity() + store.getDistrict() + store.getAddress());
            vo.setPhone(store.getPhone());
            vo.setBusinessHours(store.getBusinessHours());
            vo.setCoverImage(store.getCoverImage());
            vo.setRating(store.getRating());
            vo.setRatingCount(store.getRatingCount());
            return vo;
        }).collect(Collectors.toList());

        // 4. 组装返回
        Page<StoreListVO> resultPage = new Page<>();
        resultPage.setCurrent(storePage.getCurrent());
        resultPage.setSize(storePage.getSize());
        resultPage.setTotal(storePage.getTotal());
        resultPage.setPages(storePage.getPages());
        resultPage.setRecords(voList);

        return resultPage;
    }

    @Override
    public StoreDetailVO getStoreDetail(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,"门店不存在");
        }

        StoreDetailVO vo = new StoreDetailVO();
        vo.setStoreId(store.getId());
        vo.setStoreName(store.getStoreName());
        vo.setProvince(store.getProvince());
        vo.setCity(store.getCity());
        vo.setDistrict(store.getDistrict());
        vo.setAddress(store.getAddress());
        vo.setLongitude(store.getLongitude());
        vo.setLatitude(store.getLatitude());
        vo.setPhone(store.getPhone());
        vo.setBusinessHours(store.getBusinessHours());
        vo.setCoverImage(store.getCoverImage());
        vo.setRating(store.getRating());
        vo.setRatingCount(store.getRatingCount());

        return vo;
    }

    @Override
    public boolean isStoreValid(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        return store != null && store.getStatus() == 1;
    }

    @Override
    public List<StoreNearbyVO> getNearbyStores(Double lng, Double lat, Double radius) {
        // 参数校验：经纬度必传
        if (lng == null || lat == null) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "经度、纬度不能为空");
        }
        // 半径默认5公里，限制最大50公里防止超大范围查询
        if (radius == null || radius <= 0) {
            radius = 5.0;
        }
        if (radius > 50) {
            radius = 50.0;
        }

        // 1. Redis Geo查询周边点位
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = redisTemplate.opsForGeo().radius(
                STORE_GEO_KEY,
                new Circle(new Point(lng, lat), new Distance(radius, Metrics.KILOMETERS)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeDistance() // 返回两点距离
                        .sortAscending()   // 由近到远排序
                        .limit(20)         // 最多返回20家门店
        );

        // 无周边门店直接返回空集合
        if (geoResults == null || geoResults.getContent().isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 提取门店ID批量查库
        List<Long> storeIdList = geoResults.getContent().stream()
                .map(item -> Long.valueOf(item.getContent().getName()))
                .collect(Collectors.toList());
        List<Store> storeDbList = storeMapper.selectBatchIds(storeIdList);

        // 转为Map，过滤停业门店
        Map<Long, Store> storeMap = storeDbList.stream()
                .filter(store -> store.getStatus() == 1)
                .collect(Collectors.toMap(Store::getId, s -> s));

        // 3. 组装VO，保留Redis返回的由近到远顺序
        List<StoreNearbyVO> voResult = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> item : geoResults.getContent()) {
            Long storeId = Long.valueOf(item.getContent().getName());
            Store store = storeMap.get(storeId);
            // 过滤停业/不存在门店
            if (store == null) {
                continue;
            }
            StoreNearbyVO vo = new StoreNearbyVO();
            vo.setStoreId(store.getId());
            vo.setStoreName(store.getStoreName());
            vo.setAddress(store.getProvince() + store.getCity() + store.getDistrict() + store.getAddress());
            vo.setPhone(store.getPhone());
            vo.setBusinessHours(store.getBusinessHours());
            vo.setCoverImage(store.getCoverImage());
            vo.setRating(store.getRating());
            // 距离保留两位小数
            double km = item.getDistance().getValue();
            vo.setDistance(Math.round(km * 100) / 100.0);
            voResult.add(vo);
        }
        return voResult;
    }

    @Override
    public void addStore(Store store) {
        storeMapper.insert(store);
        // 新增同步Redis坐标
        redisTemplate.opsForGeo().add(
                STORE_GEO_KEY,
                new Point(store.getLongitude().doubleValue(), store.getLatitude().doubleValue()),
                store.getId().toString()
        );
    }

    @Override
    public void updateStore(Store store) {
        Store oldStore = storeMapper.selectById(store.getId());
        storeMapper.updateById(store);
        // 坐标发生变化才更新Redis
        if (!oldStore.getLongitude().equals(store.getLongitude()) || !oldStore.getLatitude().equals(store.getLatitude())) {
            redisTemplate.opsForGeo().remove(STORE_GEO_KEY, oldStore.getId().toString());
            redisTemplate.opsForGeo().add(
                    STORE_GEO_KEY,
                    new Point(store.getLongitude().doubleValue(), store.getLatitude().doubleValue()),
                    store.getId().toString()
            );
        }
    }


    @Override
    public List<StoreListVO> getRecommendStores(String city, Integer limit) {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        // 只查营业门店
        wrapper.eq(Store::getStatus, 1);
        // 城市筛选
        if (StringUtils.hasText(city)) {
            wrapper.eq(Store::getCity, city);
        }
        // 排序：评分优先，同评分看评价人数
        wrapper.orderByDesc(Store::getRating)
                .orderByDesc(Store::getRatingCount);
        // 限制返回条数，防全表查询
        wrapper.last("LIMIT " + limit);

        List<Store> storeList = storeMapper.selectList(wrapper);
        // PO转VO
        return storeList.stream().map(store -> {
            StoreListVO vo = new StoreListVO();
            vo.setStoreId(store.getId());
            vo.setStoreName(store.getStoreName());
            vo.setAddress(store.getProvince() + store.getCity() + store.getDistrict() + store.getAddress());
            vo.setPhone(store.getPhone());
            vo.setBusinessHours(store.getBusinessHours());
            vo.setCoverImage(store.getCoverImage());
            vo.setRating(store.getRating());
            vo.setRatingCount(store.getRatingCount());
            return vo;
        }).collect(Collectors.toList());
    }
}