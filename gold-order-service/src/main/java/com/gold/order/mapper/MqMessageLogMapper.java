package com.gold.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gold.order.domain.po.MqMessageLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface MqMessageLogMapper extends BaseMapper<MqMessageLog> {

    @Select("SELECT * FROM mq_message_log WHERE status = 0 AND retry_count < max_retry ORDER BY create_time LIMIT 100")
    List<MqMessageLog> selectPendingMessages();

    @Select("SELECT * FROM mq_message_log WHERE status = 0 AND retry_count >= max_retry ORDER BY create_time LIMIT 100")
    // 【已修复】兜底查询超限消息
    // 原问题(todo):
    //超过 max_retry 的消息不会被扫描到，status 仍为 0，永远留在数据库
    //[消息丢弃] 超过 max_retry 的消息不会被此查询扫描到（retry_count >= max_retry），但 status 仍为 0，永远留在数据库无人处理。应增加兜底：将超限消息状态改为 status=-1（失败终态）并触发告警
    // 修复：新增此方法查询死信消息，配合定时任务标记为失败终态并告警
    List<MqMessageLog> selectDeadMessages();

    @Update("UPDATE mq_message_log SET status = -1, update_time = NOW() WHERE id = #{id}")
    int markDead(@Param("id") Long id);
    
    @Update("UPDATE mq_message_log SET retry_count = retry_count + 1, update_time = NOW() WHERE id = #{id}")
    int increaseRetry(@Param("id") Long id);
}