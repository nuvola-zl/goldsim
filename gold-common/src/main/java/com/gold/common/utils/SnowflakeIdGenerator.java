package com.gold.common.utils;

import com.gold.common.code.ErrorCode;
import com.gold.common.exception.BusinessException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花算法分布式ID生成器（无锁优化版）
 *
 * 优化：用 AtomicLong CAS 替换 synchronized，提升并发性能
 * 原理：将 sequence 和 lastTimestamp 打包成一个 long，CAS 原子更新
 *
 * 结构：符号位(1) + 时间戳(41) + 机器ID(10) + 序列号(12)
 */
public class SnowflakeIdGenerator {

    // 起始时间戳（2024-01-01）
    private static final long START_TIMESTAMP = 1704067200000L;

    // 位数分配
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    // 最大值
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    // 位移
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    // 机器ID
    private final long workerId;

    /**
     * 将 sequence 和 lastTimestamp 打包成一个 AtomicLong
     * 高 52 位存 lastTimestamp（相对 START_TIMESTAMP 的毫秒数）
     * 低 12 位存 sequence
     *
     * 初始值：timestamp = 0, sequence = 0
     */
    private final AtomicLong timestampAndSequence;

    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("Worker ID must be between 0 and " + MAX_WORKER_ID);
        }
        this.workerId = workerId;
        this.timestampAndSequence = new AtomicLong(0);
    }

    public long nextId() {
        while (true) {
            long current = timestampAndSequence.get();
            long currentTimestamp = getTimestampFromValue(current);
            long currentSequence = getSequenceFromValue(current);

            long now = System.currentTimeMillis();
            long relativeNow = now - START_TIMESTAMP;

            if (now < currentTimestamp + START_TIMESTAMP) {
                // 时钟回拨
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统时钟异常");
            }

            long newTimestamp;
            long newSequence;

            if (relativeNow == currentTimestamp) {
                // 同一毫秒，序列号+1
                newSequence = (currentSequence + 1) & MAX_SEQUENCE;
                newTimestamp = relativeNow;

                if (newSequence == 0) {
                    // 序列号溢出，等待下一毫秒
                    newTimestamp = tilNextMillis(currentTimestamp + START_TIMESTAMP) - START_TIMESTAMP;
                }
            } else {
                // 新的毫秒，序列号归零
                newTimestamp = relativeNow;
                newSequence = 0;
            }

            long newValue = (newTimestamp << SEQUENCE_BITS) | newSequence;

            // CAS 原子更新
            if (timestampAndSequence.compareAndSet(current, newValue)) {
                return (newTimestamp << TIMESTAMP_SHIFT)
                        | (workerId << WORKER_ID_SHIFT)
                        | newSequence;
            }
            // CAS 失败，重试
        }
    }

    private long getTimestampFromValue(long value) {
        return value >> SEQUENCE_BITS;
    }

    private long getSequenceFromValue(long value) {
        return value & MAX_SEQUENCE;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    public String nextOrderNo() {
        return String.valueOf(nextId());
    }
}