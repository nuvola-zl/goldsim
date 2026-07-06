package com.gold.news.job;

import com.gold.news.client.AiBriefApiClient;
import com.gold.news.client.AiBriefApiClient.AiBriefItem;
import com.gold.news.doc.NewsArticleDoc;
import com.gold.news.domain.po.NewsArticle;
import com.gold.news.mapper.NewsMapper;
import com.gold.news.service.NewsFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsSyncJob {

    private final AiBriefApiClient aiBriefApiClient;
    private final NewsFilterService newsFilterService;
    private final NewsMapper newsMapper;
    private final ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void initIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(NewsArticleDoc.class);
            if (!indexOps.exists()) {
                boolean created = indexOps.createWithMapping();
                log.info("ES索引 news_article 创建结果: {}", created);
            } else {
                log.info("ES索引 news_article 已存在，跳过创建");
            }
        } catch (Exception e) {
            log.error("ES索引初始化失败: {}", e.getMessage(), e);
        }
    }

    // 每天9点和15点异步执行
    @Scheduled(cron = "${sync.cron:0 0 9,15 * * ?}")
    @Async
    public void syncNews() {
        log.info("开始异步同步AI财经简报...");
        int savedCount = 0;
        int filteredCount = 0;

        List<AiBriefApiClient.AiBriefItem> items = aiBriefApiClient.fetchAiBriefs("money", 1, 20);
        log.info("AI简报拉取到{}条新闻", items.size());

        if (items.isEmpty()) {
            log.warn("AI简报接口返回空，可能是次数用完或接口异常");
            return;
        }

        for (AiBriefApiClient.AiBriefItem item : items) {
            if (!newsFilterService.isGoldRelated(item.getTitle(), item.getSummary())) {
                filteredCount++;
                continue;
            }

            if (isDuplicate(item.getUrl(), item.getTitle(), item.getAuthor_name())) {
                continue;
            }

            NewsArticle article = saveToMySQL(item);
            savedCount++;
            syncToEs(article);
        }

        log.info("财经新闻同步完成，保存{}条，过滤{}条", savedCount, filteredCount);
    }

    private boolean isDuplicate(String url, String title, String source) {
        // 有URL就用URL查重
        if (url != null && !url.isEmpty()) {
            boolean exists = newsMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NewsArticle>()
                            .eq(NewsArticle::getSourceUrl, url)
            ) > 0;
            if (exists) {
                log.info("URL已存在，跳过：{}", url);
            }
            return exists;
        }

        // URL为空，用标题+来源查重
        if (title != null && !title.isEmpty()) {
            boolean exists = newsMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NewsArticle>()
                            .eq(NewsArticle::getTitle, title)
                            .eq(NewsArticle::getSource, source != null ? source : "")
            ) > 0;
            if (exists) {
                log.info("标题+来源已存在，跳过：{} | {}", title, source);
            }
            return exists;
        }

        return false;
    }

    private NewsArticle saveToMySQL(AiBriefApiClient.AiBriefItem item) {
        String summary = item.getSummary();
        if (summary == null || summary.trim().isEmpty()) {
            summary = item.getTitle();
        }
        if (summary.length() > 1000) {
            summary = summary.substring(0, 1000) + "...";
        }

        NewsArticle article = new NewsArticle();
        article.setTitle(item.getTitle());
        article.setSummary(summary);
        article.setSource(item.getAuthor_name());
        article.setSourceUrl(item.getUrl());
        article.setCategory(newsFilterService.classify(item.getTitle(), summary));
        article.setTags(newsFilterService.extractTags(item.getTitle(), summary));
        article.setViewCount(0);
        article.setStatus(1);
        article.setPublishTime(parseTime(item.getPublish_date()));
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());

        newsMapper.insert(article);
        log.info("保存到MySQL：{} [id={}, 分类：{}]", item.getTitle(), article.getId(), article.getCategory());

        return article;
    }

    private void syncToEs(NewsArticle article) {
        try {
            NewsArticleDoc doc = new NewsArticleDoc();
            doc.setId(article.getId());
            doc.setTitle(article.getTitle());
            doc.setSummary(article.getSummary());
            doc.setSource(article.getSource());
            doc.setCategory(article.getCategory());
            doc.setTags(article.getTags());
            doc.setViewCount(article.getViewCount());
            doc.setPublishTime(Date.from(article.getPublishTime().atZone(ZoneId.systemDefault()).toInstant()));
            doc.setCreateTime(Date.from(article.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()));

            elasticsearchOperations.save(doc);
            log.info("同步到ES：{} [id={}]", article.getTitle(), article.getId());
        } catch (Exception e) {
            log.error("同步到ES失败：{}，原因：{}", article.getTitle(), e.getMessage(), e);
        }
    }

    // 改成：明确指定北京时间
    private LocalDateTime parseTime(String timeStr) {
        try {
            return LocalDateTime.parse(timeStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(java.time.ZoneId.of("Asia/Shanghai")));
        } catch (Exception e) {
            return LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        }
    }
}