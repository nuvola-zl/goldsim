package com.gold.api.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.api.domain.dto.DeductActivityStockRequest;
import com.gold.api.domain.dto.DeductStockRequest;
import com.gold.api.domain.dto.SpecInfoDTO;
import com.gold.api.fallback.ProductFeignApiFallbackFactory;
import com.gold.common.domain.dto.GoodsPageDTO;
import com.gold.common.result.Result;

import com.gold.mallApi.domain.dto.*;
import com.gold.mallApi.domain.vo.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

/**
 * 商品服务统一远程调用Feign客户端
 * 服务注册名：gold-product-service
 * contextId：区分多Feign，防止Bean冲突
 * 整合原有MallFeignApi全部库存、规格接口，统一对外提供商品所有能力
 * 分三类接口：后台管理、商城前台展示、业务服务库存操作（订单/模拟盘调用）
 */
@FeignClient(name = "gold-product-service",
        contextId = "productFeignApi",
        fallbackFactory = ProductFeignApiFallbackFactory.class)
public interface ProductFeignApi {

    // ====================================== 一、业务服务专用接口（订单/模拟盘调用，入参出参仅gold-api极简DTO） ======================================
    /**
     * 根据规格ID查询商品规格基础信息（订单下单、模拟盘计价专用）
     * @param specId 商品规格主键
     * @return 极简规格DTO，仅包含金价、重量、库存、活动信息等业务计算必需字段
     */
    @GetMapping("/product/biz-stock/spec/{specId}")
    Result<SpecInfoDTO> getSpecInfo(@PathVariable("specId") Long specId);

    /**
     * 下单冻结商品基础库存
     * @param request 库存冻结请求体（specId+购买数量）
     */
    @PostMapping("/product/biz-stock/deduct-freeze")
    Result<Void> freezeStock(@RequestBody DeductStockRequest request);

    /**
     * 订单超时/取消，解冻基础商品库存
     * @param request 库存解冻请求体
     */
    @PostMapping("/product/biz-stock/unfreeze")
    Result<Void> unFreezeStock(@RequestBody DeductStockRequest request);

    /**
     * 支付成功，永久扣减商品基础库存
     * @param request 库存扣减请求体
     */
    @PostMapping("/product/biz-stock/deduct")
    Result<Void> deductStock(@RequestBody DeductStockRequest request);

    /**
     * 活动商品冻结库存（抢购下单）
     * @param request 活动库存扣减请求
     */
    @PostMapping("/product/biz-stock/activity/deduct-freeze")
    Result<Void> deductActivityStock(@RequestBody DeductActivityStockRequest request);

    /**
     * 活动订单取消/失败，解冻活动库存
     * @param request 活动库存解冻请求
     */
    @PostMapping("/product/biz-stock/activity/unfreeze")
    Result<Void> unFreezeActivityStock(@RequestBody DeductActivityStockRequest request);

    // ====================================== 二、Admin后台管理接口（仅admin服务调用，依赖productapi下管理端DTO/VO） ======================================
    /**
     * 分页查询商品列表（后台管理）
     * @param dto 分页查询条件
     */
    @GetMapping("/product/admin/goods/list")
    Result<Page<GoodsAdminListVO>> pageGoods(@SpringQueryMap GoodsPageDTO dto);

    /**
     * 根据商品ID查询商品完整后台详情
     * @param goodsId 商品主键
     */
    @GetMapping("/product/admin/goods/{goodsId}")
    Result<GoodsAdminDetailVO> getGoodsDetail(@PathVariable("goodsId") Long goodsId);

    /**
     * 根据规格ID查询规格后台详情（编辑回显）
     * @param specId 规格主键
     */
    @GetMapping("/product/admin/goods/spec/{specId}")
    Result<SpecAdminVO> getSpecById(@PathVariable("specId") Long specId);

    /**
     * 新增商品主体
     * @param dto 商品新增入参
     * @return 新增商品ID
     */
    @PostMapping("/product/admin/goods")
    Result<Long> addGoods(@RequestBody GoodsAddDTO dto);

    /**
     * 修改商品主体信息
     * @param goodsId 待修改商品ID
     * @param dto 商品修改入参
     */
    @PutMapping("/product/admin/goods/{goodsId}")
    Result<Void> updateGoods(@PathVariable("goodsId") Long goodsId, @RequestBody GoodsUpdateDTO dto);

    /**
     * 删除商品（级联删除规格）
     * @param goodsId 商品ID
     */
    @DeleteMapping("/product/admin/goods/{goodsId}")
    Result<Void> deleteGoods(@PathVariable("goodsId") Long goodsId);

