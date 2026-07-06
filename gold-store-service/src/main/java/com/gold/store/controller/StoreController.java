package com.gold.store.controller;

import com.gold.common.result.PageResult;
import com.gold.common.result.Result;
import com.gold.store.domain.po.Store;
import com.gold.store.domain.vo.StoreDetailVO;
import com.gold.store.domain.vo.StoreListVO;
import com.gold.store.domain.vo.StoreNearbyVO;
import com.gold.store.service.StoreService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/store")
public class StoreController {

    private final StoreService storeService;

    /**
     * 门店列表（用户端）
     * GET /store/list?page=1&size=10&city=北京市
     */
    @GetMapping("/list")
    public Result<PageResult<StoreListVO>> getStoreList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword) {
        Page<StoreListVO> pageResult = storeService.getStoreList(page, size, city, keyword);
        PageResult<StoreListVO> result = PageResult.build(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    /**
     * 门店详情（用户端）
     * GET /store/{storeId}
     */
    @GetMapping("/{storeId}")
    public Result<StoreDetailVO> getStoreDetail(@PathVariable Long storeId) {
        StoreDetailVO vo = storeService.getStoreDetail(storeId);
        return Result.success(vo);
    }

    /**
     * 附近门店查询接口
     * @param lng 用户当前经度
     * @param lat 用户当前纬度
     * @param radius 搜索半径（公里），默认5
     * @return 由近到远门店列表
     */
    @GetMapping("/nearby")
    public Result<List<StoreNearbyVO>> getNearbyStores(
            @RequestParam Double lng,
            @RequestParam Double lat,
            @RequestParam(required = false, defaultValue = "5") Double radius) {
        List<StoreNearbyVO> nearbyList = storeService.getNearbyStores(lng, lat, radius);
        return Result.success(nearbyList);
    }


    // ========== 新增：后台新增门店接口 ==========
    @PostMapping
    public Result<Void> addStore(@RequestBody Store store) {
        storeService.addStore(store);
        return Result.success();
    }

    // ========== 新增：后台编辑门店接口 ==========
    @PutMapping("/{storeId}")
    public Result<Void> updateStore(@PathVariable Long storeId, @RequestBody Store store) {
        store.setId(storeId);
        storeService.updateStore(store);
        return Result.success();
    }

    /**
     * 高分推荐门店
     * todo 以后可以更加智能的推荐，比如根据用户评价、距离等
     * @param city 筛选城市（可选）
     * @param limit 最多返回条数，默认10
     * @return 高分门店列表
     */
    @GetMapping("/recommend")
    public Result<List<StoreListVO>> getRecommendStores(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<StoreListVO> recommendList = storeService.getRecommendStores(city, limit);
        return Result.success(recommendList);
    }
}