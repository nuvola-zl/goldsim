package com.gold.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.mallApi.domain.dto.ActivityAddDTO;
import com.gold.mallApi.domain.dto.ActivityGoodsUpdateDTO;
import com.gold.mallApi.domain.dto.ActivityUpdateDTO;
import com.gold.mallApi.domain.vo.ActivityAdminDetailVO;
import com.gold.mallApi.domain.vo.ActivityAdminListVO;
import com.gold.mallApi.domain.vo.ActivityShowVO;

public interface ActivityService {

    /**
     * 分页查询活动列表
     */
    Page<ActivityAdminListVO> pageActivity(String activityName, Integer status, Integer page, Integer size);

    /**
     * 查询活动详情（含关联商品）
     */
    ActivityAdminDetailVO getActivityDetail(Long activityId);

    /**
     * 创建活动
     */
    Long addActivity(ActivityAddDTO dto);

    /**
     * 修改活动（仅待开始状态可修改）
     */
    void updateActivity(ActivityUpdateDTO dto);

    /**
     * 取消活动（仅待开始状态可取消）
     */
    void cancelActivity(Long activityId);

    /**
     * 删除活动（仅已取消/已结束可删除）
     */
    void deleteActivity(Long activityId);

    /**
     * 给活动添加关联商品
     */
    void addActivityGoods(Long activityId, ActivityGoodsUpdateDTO dto);

    /**
     * 修改活动关联商品
     */
    void updateActivityGoods(Long activityGoodsId, ActivityGoodsUpdateDTO dto);

    /**
     * 删除活动关联商品
     */
    void deleteActivityGoods(Long activityGoodsId);





    //------------ Mall ====================
    /**
     * 查询进行中的活动列表
     */
    Page<ActivityShowVO> getOngoingActivities(Integer page, Integer size);

    /**
     * 查询即将开始的活动列表
     */
    Page<ActivityShowVO> getUpcomingActivities(Integer page, Integer size);
}