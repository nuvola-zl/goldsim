package com.gold.api.feign;

import com.gold.api.domain.dto.UserInfoDTO;
import com.gold.api.fallback.UserFeignApiFallbackFactory;
import com.gold.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "gold-user-service",
        contextId = "userFeignApi",
        fallbackFactory = UserFeignApiFallbackFactory.class
)
public interface UserFeignApi {
    /**
     * 根据用户id查询用户信息（微服务远程调用专用）
     */
    @GetMapping("/user/info/{userId}")
    Result<UserInfoDTO> getUserInfo(@PathVariable("userId") Long userId);
}