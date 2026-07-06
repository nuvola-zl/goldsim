package com.gold.order.service.impl;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alibaba.fastjson2.JSONObject;
import com.gold.common.code.ErrorCode;
import com.gold.common.exception.BusinessException;
import com.gold.order.config.AlipayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService {
    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;

    /**
     * 生成电脑网页支付表单HTML
     * @param orderNo 商户订单号
     * @param totalAmount 支付金额
     * @param subject 订单标题（商品名称）
     * @return 自动提交HTML表单
     */
    public String createPayForm(String orderNo, BigDecimal totalAmount, String subject) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // 异步通知地址
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        // 支付成功同步跳转页面
        request.setReturnUrl(alipayConfig.getReturnUrl());

        // 业务参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo);
        bizContent.put("total_amount", totalAmount);
        bizContent.put("subject", subject);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());

        try {
            AlipayTradePagePayResponse resp = alipayClient.pageExecute(request);
            if (!resp.isSuccess()) {
                log.error("生成支付表单失败 code:{},msg:{}", resp.getCode(), resp.getMsg());
                throw new BusinessException(ErrorCode.CREATE_MESSAGE_FAILED,"支付创建失败：" + resp.getMsg());
            }
            return resp.getBody();
        } catch (Exception e) {
            log.error("支付宝调用异常", e);
            throw new BusinessException(ErrorCode.ALIPAY_ERROR,"支付服务异常");
        }
    }
}