package com.gold.news.domain.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * AI 每日简报 VO
 */
@Data
@Builder
public class AiBriefVO {
    private String date;              // 简报日期，如 "2026-07-09"
    private List<BriefItem> items;    // 简报条目列表
    
    @Data
    @Builder
    public static class BriefItem {
        private String title;         // 新闻标题
        private String summary;       // AI 摘要（数据库里的 summary）
        private String source;        // 来源
        private String category;      // 分类
        private String url;           // 原文链接（sourceUrl）
    }
}