package com.gold.order.controller;

import com.gold.common.context.UserContext;
import com.gold.common.result.Result;
import com.gold.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order/pay")
@RequiredArgsConstructor
public class PayController {
    private final OrderService orderService;

    /**
     * 去支付，返回支付宝支付HTML
     *
     * 【已修复】安全漏洞
     *
     * 原问题(todo):
     * - userId 来自客户端 @RequestParam，可实现越权支付
     * - 攻击者提供目标用户 userId 和自己的 token 即可替他人支付
     *
     * 修复内容(2024-07-10):
     * - userId 改为从 UserContext.getUserId() 获取当前登录用户 ID
     * - 不再信任客户端传入的 userId
     *
     * @param orderNo 订单号
     */
    @PostMapping("/{orderNo}")
    public Result<String> goPay(@PathVariable String orderNo) {
        Long userId = UserContext.getUserId();
        String payHtml = orderService.generatePayHtml(orderNo, userId);
        return Result.success(payHtml);
    }
}