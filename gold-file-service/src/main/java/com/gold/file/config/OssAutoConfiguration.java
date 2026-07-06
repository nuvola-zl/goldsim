package com.gold.file.config;



import com.gold.file.properties.AliOssProperties;
import com.gold.file.utils.AliOssUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AliOssProperties.class)
@ConditionalOnProperty(prefix = "ff.oss", name = "endpoint")  // 有配置才启用
public class OssAutoConfiguration {

    @Bean
    public AliOssUtil aliOssUtil(AliOssProperties props) {
        return new AliOssUtil(
            props.getEndpoint(),
            props.getAccessKeyId(),
            props.getAccessKeySecret(),
            props.getBucketName()
        );
    }
}