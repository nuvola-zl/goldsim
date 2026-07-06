package com.gold.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.api.domain.dto.MarketPriceDTO;
import com.gold.api.domain.dto.StoreSimpleDTO;
import com.gold.api.feign.MarketFeignApi;
import com.gold.api.feign.StoreFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.domain.dto.GoodsPageDTO;
import com.gold.common.exception.BusinessException;
import com.gold.common.result.PageResult;
import com.gold.common.result.Result;
import com.gold.mallApi.domain.dto.GoodsAddDTO;
import com.gold.mallApi.domain.dto.GoodsUpdateDTO;
import com.gold.mallApi.domain.dto.SpecAddDTO;
import com.gold.mallApi.domain.dto.SpecUpdateDTO;
import com.gold.mallApi.domain.vo.*;
import com.gold.product.entity.ActivityGoodsInfo;
import com.gold.product.entity.MallActivityGoods;
import com.gold.product.entity.MallGoods;
import com.gold.product.entity.MallGoodsSpec;
import com.gold.product.mapper.*;
import com.gold.product.service.ActivityService;
import com.gold.product.service.GoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {

    private final MallGoodsMapper goodsMapper;
    private final MallGoodsSpecMapper specMapper;
    private final GoodsAdminMapper goodsAdminMapper;
    private final GoodsShowMapper goodsShowMapper;
    private final MallActivityGoodsMapper activityGoodsMapper;
    private final ActivityGoodsMapper activityGoodsQueryMapper;
    private final MarketFeignApi marketFeignApi;
    private final StoreFeignApi storeFeignApi;
    private final StringRedisTemplate stringRedisTemplate;
    private final ActivityService activityService;

    // ==================== Admin: 查询 ====================

    @Override
    public Page<GoodsAdminListVO> pageGoods(GoodsPageDTO dto) {
        Page<GoodsAdminListVO> page = new Page<>(dto.getPage(), dto.getSize());
        return goodsAdminMapper.selectGoodsAdminPage(page, dto);
    }

    @Override
    public GoodsAdminDetailVO getGoodsDetail(Long goodsId) {
        MallGoods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
        }
        List<SpecAdminVO> specList = goodsAdminMapper.selectSpecsByGoodsId(goodsId);
        GoodsAdminDetailVO vo = new GoodsAdminDetailVO();
        BeanUtils.copyProperties(goods, vo);
        vo.setGoodsId(goods.getId());
        vo.setSpecList(specList);
        return vo;
    }

    @Override
    public SpecAdminVO getSpecById(Long specId) {
        MallGoodsSpec spec = specMapper.selectById(specId);
        if (spec == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "规格不存在");
        }
        SpecAdminVO vo = new SpecAdminVO();
        BeanUtils.copyProperties(spec, vo);
        vo.setSpecId(spec.getId());
        return vo;
    }

    // ==================== Admin: 商品主信息 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addGoods(GoodsAddDTO dto) {
        MallGoods goods = new MallGoods();
        goods.setGoodsName(dto.getGoodsName());
        goods.setCategory(dto.getCategory());
        goods.setMetalType(dto.getMetalType());
        goods.setStoreId(dto.getStoreId());
        goods.setSort(dto.getSort() != null ? dto.getSort() : 0);
        goods.setCreateTime(LocalDateTime.now());
        goods.setUpdateTime(LocalDateTime.now());
        goodsMapper.insert(goods);

        Long goodsId = goods.getId();

        if (!CollectionUtils.isEmpty(dto.getSpecList())) {
            BigDecimal referencePrice = getReferencePrice(dto.getMetalType());
            List<MallGoodsSpec> specList = dto.getSpecList().stream()
                    .map(specDto -> buildSpec(goodsId, specDto, referencePrice))
                    .toList();
            for (MallGoodsSpec spec : specList) {
                specMapper.insert(spec);
            }
        }

        log.info("添加商品成功，goodsId={}", goodsId);
        return goodsId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGoods(GoodsUpdateDTO dto) {
        MallGoods goods = goodsMapper.selectById(dto.getId());
        if (goods == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
        }

        String oldMetalType = goods.getMetalType();
        goods.setGoodsName(dto.getGoodsName());
        goods.setCategory(dto.getCategory());
        goods.setMetalType(dto.getMetalType());
        goods.setStoreId(dto.getStoreId());
        goods.setSort(dto.getSort());
        goods.setUpdateTime(LocalDateTime.now());
        goodsMapper.updateById(goods);

        if (!oldMetalType.equals(dto.getMetalType())) {
            BigDecimal newPrice = getReferencePrice(dto.getMetalType());
            List<MallGoodsSpec> specs = specMapper.selectList(
                    new LambdaQueryWrapper<MallGoodsSpec>()
                            .eq(MallGoodsSpec::getGoodsId, dto.getId())
            );
            for (MallGoodsSpec spec : specs) {
                spec.setPricePerGram(newPrice);
                spec.setTotalPrice(calculateTotalPrice(spec.getWeight(), newPrice, spec.getCraftFee()));
                spec.setUpdateTime(LocalDateTime.now());
                specMapper.updateById(spec);
            }
            log.info("商品 metalType 变更，已同步更新所有规格金价，goodsId={}", dto.getId());
        }

        log.info("修改商品成功，goodsId={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGoods(Long goodsId) {
        MallGoods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
        }
        specMapper.delete(
                new LambdaQueryWrapper<MallGoodsSpec>()
                        .eq(MallGoodsSpec::getGoodsId, goodsId)
        );
        goodsMapper.deleteById(goodsId);
        log.info("删除商品成功，goodsId={}", goodsId);
    }

    // ==================== Admin: 规格操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addSpec(Long goodsId, SpecAddDTO dto) {
        MallGoods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
        }

        BigDecimal referencePrice = getReferencePrice(goods.getMetalType());
        MallGoodsSpec spec = buildSpec(goodsId, dto, referencePrice);
        specMapper.insert(spec);

        log.info("添加规格成功，specId={}, goodsId={}", spec.getId(), goodsId);
        return spec.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpec(SpecUpdateDTO dto) {
        MallGoodsSpec spec = specMapper.selectById(dto.getId());
        if (spec == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "规格不存在");
        }

        if (dto.getWeight() != null) spec.setWeight(dto.getWeight());
        if (dto.getCraftFee() != null) spec.setCraftFee(dto.getCraftFee());
        if (dto.getCoverImage() != null) spec.setCoverImage(dto.getCoverImage());
        if (dto.getDetailImages() != null) spec.setDetailImages(dto.getDetailImages());
        if (dto.getDescription() != null) spec.setDescription(dto.getDescription());

        if (dto.getWeight() != null || dto.getCraftFee() != null) {
            BigDecimal weight = spec.getWeight();
            BigDecimal craftFee = spec.getCraftFee();
            BigDecimal referencePrice = spec.getPricePerGram();
            if (referencePrice == null || referencePrice.compareTo(BigDecimal.ZERO) <= 0) {
                MallGoods goods = goodsMapper.selectById(spec.getGoodsId());
                referencePrice = getReferencePrice(goods.getMetalType());
                spec.setPricePerGram(referencePrice);
            }
            spec.setTotalPrice(calculateTotalPrice(weight, referencePrice, craftFee));
        }

        spec.setUpdateTime(LocalDateTime.now());
        specMapper.updateById(spec);
        log.info("修改规格成功，specId={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSpec(Long specId) {
        MallGoodsSpec spec = specMapper.selectById(specId);
        if (spec == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "规格不存在");
        }

        Long count = activityGoodsMapper.selectCount(
                new LambdaQueryWrapper<MallActivityGoods>()
                        .eq(MallActivityGoods::getSpecId, specId)
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "该规格已关联活动，请先移除活动关联后再删除");
        }

        specMapper.deleteById(specId);
        log.info("删除规格成功，specId={}", specId);
    }

    @Override
    public void updateStock(Long specId, Integer stock) {
        if (stock < 0) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "库存不能为负数");
        }
        int rows = goodsAdminMapper.updateStockDirect(specId, stock);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "规格不存在，库存修改失败");
        }
        log.info("修改库存成功，specId={}, stock={}", specId, stock);
    }

    @Override
    public void updateSaleStatus(Long specId, Integer saleStatus) {
        if (saleStatus != 0 && saleStatus != 1) {
            throw new RuntimeException("售卖状态非法");
        }
        int rows = goodsAdminMapper.updateSaleStatus(specId, saleStatus);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "规格不存在，状态修改失败");
        }
        log.info("修改售卖状态成功，specId={}, saleStatus={}", specId, saleStatus);
    }

    // ==================== Mall: 商品列表 ====================

    // TODO: 后续加Redis缓存
    // 1. 列表页缓存5分钟
    // 2. 详情页缓存10分钟
    // 3. 后台修改商品时删除缓存
    @Override
    public Page<GoodsListVO> getGoodsList(Integer page, Integer size, String category, String metalType) {
        Page<GoodsListVO> pageParam = new Page<>(page, size);
        goodsShowMapper.selectGoodsBaseList(pageParam, category, metalType);

        List<GoodsListVO> records = pageParam.getRecords();
        if (records == null || records.isEmpty()) {
            return pageParam;
        }

        // 1. 补充默认规格信息（原有逻辑）
        List<Long> goodsIds = records.stream()
                .map(GoodsListVO::getGoodsId)
                .distinct()
                .collect(Collectors.toList());

        List<GoodsListVO> specList = goodsShowMapper.selectDefaultSpecsByGoodsIds(goodsIds);
        Map<Long, GoodsListVO> specMap = specList.stream()
                .collect(Collectors.toMap(GoodsListVO::getGoodsId, v -> v, (a, b) -> a));

        records.forEach(goods -> {
            GoodsListVO spec = specMap.get(goods.getGoodsId());
            if (spec != null) {
                goods.setSpecId(spec.getSpecId());
                goods.setWeight(spec.getWeight());
                goods.setTotalPrice(spec.getTotalPrice());
                goods.setCoverImage(spec.getCoverImage());
            }
        });

        // 2.查询进行中的活动（复用详情页逻辑）
        List<Long> specIds = records.stream()
                .map(GoodsListVO::getSpecId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!specIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            List<ActivityGoodsInfo> ongoingActivities =
                    activityGoodsQueryMapper.selectOngoingBySpecIds(specIds, now);

            Map<Long, ActivityGoodsInfo> activityMap = ongoingActivities.stream()
                    .collect(Collectors.toMap(ActivityGoodsInfo::getSpecId, v -> v, (a, b) -> a));

            records.forEach(goods -> {
                ActivityGoodsInfo act = activityMap.get(goods.getSpecId());
                if (act != null) {
                    goods.setActivityId(act.getActivityId());  // 或 goods.setHasActivity(true);
                }
            });
        }

        return pageParam;
    }
    // ==================== Mall: 商品详情 ====================

    @Override
    public GoodsDetailVO getMallGoodsDetail(Long goodsId) {
        MallGoods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
        }

        List<MallGoodsSpec> specList = specMapper.selectList(
                new LambdaQueryWrapper<MallGoodsSpec>()
                        .eq(MallGoodsSpec::getGoodsId, goodsId)
        );

        List<Long> specIds = specList.stream()
                .map(MallGoodsSpec::getId)
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();

        List<ActivityGoodsInfo> ongoingActivities = activityGoodsQueryMapper.selectOngoingBySpecIds(specIds, now);
        Map<Long, ActivityGoodsInfo> ongoingMap = ongoingActivities.stream()
                .collect(Collectors.toMap(ActivityGoodsInfo::getSpecId, v -> v, (a, b) -> a));

        List<ActivityGoodsInfo> upcomingActivities = activityGoodsQueryMapper.selectUpcomingBySpecIds(specIds, now);

        GoodsDetailVO vo = new GoodsDetailVO();
        vo.setGoodsId(goods.getId());
        vo.setGoodsName(goods.getGoodsName());
        vo.setCategory(goods.getCategory());
        vo.setMetalType(goods.getMetalType());
        vo.setStoreInfo(fetchStoreInfo(goods.getStoreId()));

        List<GoodsDetailVO.SpecDetailVO> specVOList = specList.stream()
                .map(spec -> {
                    GoodsDetailVO.SpecDetailVO specVO = GoodsDetailVO.SpecDetailVO.builder()
                            .specId(spec.getId())
                            .weight(spec.getWeight())
                            .craftFee(spec.getCraftFee())
                            .pricePerGram(spec.getPricePerGram())
                            .totalPrice(spec.getTotalPrice())
                            .stock(spec.getStock())
                            .frozenStock(spec.getFrozenStock())
                            .coverImage(spec.getCoverImage())
                            .detailImages(spec.getDetailImages())
                            .description(spec.getDescription())
                            .saleStatus(spec.getSaleStatus())
                            .build();

                    ActivityGoodsInfo ongoing = ongoingMap.get(spec.getId());
                    if (ongoing != null) {
                        specVO.setActivityInfo(convertToActivityInfo(ongoing));
                    }
                    return specVO;
                })
                .collect(Collectors.toList());

        vo.setSpecList(specVOList);

        if (upcomingActivities != null && !upcomingActivities.isEmpty()) {
            vo.setUpcomingActivities(upcomingActivities.stream()
                    .map(this::convertToActivityInfo)
                    .collect(Collectors.toList()));
        }

        return vo;
    }

    // ==================== Mall: 购买信息 ====================

    @Override
    public BuyInfoVO getBuyInfo(Long specId, Integer quantity) {
        MallGoodsSpec spec = specMapper.selectById(specId);
        if (spec == null || spec.getSaleStatus() == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品规格不存在或已停售");
        }

        MallGoods goods = goodsMapper.selectById(spec.getGoodsId());
        if (goods == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在");
        }

        ActivityGoodsInfo activity = activityGoodsQueryMapper.selectOngoingBySpecId(specId, LocalDateTime.now());

        BigDecimal unitPrice;
        Integer priceType;
        BuyInfoVO.ActivityInfo activityVO = null;

        if (activity != null) {
            unitPrice = activity.getActivityPrice();
            priceType = 1;

            activityVO = new BuyInfoVO.ActivityInfo();
            activityVO.setActivityId(activity.getActivityId());
            activityVO.setActivityName(activity.getActivityName());
            activityVO.setActivityPrice(activity.getActivityPrice());
            activityVO.setOriginalPrice(activity.getOriginalPrice());
            activityVO.setLimitPerUser(activity.getLimitPerUser());
            activityVO.setStartTime(activity.getStartTime());
            activityVO.setEndTime(activity.getEndTime());
            activityVO.setStatus(1);
        } else {
            BigDecimal latestPrice = getReferencePrice(goods.getMetalType());
            unitPrice = calculateTotalPrice(spec.getWeight(), latestPrice, spec.getCraftFee());
            priceType = 0;
        }

        BigDecimal totalPrice = unitPrice.multiply(new BigDecimal(quantity));
        int availableStock = spec.getStock() - spec.getFrozenStock();
        if (quantity > availableStock) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存不足，剩余可售：" + availableStock);
        }

        BuyInfoVO vo = new BuyInfoVO();
        vo.setGoodsName(goods.getGoodsName());
        vo.setCategory(goods.getCategory());
        vo.setWeight(spec.getWeight());
        vo.setCoverImage(spec.getCoverImage());
        vo.setQuantity(quantity);
        vo.setUnitPrice(unitPrice);
        vo.setTotalPrice(totalPrice);
        vo.setPriceType(priceType);
        vo.setAvailableStock(availableStock);
        vo.setActivityInfo(activityVO);

        return vo;
    }

    // ==================== 私有方法 ====================

    private MallGoodsSpec buildSpec(Long goodsId, SpecAddDTO dto, BigDecimal referencePrice) {
        MallGoodsSpec spec = new MallGoodsSpec();
        spec.setGoodsId(goodsId);
        spec.setWeight(dto.getWeight());
        spec.setCraftFee(dto.getCraftFee());
        spec.setPricePerGram(referencePrice);
        spec.setTotalPrice(calculateTotalPrice(dto.getWeight(), referencePrice, dto.getCraftFee()));
        spec.setStock(dto.getStock());
        spec.setFrozenStock(0);
        spec.setSaleStatus(1);
        spec.setCoverImage(dto.getCoverImage());
        spec.setDetailImages(dto.getDetailImages());
        spec.setDescription(dto.getDescription());
        spec.setCreateTime(LocalDateTime.now());
        spec.setUpdateTime(LocalDateTime.now());
        return spec;
    }

    private BigDecimal getReferencePrice(String metalType) {
        String redisKey = "market:price:" + metalType;
        String cached = stringRedisTemplate.opsForValue().get(redisKey);

        if (StringUtils.hasText(cached)) {
            try {
                MarketPriceDTO dto = com.alibaba.fastjson.JSON.parseObject(cached, MarketPriceDTO.class);
                if (dto.getLatestPrice() != null && dto.getLatestPrice().compareTo(BigDecimal.ZERO) > 0) {
                    log.debug("[{}] 命中Redis参考金价: {}", metalType, dto.getLatestPrice());
                    return dto.getLatestPrice();
                }
            } catch (Exception e) {
                log.warn("解析Redis金价失败: {}", e.getMessage());
            }
        }

        log.info("[{}] Redis无缓存，远程调用行情服务", metalType);
        Result<MarketPriceDTO> result = marketFeignApi.getLatestPrice(metalType);
        if (result == null || result.getData() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "获取行情价失败，请稍后重试");
        }

        BigDecimal latestPrice = result.getData().getLatestPrice();
        if (latestPrice == null || latestPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.MARKET_PRICE_FETCH_FAILED, "行情价格异常");
        }

        stringRedisTemplate.opsForValue().set(redisKey,
                com.alibaba.fastjson.JSON.toJSONString(result.getData()), 35, TimeUnit.MINUTES);
        return latestPrice;
    }

    private BigDecimal calculateTotalPrice(BigDecimal weight, BigDecimal pricePerGram, BigDecimal craftFee) {
        if (weight == null || pricePerGram == null || craftFee == null) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "重量、金价、工费不能为空");
        }
        BigDecimal goldValue = weight.multiply(pricePerGram);
        BigDecimal craftFeeTotal = weight.multiply(craftFee);
        return goldValue.add(craftFeeTotal).setScale(2, RoundingMode.HALF_UP);
    }

    private GoodsDetailVO.StoreInfo fetchStoreInfo(Long storeId) {
        if (storeId == null) return null;
        try {
            Result<StoreSimpleDTO> result = storeFeignApi.getStoreSimple(storeId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                StoreSimpleDTO dto = result.getData();
                return GoodsDetailVO.StoreInfo.builder()
                        .storeId(dto.getStoreId())
                        .storeName(dto.getStoreName())
                        .storeAddress(dto.getAddress())
                        .phone(dto.getPhone())
                        .businessHours(dto.getBusinessHours())
                        .rating(dto.getRating())
                        .build();
            }
        } catch (Exception e) {
            log.warn("查询商品绑定门店信息失败，storeId={}", storeId, e);
        }
        return null;
    }

    private GoodsDetailVO.ActivityInfo convertToActivityInfo(ActivityGoodsInfo activity) {
        if (activity == null) return null;
        return GoodsDetailVO.ActivityInfo.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .specId(activity.getSpecId())
                .originalPrice(activity.getOriginalPrice())
                .activityPrice(activity.getActivityPrice())
                .activityStock(activity.getActivityStock() - activity.getSoldCount())
                .soldCount(activity.getSoldCount())
                .limitPerUser(activity.getLimitPerUser())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .status(calcActivityStatus(activity.getStartTime(), activity.getEndTime()))
                .build();
    }

    private Integer calcActivityStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) return 0;
        if (now.isAfter(endTime)) return 2;
        return 1;
    }

    // ==================== 商品搜索 ====================

    // ==================== 商品搜索 ====================

    @Override
    public PageResult<GoodsListVO> searchGoods(String keyword, String category, String metalType, Integer page, Integer size) {
        Page<GoodsListVO> pageParam = new Page<>(page, size);
        goodsShowMapper.selectGoodsBaseList(pageParam, category, metalType);

        List<GoodsListVO> records = pageParam.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResult.build(Collections.emptyList(), pageParam.getTotal(), page, size);
        }

        // 关键词过滤
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            records = records.stream()
                    .filter(g -> g.getGoodsName() != null && g.getGoodsName().toLowerCase().contains(kw))
                    .collect(Collectors.toList());
        }

        //补充默认规格信息
        List<Long> goodsIds = records.stream()
                .map(GoodsListVO::getGoodsId)
                .distinct()
                .collect(Collectors.toList());

        if (!goodsIds.isEmpty()) {
            List<GoodsListVO> specList = goodsShowMapper.selectDefaultSpecsByGoodsIds(goodsIds);
            Map<Long, GoodsListVO> specMap = specList.stream()
                    .collect(Collectors.toMap(GoodsListVO::getGoodsId, v -> v, (a, b) -> a));

            records.forEach(goods -> {
                GoodsListVO spec = specMap.get(goods.getGoodsId());
                if (spec != null) {
                    goods.setSpecId(spec.getSpecId());
                    goods.setWeight(spec.getWeight());
                    goods.setTotalPrice(spec.getTotalPrice());
                    goods.setCoverImage(spec.getCoverImage());
                }
            });
        }

        // 查进行中的活动
        List<Long> specIds = records.stream()
                .map(GoodsListVO::getSpecId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!specIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            List<ActivityGoodsInfo> ongoingActivities =
                    activityGoodsQueryMapper.selectOngoingBySpecIds(specIds, now);

            Map<Long, ActivityGoodsInfo> activityMap = ongoingActivities.stream()
                    .collect(Collectors.toMap(ActivityGoodsInfo::getSpecId, v -> v, (a, b) -> a));

            records.forEach(goods -> {
                ActivityGoodsInfo act = activityMap.get(goods.getSpecId());
                if (act != null) {
                    goods.setActivityId(act.getActivityId());
                }
            });
        }

        return PageResult.build(records, pageParam.getTotal(), page, size);
    }

