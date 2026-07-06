package com.gold.api.fallback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.api.domain.dto.DeductActivityStockRequest;
import com.gold.api.domain.dto.DeductStockRequest;
import com.gold.api.domain.dto.SpecInfoDTO;
import com.gold.api.feign.ProductFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.result.Result;
import com.gold.mallApi.domain.dto.*;
import com.gold.mallApi.domain.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 商品服务Feign远程调用降级工厂
 * 当gold-product-service服务不可用、超时、熔断时触发本地降级逻辑
 * 使用FallbackFactory可捕获原始异常，便于日志排查问题
 */
@Slf4j
@Component
public class ProductFeignApiFallbackFactory implements FallbackFactory<ProductFeignApi> {

    @Override
    public ProductFeignApi create(Throwable cause) {
        log.error("【商品服务远程调用熔断降级】gold-product-service调用失败，异常信息：{}", cause.getMessage(), cause);
        return new ProductFeignApi() {

            // ===================== 业务库存接口降级（订单/模拟盘调用） =====================
            @Override
            public Result<SpecInfoDTO> getSpecInfo(Long specId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，无法查询规格信息，请稍后重试");
            }

            @Override
            public Result<Void> freezeStock(DeductStockRequest request) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，库存冻结失败，请稍后重试");
            }

            @Override
            public Result<Void> unFreezeStock(DeductStockRequest request) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，库存解冻失败，请稍后重试");
            }

            @Override
            public Result<Void> deductStock(DeductStockRequest request) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，库存扣减失败，请稍后重试");
            }

            @Override
            public Result<Void> deductActivityStock(DeductActivityStockRequest request) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，活动库存冻结失败，请稍后重试");
            }

            @Override
            public Result<Void> unFreezeActivityStock(DeductActivityStockRequest request) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，活动库存解冻失败，请稍后重试");
            }

            // ===================== Admin后台管理接口降级 =====================
            @Override
            public Result<Page<GoodsAdminListVO>> pageGoods(com.gold.common.domain.dto.GoodsPageDTO dto) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，无法分页查询商品");
            }

            @Override
            public Result<GoodsAdminDetailVO> getGoodsDetail(Long goodsId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，查询商品详情失败");
            }

            @Override
            public Result<SpecAdminVO> getSpecById(Long specId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，查询规格详情失败");
            }

            @Override
            public Result<Long> addGoods(GoodsAddDTO dto) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，新增商品失败");
            }

            @Override
            public Result<Void> updateGoods(Long goodsId, GoodsUpdateDTO dto) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，修改商品失败");
            }

            @Override
            public Result<Void> deleteGoods(Long goodsId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，删除商品失败");
            }

            @Override
            public Result<Long> addSpec(Long goodsId, SpecAddDTO dto) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，新增规格失败");
            }

            @Override
            public Result<Void> updateSpec(Long specId, SpecUpdateDTO dto) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，修改规格失败");
            }

            @Override
            public Result<Void> deleteSpec(Long specId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，删除规格失败");
            }

            @Override
            public Result<Void> updateStock(Long specId, Integer stock) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，修改库存失败");
            }

            @Override
            public Result<Void> updateSaleStatus(Long specId, Integer saleStatus) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，修改上下架状态失败");
            }

            @Override
            public Result<Page<ActivityAdminListVO>> pageActivity(String activityName, Integer status, Integer page, Integer size) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，分页查询活动失败");
            }

            @Override
            public Result<ActivityAdminDetailVO> getActivityDetail(Long activityId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，查询活动详情失败");
            }

            @Override
            public Result<Long> addActivity(ActivityAddDTO dto) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，新增活动失败");
            }

            @Override
            public Result<Void> updateActivity(Long activityId, ActivityUpdateDTO dto) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，修改活动失败");
            }

            @Override
            public Result<Void> cancelActivity(Long activityId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，取消活动失败");
            }

            @Override
            public Result<Void> deleteActivity(Long activityId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，删除活动失败");
            }

            @Override
            public Result<Void> addActivityGoods(Long activityId, ActivityGoodsUpdateDTO dto) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，活动绑定商品失败");
            }

            @Override
            public Result<Void> updateActivityGoods(Long activityGoodsId, ActivityGoodsUpdateDTO dto) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，修改活动商品失败");
            }

            @Override
            public Result<Void> deleteActivityGoods(Long activityGoodsId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，移除活动商品失败");
            }

            // ===================== Mall商城前台展示接口降级 =====================
            @Override
            public Result<Page<GoodsListVO>> getGoodsList(Integer page, Integer size, String category, String metalType) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，商品列表加载失败，请稍后刷新");
            }

            @Override
            public Result<GoodsDetailVO> getMallGoodsDetail(Long id) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，商品详情加载失败，请稍后刷新");
            }

            @Override
            public Result<BuyInfoVO> getBuyInfo(Long specId, Integer quantity) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，无法获取购买信息，请稍后重试");
            }

            @Override
            public Result<Page<ActivityShowVO>> getOngoingActivities(Integer page, Integer size) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，进行中活动加载失败");
            }

            @Override
            public Result<Page<ActivityShowVO>> getUpcomingActivities(Integer page, Integer size) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂时不可用，即将开始活动加载失败");
            }

            // TODO [代码规范] 缺少 @Override 注解。同匿名类中其余 21 个方法都有 @Override，唯独此处遗漏。若 ProductFeignApi 修改了 tccFreezeStock 签名，编译器不会报错，此方法会静默失效
            public Result<Void> tccFreezeStock(DeductStockRequest request) {
                log.error("【熔断】商品服务 TCC 冻结库存不可用，orderNo={}", request.getOrderNo());
                return Result.error(ErrorCode.SYSTEM_BUSY, "商品服务暂不可用，请稍后重试");
            }
        };
    }
}