package com.gold.api.fallback;

import com.gold.api.feign.FileFeignApi;
import com.gold.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
// TODO [命名规范] 同 MarketFeignApiFallback，类名是 Fallback 但实现 FallbackFactory，应改名为 FileFeignApiFallbackFactory
public class FileFeignApiFallback implements FallbackFactory<FileFeignApi> {

    @Override
    public FileFeignApi create(Throwable cause) {
        log.error("调用文件服务gold-file-service远程接口失败，触发熔断降级，异常信息：{}",
                cause != null ? cause.getMessage() : "服务不可用");

        return new FileFeignApi() {
            @Override
            public Result<String> upload(MultipartFile file) {
                // 文件上传失败，返回明确错误，不要返回假 URL
                return Result.error("C06001", "文件服务暂不可用，请稍后重试");
            }
        };
    }
}