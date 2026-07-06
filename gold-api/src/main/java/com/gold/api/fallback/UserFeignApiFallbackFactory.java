package com.gold.api.fallback;

import com.gold.api.domain.dto.UserInfoDTO;
import com.gold.api.feign.UserFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务Feign降级工厂
 * 调用gold-user-service超时/熔断/下线时触发降级
 */
@Slf4j
@Component
public class UserFeignApiFallbackFactory implements FallbackFactory<UserFeignApi> {

    @Override
    public UserFeignApi create(Throwable cause) {
        log.error("【用户服务熔断降级】gold-user-service调用异常：{}", cause.getMessage(), cause);
        return new UserFeignApi() {
            /**
             * 根据用户id查询用户信息（微服务远程调用专用）
             */
            @Override
            public Result<UserInfoDTO> getUserInfo(Long userId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "用户服务暂时不可用，查询用户信息失败");
            }
        };
    }
}