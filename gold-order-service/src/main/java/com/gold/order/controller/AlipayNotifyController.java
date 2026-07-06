package com.gold.order.controller;

import com.alipay.api.internal.util.AlipaySignature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gold.common.code.ErrorCode;
import com.gold.common.result.Result;
import com.gold.order.config.AlipayConfig;
import com.gold.order.domain.po.MallOrder;
import com.gold.order.mapper.MallOrderMapper;
import com.gold.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/order/pay")
@RequiredArgsConstructor
public class AlipayNotifyController {
    private final AlipayConfig alipayConfig;
    private final OrderService orderService;
    private final MallOrderMapper orderMapper;

    /**
     * 支付宝异步回调接口
     */
    @PostMapping("/notify")
    public String payNotify(HttpServletRequest request) {
        // 1. 接收所有回调参数
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> paramMap = request.getParameterMap();
        for (String key : paramMap.keySet()) {
            params.put(key, String.join(",", paramMap.get(key)));
        }

        // 2. RSA2验签
        boolean signOk;
        try {
            signOk = AlipaySignature.rsaCheckV1(params, alipayConfig.getAlipayPublicKey(), "UTF-8", "RSA2");
        } catch (Exception e) {
            log.error("支付宝回调验签异常", e);
            return "fail";
        }
        if (!signOk) {
            log.error("支付宝回调非法请求，验签失败");
            return "fail";
        }

        // 3. 提取参数
        String orderNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");

        // 仅支付成功执行业务
        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            // TODO [安全加固] 未校验 total_amount（回调中的支付金额）与订单 payAmount 是否一致。虽然 RSA2 验签已保证防篡改，但防御深度原则下应增加金额校验，防止配置错误导致的金额不匹配
            orderService.handleOrderPaySuccess(orderNo, tradeNo);
        }
        // 返回success，支付宝不再重试
        return "success";
    }

    /**
     * 同步跳转页面
     */
    @RequestMapping("/return")
    public Result<String> payReturn(@RequestParam String out_trade_no) {
        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallOrder::getOrderNo, out_trade_no);
        MallOrder order = orderMapper.selectOne(wrapper);
        if (order == null) {
            return Result.error(ErrorCode.RESOURCE_NOT_FOUND,"订单不存在");
        }
        if (order.getStatus() == 1) {
            return Result.success("支付成功");
        }
        return Result.error("A00001","支付处理中，请稍后刷新订单");
    }
}