package com.gold.news.controller;

import com.gold.news.client.AiBriefApiClient;
import com.gold.news.job.NewsSyncJob;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final NewsSyncJob newsSyncJob;


    private final AiBriefApiClient aiBriefApiClient;

    /**
     * 立即触发新闻同步
     */
    @GetMapping("/sync-now")
    public String syncNow() {
        newsSyncJob.syncNews();
        return "同步完成，请查看日志";
    }

    @GetMapping("/test-ai")
    public String testAi() {
        List<AiBriefApiClient.AiBriefItem> items = aiBriefApiClient.fetchAiBriefs("money", 1, 5);
        if (items.isEmpty()) {
            return "AI简报返回空";
        }

        StringBuilder sb = new StringBuilder();
        for (AiBriefApiClient.AiBriefItem item : items) {
            sb.append("标题：").append(item.getTitle()).append("\n");
            sb.append("摘要：").append(item.getSummary()).append("\n");
            sb.append("来源：").append(item.getAuthor_name()).append("\n");
            sb.append("时间：").append(item.getPublish_date()).append("\n");
            sb.append("---\n");
        }
        return sb.toString();
    }
}