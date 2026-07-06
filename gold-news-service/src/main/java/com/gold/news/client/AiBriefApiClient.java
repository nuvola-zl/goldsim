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
public class AiBriefApiClient {

    @Value("${juhe.ai.key}")
    private String aiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 拉取AI新闻简报
     */
    public List<AiBriefItem> fetchAiBriefs(String type, int page, int pageSize) {
        String url = String.format(
            "https://apis.juhe.cn/fapigw/aibrief/list?key=%s&type=%s&page=%d&page_size=%d",
            aiKey, type, page, pageSize
        );
        
        List<AiBriefItem> resultList = new ArrayList<>();
        try {
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSON.parseObject(response);

            if (json == null || json.getInteger("error_code") != 0) {
                log.error("AI简报API返回错误：{}", json != null ? json.getString("reason") : "空响应");
                return resultList;
            }

            JSONObject result = json.getJSONObject("result");
            if (result != null) {
                resultList = result.getList("list", AiBriefItem.class);
            }
        } catch (Exception e) {
            log.error("拉取AI简报失败", e);
        }
        return resultList;
    }

    @Data
    public static class AiBriefItem {
        private String id;
        private String title;
        private String author_name;  // 来源
        private String type;         // 类型：money=财经
        private String image_url;
        private String summary;      // AI总结摘要
        private String publish_date; // 发布时间
        private String url;          // 原文链接
    }
}