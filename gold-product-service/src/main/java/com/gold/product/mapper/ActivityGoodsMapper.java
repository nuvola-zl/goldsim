package com.gold.product.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.mallApi.domain.vo.ActivityShowVO;
import com.gold.product.entity.ActivityGoodsInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ActivityGoodsMapper {

    /**
     * 查询指定规格ID列表的进行中/待开始的活动
     */
    List<ActivityGoodsInfo> selectActiveBySpecIds(@Param("specIds") List<Long> specIds,
                                                  @Param("nowTime") LocalDateTime nowTime);


    /** 【新增】查进行中活动：已经开始且未结束 */
    List<ActivityGoodsInfo> selectOngoingBySpecIds(@Param("specIds") List<Long> specIds,
                                                   @Param("nowTime") LocalDateTime nowTime);

    /** 【新增】查即将开始活动：status=0 且 startTime > now */
    List<ActivityGoodsInfo> selectUpcomingBySpecIds(@Param("specIds") List<Long> specIds,
                                                    @Param("nowTime") LocalDateTime nowTime);

    /** 【新增】查单个规格的进行中活动（购买页用） */
    ActivityGoodsInfo selectOngoingBySpecId(@Param("specId") Long specId,
                                            @Param("nowTime") LocalDateTime nowTime);

    /** 查询进行中的活动列表（活动页用） */
    Page<ActivityShowVO> selectOngoingActivityList(Page<ActivityShowVO> page,
                                                   @Param("nowTime") LocalDateTime nowTime);

    /** 查询即将开始的活动列表（活动页用） */
    Page<ActivityShowVO> selectUpcomingActivityList(Page<ActivityShowVO> page,
                                                    @Param("nowTime") LocalDateTime nowTime);
}