    /**
     * 商品下新增规格
     * @param goodsId 归属商品ID
     * @param dto 规格新增入参
     * @return 新增规格ID
     */
    @PostMapping("/product/admin/goods/{goodsId}/spec")
    Result<Long> addSpec(@PathVariable("goodsId") Long goodsId, @RequestBody SpecAddDTO dto);

    /**
     * 修改规格信息
     * @param specId 规格ID
     * @param dto 规格修改入参
     */
    @PutMapping("/product/admin/goods/spec/{specId}")
    Result<Void> updateSpec(@PathVariable("specId") Long specId, @RequestBody SpecUpdateDTO dto);

    /**
     * 删除商品规格
     * @param specId 规格ID
     */
    @DeleteMapping("/product/admin/goods/spec/{specId}")
    Result<Void> deleteSpec(@PathVariable("specId") Long specId);

    /**
     * 直接修改规格库存（后台手动调整）
     * @param specId 规格ID
     * @param stock 新库存数量
     */
    @PutMapping("/product/admin/goods/spec/{specId}/stock")
    Result<Void> updateStock(@PathVariable("specId") Long specId, @RequestParam("stock") Integer stock);

    /**
     * 修改规格上下架状态
     * @param specId 规格ID
     * @param saleStatus 销售状态 0下架 1上架
     */
    @PutMapping("/product/admin/goods/spec/{specId}/status")
    Result<Void> updateSaleStatus(@PathVariable("specId") Long specId, @RequestParam("saleStatus") Integer saleStatus);

    // 后台活动管理接口
    @GetMapping("/product/admin/activity/list")
    Result<Page<ActivityAdminListVO>> pageActivity(
            @RequestParam(value = "activityName", required = false) String activityName,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size);

    @GetMapping("/product/admin/activity/{activityId}")
    Result<ActivityAdminDetailVO> getActivityDetail(@PathVariable("activityId") Long activityId);

    @PostMapping("/product/admin/activity")
    Result<Long> addActivity(@RequestBody ActivityAddDTO dto);

    @PutMapping("/product/admin/activity/{activityId}")
    Result<Void> updateActivity(@PathVariable("activityId") Long activityId, @RequestBody ActivityUpdateDTO dto);

    @PutMapping("/product/admin/activity/{activityId}/cancel")
    Result<Void> cancelActivity(@PathVariable("activityId") Long activityId);

    @DeleteMapping("/product/admin/activity/{activityId}")
    Result<Void> deleteActivity(@PathVariable("activityId") Long activityId);

    @PostMapping("/product/admin/activity/{activityId}/goods")
    Result<Void> addActivityGoods(@PathVariable("activityId") Long activityId, @RequestBody ActivityGoodsUpdateDTO dto);

    @PutMapping("/product/admin/activity/goods/{activityGoodsId}")
    Result<Void> updateActivityGoods(@PathVariable("activityGoodsId") Long activityGoodsId, @RequestBody ActivityGoodsUpdateDTO dto);

    @DeleteMapping("/product/admin/activity/goods/{activityGoodsId}")
    Result<Void> deleteActivityGoods(@PathVariable("activityGoodsId") Long activityGoodsId);

    // ====================================== 三、商城前台展示接口（仅mall服务调用，依赖productapi商城VO） ======================================
    /**
     * 商城分页商品列表
     */
    @GetMapping("/product/mall/goods/list")
    Result<Page<GoodsListVO>> getGoodsList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "metalType", required = false) String metalType);

    /**
     * 商城商品详情页
     */
    @GetMapping("/product/mall/goods/{id}")
    Result<GoodsDetailVO> getMallGoodsDetail(@PathVariable("id") Long id);

    /**
     * 商品购买预览信息（价格、库存、活动）
     */
    @GetMapping("/product/mall/goods/buy-info/{specId}")
    Result<BuyInfoVO> getBuyInfo(@PathVariable("specId") Long specId,
                                 @RequestParam(value = "quantity", defaultValue = "1") Integer quantity);

    /**
     * 查询进行中活动列表
     */
    @GetMapping("/product/mall/activity/ongoing")
    Result<Page<ActivityShowVO>> getOngoingActivities(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size);

    /**
     * 查询即将开始活动列表
     */
    @GetMapping("/product/mall/activity/upcoming")
    Result<Page<ActivityShowVO>> getUpcomingActivities(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size);


    // ========== 新增：TCC Try 接口 ==========
    /**
     * TCC Try：冻结库存（Seata 全局事务分支）
     *
     * 调用方（订单服务）有 @GlobalTransactional 时
     * Seata 自动在请求头里加 XID
     * 商品服务收到后，自动注册为 TCC 分支事务
     */
    @PostMapping("/product/tcc/freeze")
    Result<Void> tccFreezeStock(@RequestBody DeductStockRequest request);
}