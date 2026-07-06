package com.gold.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.common.code.ErrorCode;
import com.gold.common.exception.BusinessException;
import com.gold.mallApi.domain.dto.ActivityAddDTO;
import com.gold.mallApi.domain.dto.ActivityGoodsAddDTO;
import com.gold.mallApi.domain.dto.ActivityGoodsUpdateDTO;
import com.gold.mallApi.domain.dto.ActivityUpdateDTO;
import com.gold.mallApi.domain.vo.ActivityAdminDetailVO;
import com.gold.mallApi.domain.vo.ActivityAdminListVO;
import com.gold.mallApi.domain.vo.ActivityShowVO;
import com.gold.product.entity.MallActivity;
import com.gold.product.entity.MallActivityGoods;
import com.gold.product.entity.MallGoodsSpec;
import com.gold.product.mapper.ActivityAdminMapper;
import com.gold.product.mapper.ActivityGoodsMapper;
import com.gold.product.mapper.MallActivityGoodsMapper;
import com.gold.product.mapper.MallActivityMapper;
import com.gold.product.mapper.MallGoodsSpecMapper;
import com.gold.product.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final MallActivityMapper activityMapper;
    private final MallActivityGoodsMapper activityGoodsMapper;
    private final ActivityAdminMapper activityAdminMapper;
    private final ActivityGoodsMapper activityGoodsQueryMapper;
    private final MallGoodsSpecMapper specMapper;

    // ==================== Admin: 查询 ====================

    @Override
    public Page<ActivityAdminListVO> pageActivity(String activityName, Integer status, Integer page, Integer size) {
        Page<ActivityAdminListVO> pageParam = new Page<>(page, size);
        return activityAdminMapper.selectActivityAdminPage(pageParam, activityName, status);
    }

    @Override
    public ActivityAdminDetailVO getActivityDetail(Long activityId) {
        MallActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "活动不存在");
        }

        ActivityAdminDetailVO vo = new ActivityAdminDetailVO();
        vo.setActivityId(activity.getId());
        vo.setActivityName(activity.getActivityName());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        vo.setCreateTime(activity.getCreateTime());
        vo.setUpdateTime(activity.getUpdateTime());
        vo.setGoodsList(activityAdminMapper.selectActivityGoodsByActivityId(activityId));
        return vo;
    }

    // ==================== Admin: 活动主信息 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addActivity(ActivityAddDTO dto) {
        validateActivityTime(dto.getStartTime(), dto.getEndTime());

        MallActivity activity = new MallActivity();
        activity.setActivityName(dto.getActivityName());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setStatus(0);
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        activityMapper.insert(activity);

        Long activityId = activity.getId();

        if (dto.getGoodsList() != null && !dto.getGoodsList().isEmpty()) {
            for (ActivityGoodsAddDTO goodsDto : dto.getGoodsList()) {
                MallGoodsSpec spec = specMapper.selectById(goodsDto.getSpecId());
                if (spec == null) {
                    throw new RuntimeException("规格不存在，specId=" + goodsDto.getSpecId());
                }

                Long existCount = activityGoodsMapper.selectCount(
                        new LambdaQueryWrapper<MallActivityGoods>()
                                .eq(MallActivityGoods::getActivityId, activityId)
                                .eq(MallActivityGoods::getSpecId, goodsDto.getSpecId())
                );
                if (existCount > 0) {
                    throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "该规格已存在于当前活动中");
                }

                MallActivityGoods activityGoods = new MallActivityGoods();
                activityGoods.setActivityId(activityId);
                activityGoods.setSpecId(goodsDto.getSpecId());
                activityGoods.setOriginalPrice(goodsDto.getOriginalPrice());
                activityGoods.setActivityPrice(goodsDto.getActivityPrice());
                activityGoods.setActivityStock(goodsDto.getActivityStock());
                activityGoods.setSoldCount(0);
                activityGoods.setLimitPerUser(goodsDto.getLimitPerUser() != null ? goodsDto.getLimitPerUser() : 1);
                activityGoods.setCreateTime(LocalDateTime.now());
                activityGoodsMapper.insert(activityGoods);
            }
        }

        log.info("创建活动成功，activityId={}", activityId);
        return activityId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActivity(ActivityUpdateDTO dto) {
        MallActivity activity = activityMapper.selectById(dto.getId());
        if (activity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "活动不存在");
        }
        assertActivityModifiable(activity);

        validateActivityTime(dto.getStartTime(), dto.getEndTime());

        activity.setActivityName(dto.getActivityName());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(activity);

        log.info("修改活动成功，activityId={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelActivity(Long activityId) {
        MallActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "活动不存在");
        }
        assertActivityModifiable(activity);

        int rows = activityAdminMapper.cancelActivity(activityId);
        if (rows <= 0) {
            throw new RuntimeException("取消活动失败，活动状态可能已变更");
        }

        log.info("取消活动成功，activityId={}", activityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long activityId) {
        MallActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "活动不存在");
        }
        if (activity.getStatus() != 2 && activity.getStatus() != 3) {
            throw new RuntimeException("只能删除已结束或已取消的活动");
        }

        activityGoodsMapper.delete(
                new LambdaQueryWrapper<MallActivityGoods>()
                        .eq(MallActivityGoods::getActivityId, activityId)
        );

        activityMapper.deleteById(activityId);
        log.info("删除活动成功，activityId={}", activityId);
    }

    // ==================== Admin: 活动商品 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addActivityGoods(Long activityId, ActivityGoodsUpdateDTO dto) {
        MallActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "活动不存在");
        }
        assertActivityModifiable(activity);

        if (dto.getSpecId() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "规格ID不能为空");
        }

        MallGoodsSpec spec = specMapper.selectById(dto.getSpecId());
        if (spec == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "规格不存在");
        }

        Long existCount = activityGoodsMapper.selectCount(
                new LambdaQueryWrapper<MallActivityGoods>()
                        .eq(MallActivityGoods::getActivityId, activityId)
                        .eq(MallActivityGoods::getSpecId, dto.getSpecId())
        );
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "该规格已存在于当前活动中");
        }

        Long otherCount = activityGoodsMapper.selectCount(
                new LambdaQueryWrapper<MallActivityGoods>()
                        .eq(MallActivityGoods::getSpecId, dto.getSpecId())
                        .ne(MallActivityGoods::getActivityId, activityId)
        );
        if (otherCount > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "该规格已关联其他活动，不能重复添加");
        }

        MallActivityGoods activityGoods = new MallActivityGoods();
        activityGoods.setActivityId(activityId);
        activityGoods.setSpecId(dto.getSpecId());
        activityGoods.setOriginalPrice(dto.getOriginalPrice());
        activityGoods.setActivityPrice(dto.getActivityPrice());
        activityGoods.setActivityStock(dto.getActivityStock());
        activityGoods.setSoldCount(0);
        activityGoods.setLimitPerUser(dto.getLimitPerUser() != null ? dto.getLimitPerUser() : 1);
        activityGoods.setCreateTime(LocalDateTime.now());
        activityGoodsMapper.insert(activityGoods);

        log.info("活动添加商品成功，activityId={}, specId={}", activityId, dto.getSpecId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActivityGoods(Long activityGoodsId, ActivityGoodsUpdateDTO dto) {
        MallActivityGoods activityGoods = activityGoodsMapper.selectById(activityGoodsId);
        if (activityGoods == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "活动商品关联不存在");
        }

        MallActivity activity = activityMapper.selectById(activityGoods.getActivityId());
        if (activity == null || activity.getStatus() != 0) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "只有待开始的活动才能修改商品");
        }

        if (dto.getActivityStock() < activityGoods.getSoldCount()) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "活动库存不能低于已售出数量");
        }

        activityGoods.setOriginalPrice(dto.getOriginalPrice());
        activityGoods.setActivityPrice(dto.getActivityPrice());
        activityGoods.setActivityStock(dto.getActivityStock());
        activityGoods.setLimitPerUser(dto.getLimitPerUser());
        activityGoodsMapper.updateById(activityGoods);

        log.info("修改活动商品成功，activityGoodsId={}", activityGoodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivityGoods(Long activityGoodsId) {
        MallActivityGoods activityGoods = activityGoodsMapper.selectById(activityGoodsId);
        if (activityGoods == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "活动商品关联不存在");
        }

        MallActivity activity = activityMapper.selectById(activityGoods.getActivityId());
        if (activity == null || activity.getStatus() != 0) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "只有待开始的活动才能删除商品");
        }

        if (activityGoods.getSoldCount() > 0) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "该商品已有售出记录，不能删除");
        }

        activityGoodsMapper.deleteById(activityGoodsId);
        log.info("删除活动商品成功，activityGoodsId={}", activityGoodsId);
    }

    // ==================== Mall: 活动展示 ====================

    @Override
    public Page<ActivityShowVO> getOngoingActivities(Integer page, Integer size) {
        Page<ActivityShowVO> pageParam = new Page<>(page, size);
        LocalDateTime now = LocalDateTime.now();
        // ✅ 真分页：MyBatis-Plus 自动在 SQL 末尾加 LIMIT
        return activityGoodsQueryMapper.selectOngoingActivityList(pageParam, now);
    }

    @Override
    public Page<ActivityShowVO> getUpcomingActivities(Integer page, Integer size) {
        Page<ActivityShowVO> pageParam = new Page<>(page, size);
        LocalDateTime now = LocalDateTime.now();
        return activityGoodsQueryMapper.selectUpcomingActivityList(pageParam, now);
    }

    // ==================== 私有方法 ====================

    private void validateActivityTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "活动开始时间不能早于当前时间");
        }
        if (endTime.isBefore(startTime)) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "活动结束时间不能早于开始时间");
        }
    }

    private void assertActivityModifiable(MallActivity activity) {
        if (activity.getStatus() != 0) {
            throw new BusinessException(ErrorCode.RESOURCE_STATUS_INVALID, "只有待开始的活动才能进行操作");
        }
    }
}