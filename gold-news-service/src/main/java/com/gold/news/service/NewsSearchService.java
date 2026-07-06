package com.gold.news.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.json.JsonData;
import com.gold.news.doc.NewsArticleDoc;
import com.gold.news.domain.po.NewsArticle;
import com.gold.news.domain.vo.AiBriefVO;
import com.gold.news.mapper.NewsMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final NewsMapper newsMapper;

    @Data
    @AllArgsConstructor
    public static class PageData<T> {
        private List<T> list;
        private long total;
    }


    /**
     * 增强搜索：支持关键词 + 时间范围 + 分类 + 排序
     *
     * @param keyword    关键词
     * @param category   分类：黄金/白银/铂金/宏观/其他，null表示不限
     * @param days       最近N天，null表示不限
     * @param sortBy     排序：time(时间)/relevance(相关度)/views(浏览量)，默认time
     * @param page       页码
     * @param size       每页条数
     */
    public PageData<NewsArticleDoc> searchPage(String keyword, String category, Integer days, String sortBy, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        // 先尝试 ES
        PageData<NewsArticleDoc> esResult = searchPageFromEs(keyword, category, days, sortBy, page, size);
        if (!esResult.getList().isEmpty()) {
            return esResult;
        }

        // ES 无结果或异常，降级 MySQL
        log.warn("ES无结果，降级到MySQL搜索：keyword={}", keyword);
        return searchPageFromMySQL(keyword, category, days, sortBy, page, size);
    }


    private PageData<NewsArticleDoc> searchPageFromEs(String keyword, String category, Integer days, String sortBy, int page, int size) {
        try {
            // 1. 构建 BoolQuery
            var boolQuery = co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.of(b -> {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    b.must(QueryBuilders.multiMatch(q -> q
                            .query(keyword.trim())
                            .fields("title^3", "summary", "tags")
                            .type(TextQueryType.BestFields)
                    ));
                }
                if (category != null && !category.isEmpty()) {
                    b.filter(QueryBuilders.term(q -> q.field("category").value(category)));
                }
                if (days != null && days > 0) {
                    String startDateStr = LocalDateTime.now().minusDays(days).toString();
                    b.filter(QueryBuilders.range(q -> q
                            .field("publishTime")
                            .gte(JsonData.fromJson(startDateStr))
                    ));
                }
                return b;
            });

            // 2. 高亮
            var highlightParams = HighlightParameters.builder()
                    .withPreTags("<em style='color:red;font-weight:bold'>")
                    .withPostTags("</em>")
                    .build();
            var highlight = new Highlight(highlightParams,
                    List.of(new HighlightField("title"), new HighlightField("summary")));

            // 3. 排序
            org.springframework.data.domain.Sort sort;
            if ("views".equals(sortBy)) {
                sort = org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("viewCount"));
            } else if ("relevance".equals(sortBy)) {
                sort = org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("_score"));
            } else {
                sort = org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("publishTime"));
            }

            // 4. 组装 Query
            NativeQuery query = NativeQuery.builder()
                    .withQuery(boolQuery._toQuery())
                    .withPageable(PageRequest.of(page - 1, size, sort))
                    .withHighlightQuery(new HighlightQuery(highlight, NewsArticleDoc.class))
                    .build();

            // 5. 执行搜索
            SearchHits<NewsArticleDoc> searchHits = elasticsearchOperations.search(query, NewsArticleDoc.class);
            long total = searchHits.getTotalHits();

            // 6. 处理高亮
            List<NewsArticleDoc> results = new ArrayList<>();
            for (SearchHit<NewsArticleDoc> hit : searchHits) {
                NewsArticleDoc doc = hit.getContent();

                if (hit.getHighlightFields().containsKey("title")) {
                    List<String> hl = hit.getHighlightFields().get("title");
                    if (!hl.isEmpty()) doc.setTitle(hl.get(0));
                }
                if (hit.getHighlightFields().containsKey("summary")) {
                    List<String> hl = hit.getHighlightFields().get("summary");
                    if (!hl.isEmpty()) {
                        doc.setSummary(hl.stream().limit(3).collect(Collectors.joining("...")));
                    }
                }
                results.add(doc);
            }

            log.info("ES搜索 [{}] 分类[{}] 最近{}天 排序{} 命中 {} 条，共 {} 条",
                    keyword, category, days, sortBy, results.size(), total);
            return new PageData<>(results, total);

        } catch (Exception e) {
            log.error("ES搜索失败，将降级到MySQL：{}", e.getMessage());
            return new PageData<>(new ArrayList<>(), 0L);
        }
    }

    private PageData<NewsArticleDoc> searchPageFromMySQL(String keyword, String category, Integer days, String sortBy, int page, int size) {
        try {
            var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NewsArticle>()
                    .eq(NewsArticle::getStatus, 1);

            // 条件组装
            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.and(w -> w.like(NewsArticle::getTitle, keyword)
                        .or().like(NewsArticle::getSummary, keyword)
                        .or().like(NewsArticle::getTags, keyword));
            }
            if (category != null && !category.isEmpty()) {
                wrapper.eq(NewsArticle::getCategory, category);
            }
            if (days != null && days > 0) {
                wrapper.ge(NewsArticle::getPublishTime, LocalDateTime.now().minusDays(days));
            }

            // 1. 先查总数（此时 wrapper 还没有排序和 LIMIT）
            long total = newsMapper.selectCount(wrapper);

            // 2. 加排序
            if ("views".equals(sortBy)) {
                wrapper.orderByDesc(NewsArticle::getViewCount);
            } else {
                wrapper.orderByDesc(NewsArticle::getPublishTime);
            }

            // 3. 分页查列表
            int offset = (page - 1) * size;
            wrapper.last("LIMIT " + offset + ", " + size);
            List<NewsArticle> articles = newsMapper.selectList(wrapper);

            // 4. PO -> Doc
            List<NewsArticleDoc> results = new ArrayList<>();
            for (NewsArticle article : articles) {
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
                results.add(doc);
            }

            log.info("MySQL降级搜索 [{}] 分类[{}] 最近{}天 排序{} 命中 {} 条，共 {} 条",
                    keyword, category, days, sortBy, results.size(), total);
            return new PageData<>(results, total);

        } catch (Exception e) {
            log.error("MySQL降级搜索失败：{}", e.getMessage(), e);
            return new PageData<>(new ArrayList<>(), 0L);
        }
    }

    // 保留原来的分类搜索（兼容）
    @Deprecated
    public List<NewsArticleDoc> search(String keyword, String category, Integer days, String sortBy, int page, int size) {
        return searchPage(keyword, category, days, sortBy, page, size).getList();
    }

    public List<NewsArticleDoc> searchByCategory(String category, int page, int size) {
        return searchPage(null, category, null, "time", page, size).getList();
    }


    public AiBriefVO getAiBrief() {
        // 取最近 24 小时、已发布的新闻，按浏览量排序取前 5 条
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NewsArticle>()
                .eq(NewsArticle::getStatus, 1)
                .ge(NewsArticle::getPublishTime, LocalDateTime.now().minusDays(1))
                .orderByDesc(NewsArticle::getViewCount)
                .last("LIMIT 5");

        List<NewsArticle> articles = newsMapper.selectList(wrapper);

        if (articles.isEmpty()) {
            // 兜底：取最近 5 条，不管时间
            wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NewsArticle>()
                    .eq(NewsArticle::getStatus, 1)
                    .orderByDesc(NewsArticle::getPublishTime)
                    .last("LIMIT 5");
            articles = newsMapper.selectList(wrapper);
        }

        List<AiBriefVO.BriefItem> items = articles.stream().map(article ->
                AiBriefVO.BriefItem.builder()
                        .title(article.getTitle())
                        .summary(article.getSummary())
                        .source(article.getSource())
                        .category(article.getCategory())
                        .url(article.getSourceUrl())
                        .build()
        ).collect(Collectors.toList());

        return AiBriefVO.builder()
                .date(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .items(items)
                .build();
    }


}