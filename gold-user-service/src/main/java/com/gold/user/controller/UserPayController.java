package com.gold.user.controller;


import com.gold.api.feign.OrderFeignApi;
import com.gold.common.context.UserContext;
import com.gold.common.result.Result;
import com.gold.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/pay")
@RequiredArgsConstructor
public class UserPayController {


    private final OrderFeignApi orderFeignApi;


    @PostMapping("/{orderNo}")
    public Result<String> goPay(@PathVariable String orderNo) {
        // 公共过滤器已经自动从网关header读取userId存入上下文
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("A00001","请登录");
        }
        // feign传递userId给订单服务做订单归属校验
        return orderFeignApi.generatePayHtml(orderNo, userId);
    }
}