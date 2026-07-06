package com.gold.news.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JuheApiClient {

    @Value("${juhe.api.key}")
    private String apiKey;

    @Value("${juhe.api.url:https://apis.juhe.cn/fapigx/caijing/query}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 拉取财经新闻（适配实际返回格式）
     */
    public List<JuheNewsItem> fetchFinanceNews(int page, int pageSize) {
        String url = String.format("%s?key=%s&num=%d&page=%d", apiUrl, apiKey, pageSize, page);
        List<JuheNewsItem> resultList = new ArrayList<>();
        try {
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSON.parseObject(response);

            if (json != null && json.getInteger("error_code") != 0) {
                log.error("聚合数据API返回错误：{}", json.getString("reason"));
                return resultList;
            }

            JSONObject result = null;
            if (json != null) {
                result = json.getJSONObject("result");
            }
            if (result != null) {
                resultList = result.getList("newslist", JuheNewsItem.class);
            }
        } catch (Exception e) {
            log.error("拉取财经新闻失败", e);
        }
        // 所有路径最终都会走到这里返回
        return resultList;
    }

    
    /**
     * 新闻条目DTO（适配实际字段）
     */
    @Data
    public static class JuheNewsItem {
        private String id;           // 新闻唯一ID
        private String ctime;        // 发布时间 "2026-07-07 20:48:56"
        private String title;        // 标题
        private String description;  // 摘要（实际返回为空）
        private String source;       // 来源 "澎湃新闻"
        private String picUrl;       // 图片URL
        private String url;          // 原文链接
    }
}