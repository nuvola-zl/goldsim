package com.gold.api.feign;

import com.gold.api.fallback.FileFeignApiFallback;
import com.gold.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;


 // ← 加日志
@FeignClient(
        name = "gold-file-service",
        contextId = "fileFeignApi",
        configuration = com.gold.common.config.FeignMultipartConfig.class,
        fallbackFactory = FileFeignApiFallback.class
)
public interface FileFeignApi {

    @PostMapping(value = "/file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<String> upload(@RequestPart("file") MultipartFile file);
}