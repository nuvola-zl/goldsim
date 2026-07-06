package com.gold.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.mallApi.domain.vo.ActivityAdminListVO;
import com.gold.mallApi.domain.vo.ActivityGoodsAdminVO;
import com.gold.product.entity.MallActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ActivityAdminMapper extends BaseMapper<MallActivity> {

    /**
     * 管理端分页查询活动列表
     */
    Page<ActivityAdminListVO> selectActivityAdminPage(Page<ActivityAdminListVO> page,
                                                      @Param("activityName") String activityName,
                                                      @Param("status") Integer status);

    /**
     * 查询活动关联的商品规格列表
     */
    List<ActivityGoodsAdminVO> selectActivityGoodsByActivityId(@Param("activityId") Long activityId);

    /**
     * 取消活动
     */
    @Update("UPDATE mall_activity SET status = 3, update_time = NOW() WHERE id = #{activityId} AND status = 0")
    int cancelActivity(@Param("activityId") Long activityId);

    /**
     * 统计活动关联商品数量
     */
    @Select("SELECT COUNT(*) FROM mall_activity_goods WHERE activity_id = #{activityId}")
    int countGoodsByActivityId(@Param("activityId") Long activityId);

    /**
     * 统计活动总售出
     */
    @Select("SELECT COALESCE(SUM(sold_count), 0) FROM mall_activity_goods WHERE activity_id = #{activityId}")
    int sumSoldByActivityId(@Param("activityId") Long activityId);
}