// ==================== 商品分类 ====================

    @Override
    public List<String> getCategories() {
        // 从数据库查 distinct category，或者写死
        // 看你 MallGoods.category 的取值范围
        return Arrays.asList("投资金条", "首饰", "金币", "摆件");
    }

// ==================== 首页聚合 ====================

    @Override
    public HomePageVO getHomePage() {
        // 1. 推荐商品（按排序权重取前 6 个）
        Page<GoodsListVO> goodsPage = getGoodsList(1, 6, null, null);
        List<GoodsListVO> recommendGoods = goodsPage.getRecords();

        // 2. 热门活动（进行中，取前 3 个）
        Page<ActivityShowVO> activityPage = activityService.getOngoingActivities(1, 3);
        List<ActivityShowVO> hotActivities = activityPage.getRecords();

        // 3. 最新金价（AU/AG/PT）
        List<HomePageVO.MetalPriceVO> latestPrices = Arrays.asList(
                fetchMetalPrice("AU", "黄金"),
                fetchMetalPrice("AG", "白银"),
                fetchMetalPrice("PT", "铂金")
        );

        return HomePageVO.builder()
                .recommendGoods(recommendGoods)
                .hotActivities(hotActivities)
                .latestPrices(latestPrices)
                .build();
    }

    private HomePageVO.MetalPriceVO fetchMetalPrice(String metalType, String metalName) {
        try {
            Result<MarketPriceDTO> result = marketFeignApi.getLatestPrice(metalType);
            if (result != null && result.isSuccess() && result.getData() != null) {
                MarketPriceDTO dto = result.getData();
                return HomePageVO.MetalPriceVO.builder()
                        .metalType(metalType)
                        .metalName(metalName)
                        .latestPrice(dto.getLatestPrice())
                        .changePercent(dto.getChangePercent())
                        .build();
            }
        } catch (Exception e) {
            log.warn("获取{}行情失败", metalName, e);
        }

        // 兜底：返回 0
        return HomePageVO.MetalPriceVO.builder()
                .metalType(metalType)
                .metalName(metalName)
                .latestPrice(BigDecimal.ZERO)
                .changePercent("0%")
                .build();
    }
}