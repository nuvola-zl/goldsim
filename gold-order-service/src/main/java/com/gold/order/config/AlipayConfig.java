package com.gold.order.config;


import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {
    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String notifyUrl;
    private String returnUrl;

    @Bean
    public AlipayClient alipayClient() {
        // 沙箱固定网关
        String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";  // ✅ 新地址
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                privateKey,
                "json",
                "UTF-8",
                alipayPublicKey,
                "RSA2"
        );
    }
}