//package com.gold.common.utils;
//
//import lombok.RequiredArgsConstructor;
//import org.redisson.api.RBucket;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.stereotype.Component;
//
//import java.util.concurrent.TimeUnit;
//
//@Component
//@RequiredArgsConstructor
//public class RedisCacheUtil {
//
//    private final RedissonClient redissonClient;
//
//    // ==================== String 操作 ====================
//
//    public <T> void set(String key, T value, long expire, TimeUnit unit) {
//        RBucket<T> bucket = redissonClient.getBucket(key);
//        bucket.set(value, expire, unit);
//    }
//
//    /**
//     * 原子设置：key不存在时才设置，返回true；已存在返回false
//     * 用于防刷限流等需要原子判断的场景
//     */
//    public <T> boolean trySet(String key, T value, long expire, TimeUnit unit) {
//        RBucket<T> bucket = redissonClient.getBucket(key);
//        return bucket.trySet(value, expire, unit);
//    }
//
//    public <T> T get(String key) {
//        RBucket<T> bucket = redissonClient.getBucket(key);
//        return bucket.get();
//    }
//
//    public boolean delete(String key) {
//        return redissonClient.getBucket(key).delete();
//    }
//
//    public boolean exists(String key) {
//        return redissonClient.getBucket(key).isExists();
//    }
//
//    // ==================== 分布式锁 ====================
//
//    public RLock getLock(String lockKey) {
//        return redissonClient.getLock(lockKey);
//    }
//
//    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
//        RLock lock = getLock(lockKey);
//        return lock.tryLock(waitTime, leaseTime, unit);
//    }
//
//    public void unlock(String lockKey) {
//        RLock lock = getLock(lockKey);
//        if (lock.isHeldByCurrentThread()) {
//            lock.unlock();
//        }
//    }
//}

package com.gold.common.utils;

import com.gold.common.exception.BusinessException;
import com.gold.common.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheUtil {

    private final RedissonClient redissonClient;

    // ==================== String 操作 ====================

    public <T> void set(String key, T value, long expire, TimeUnit unit) {
        if (key == null || key.isBlank()) {
            log.warn("redis set key为空，直接返回");
            return;
        }
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, expire, unit);
    }

    public <T> boolean trySet(String key, T value, long expire, TimeUnit unit) {
        if (key == null || key.isBlank()) {
            log.warn("redis trySet key为空，返回false");
            return false;
        }
        RBucket<T> bucket = redissonClient.getBucket(key);
        boolean success = bucket.trySet(value, expire, unit);
        log.debug("trySet key={}, result={}", key, success);
        return success;
    }

    public <T> T get(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public boolean delete(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return redissonClient.getBucket(key).delete();
    }

    public boolean exists(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return redissonClient.getBucket(key).isExists();
    }

    // ==================== 分布式锁 ====================

    /**
     * 获取分布式锁对象
     * 调用方自行管理加锁/解锁生命周期
     *
     * 标准用法：
     * <pre>
     * RLock lock = redisCacheUtil.getLock("myKey");
     * boolean lockSuccess = false;
     * try {
     *     lockSuccess = lock.tryLock(3, 15, TimeUnit.SECONDS);
     *     if (lockSuccess) {
     *         // 执行业务
     *     }
     * } catch (InterruptedException e) {
     *     Thread.currentThread().interrupt();
     * } finally {
     *     if (lockSuccess && lock.isHeldByCurrentThread()) {
     *         lock.unlock();
     *     }
     * }
     * </pre>
     */
    public RLock getLock(String lockKey) {
        if (lockKey == null || lockKey.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "锁key不能为空");
        }
        return redissonClient.getLock(lockKey);
    }
}