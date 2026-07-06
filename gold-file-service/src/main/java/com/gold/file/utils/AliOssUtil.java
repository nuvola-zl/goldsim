package com.gold.file.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.gold.common.code.ErrorCode;
import com.gold.common.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

@Slf4j
public class AliOssUtil {

    private final String endpoint;
    private final String bucketName;

    // 单例 OSS 客户端，复用连接池
    private final OSS ossClient;

    public AliOssUtil(String endpoint, String accessKeyId, String accessKeySecret, String bucketName) {
        this.endpoint = endpoint;
        this.bucketName = bucketName;
        // 构造时创建客户端，accessKeyId/accessKeySecret 只在构造时使用
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

    }

    /**
     * 上传文件
     */
    public String upload(byte[] bytes, String objectName) {
        try {
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            log.error("OSS 上传失败: Message={}, Code={}, RequestId={}, HostId={}",
                    oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            throw new BusinessException(ErrorCode.OSS_UPLOAD_FAILED,"文件上传至 OSS 失败: " + oe.getErrorMessage());
        } catch (ClientException ce) {
            log.error("OSS 客户端异常: ErrorCode={}, ErrorMessage={}",
                    ce.getErrorCode(), ce.getMessage());
            throw new BusinessException(ErrorCode.CLIENT_ERROR,"OSS 客户端连接异常: " + ce.getMessage());
        }

        // 安全的 URL 拼接
        String cleanEndpoint = endpoint.replaceFirst("^(https?://)", "");

        return "https://" + bucketName + "." + cleanEndpoint + "/" + objectName;
    }

    /**
     * 应用关闭时释放资源
     */
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS 客户端已安全关闭");
        }
    }
}