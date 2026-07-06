package com.gold.store.service;

import com.gold.store.domain.po.Store;
import com.gold.store.domain.vo.StoreDetailVO;
import com.gold.store.domain.vo.StoreListVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.store.domain.vo.StoreNearbyVO;

import java.util.List;

public interface StoreService {

    /**
     * 门店列表（分页 + 城市筛选 + 关键词筛选）
     */
    Page<StoreListVO> getStoreList(Integer page, Integer size, String city,String keyword);

    /**
     * 门店详情
     */
    StoreDetailVO getStoreDetail(Long storeId);

    /**
     * 校验门店是否有效（营业中）
     */
    boolean isStoreValid(Long storeId);

    /**
     * 查询用户周边营业门店（Redis Geo实现）
     * @param lng 用户经度
     * @param lat 用户纬度
     * @param radius 搜索半径，单位公里，不传默认5公里
     * @return 按距离由近到远排序门店列表
     */
    List<StoreNearbyVO> getNearbyStores(Double lng, Double lat, Double radius);


    /**
     * 新增门店，自动同步Redis Geo坐标
     */
    void addStore(Store store);

    /**
     * 修改门店，经纬度变更时同步更新Redis Geo
     */
    void updateStore(Store store);


    /**
     * 高分推荐门店（简单版：按评分、评价人数降序）
     * @param city 筛选城市，不传查全部
     * @param limit 返回条数
     * @return 推荐门店列表
     */
    List<StoreListVO> getRecommendStores(String city, Integer limit);
}