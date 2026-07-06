package com.gold.news.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gold.common.result.PageResult;
import com.gold.common.result.Result;
import com.gold.news.doc.NewsArticleDoc;
import com.gold.news.domain.po.NewsArticle;
import com.gold.news.domain.vo.AiBriefVO;
import com.gold.news.mapper.NewsMapper;
import com.gold.news.service.NewsSearchService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

//    ┌─────────────────────────────────────────────────┐
//            │  🔍 搜索框（输入关键词）  [搜索按钮]              │
//            ├─────────────────────────────────────────────────┤
//            │  分类：全部 | 黄金 | 白银 | 铂金 | 宏观          │
//            │  时间：全部 | 今天 | 7天内 | 30天内              │
//            │  排序：最新 | 最热 | 相关度                      │
//            ├─────────────────────────────────────────────────┤
//            │  📰 文章列表（默认展示最新10条）                  │
//            │  ┌─────────────────────────────────────────┐     │
//            │  │ 标题：央行连续20个月增持黄金              │     │
//            │  │ 摘要：中国黄金储备连续20个月增长...        │     │
//            │  │ 来源：澎湃新闻 | 分类：黄金 | 2026-07-08   │     │
//            │  │ [查看详情]                               │     │
//            │  └─────────────────────────────────────────┘     │
//            │  ┌─────────────────────────────────────────┐     │
//            │  │ 标题：老铺黄金，用产品价值重新定义奢侈品标准 │     │
//            │  │ ...                                     │     │
//            │  └─────────────────────────────────────────┘     │
//            ├─────────────────────────────────────────────────┤
//            │  < 1  2  3  ...  10 >  共100条                  │
//            └─────────────────────────────────────────────────┘
//    对应后端接口
//    前端调用你的 /news/search 接口：
//    表格
//    场景	请求
//    首次进入页面	GET /news/search?page=1&size=10（无参数，展示全部最新）
//    输入关键词搜索	GET /news/search?keyword=黄金&page=1&size=10
//    选分类"黄金"	GET /news/search?category=黄金&page=1&size=10
//    选"7天内"	GET /news/search?days=7&page=1&size=10
//    选"最热"排序	GET /news/search?sortBy=views&page=1&size=10
//    组合条件	GET /news/search?keyword=黄金&category=宏观&days=7&sortBy=views&page=1&size=10

    private final NewsMapper newsMapper;
    private final NewsSearchService newsSearchService;


    // 改成：返回完整的分页信息
    @GetMapping("/list")
    public Result<PageResult<NewsArticle>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category) {

        // 1. 构建查询条件
        var wrapper = new LambdaQueryWrapper<NewsArticle>()
                .eq(NewsArticle::getStatus, 1)
                .orderByDesc(NewsArticle::getPublishTime);

        if (category != null && !category.isEmpty()) {
            wrapper.eq(NewsArticle::getCategory, category);
        }

        // 2. MyBatis-Plus 分页查询
        var mpPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<NewsArticle>(page, size);
        newsMapper.selectPage(mpPage, wrapper);

        // 3. 转换为 PageResult（关键：这里解耦）
        PageResult<NewsArticle> result = PageResult.build(
                mpPage.getRecords(),   // 当前页数据
                mpPage.getTotal(),     // 总条数
                (int) mpPage.getCurrent(), // 当前页码
                (int) mpPage.getSize()     // 每页大小
        );

        return Result.success(result);
    }
    @GetMapping("/search")
    public Result<PageResult<NewsArticleDoc>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer days,
            @RequestParam(defaultValue = "time") String sortBy,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        var searchResult = newsSearchService.searchPage(keyword, category, days, sortBy, page, size);

        PageResult<NewsArticleDoc> result = PageResult.build(
                searchResult.getList(),
                searchResult.getTotal(),
                page,
                size
        );

        return Result.success(result);
    }

    @GetMapping("/hot")
    public Result<List<NewsArticle>> hot(
            @RequestParam(defaultValue = "7") Integer days) {

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NewsArticle>()
                .eq(NewsArticle::getStatus, 1)
                .ge(NewsArticle::getPublishTime, LocalDateTime.now().minusDays(days))
                .orderByDesc(NewsArticle::getViewCount)
                .last("LIMIT 10");

        return Result.success(newsMapper.selectList(wrapper));
    }

    @GetMapping("/{id}")
    public Result<NewsArticle> detail(@PathVariable Long id) {
        NewsArticle article = newsMapper.selectById(id);
        if (article == null) {
            return Result.error("A04004", "新闻不存在");
        }

        // ✅ 修复：使用原子更新，避免并发丢失
        newsMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<NewsArticle>()
                .eq(NewsArticle::getId, id)
                .setSql("view_count = view_count + 1"));

        // 返回时手动+1（因为数据库已经更新了，但当前对象还是旧值）
        article.setViewCount(article.getViewCount() + 1);

        return Result.success(article);
    }

    @GetMapping("/relate/{metalType}")
    public Result<List<NewsArticle>> relate(@PathVariable String metalType) {
        String category = switch (metalType.toUpperCase()) {
            case "AU", "GOLD" -> "黄金";
            case "AG", "SILVER" -> "白银";
            case "PT", "PLATINUM" -> "铂金";
            default -> "宏观";
        };

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NewsArticle>()
                .eq(NewsArticle::getStatus, 1)
                .and(w -> w.eq(NewsArticle::getCategory, category)
                        .or()
                        .like(NewsArticle::getTags, metalType))
                .orderByDesc(NewsArticle::getPublishTime)
                .last("LIMIT 5");

        return Result.success(newsMapper.selectList(wrapper));
    }

    @GetMapping("/tags")
    public Result<List<String>> tags() {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NewsArticle>()
                .eq(NewsArticle::getStatus, 1)
                .orderByDesc(NewsArticle::getPublishTime)
                .last("LIMIT 100");

        List<NewsArticle> articles = newsMapper.selectList(wrapper);

        java.util.Map<String, Integer> tagCount = new java.util.HashMap<>();
        for (NewsArticle article : articles) {
            if (article.getTags() != null) {
                for (String tag : article.getTags().split(",")) {
                    if (!tag.trim().isEmpty()) {
                        tagCount.merge(tag.trim(), 1, Integer::sum);
                    }
                }
            }
        }

        List<String> hotTags = tagCount.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(java.util.Map.Entry::getKey)
                .toList();

        return Result.success(hotTags);
    }

    /**
     * 获取新闻分类列表
     * 接口：GET /api/news/categories
     */
    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        return Result.success(Arrays.asList("黄金", "白银", "铂金", "宏观", "其他"));
    }

    /**
     * 获取 AI 每日简报
     * 接口：GET /api/news/ai-brief
     * 从数据库取最近热门新闻，拼成简报格式
     */
    @GetMapping("/ai-brief")
    public Result<AiBriefVO> getAiBrief() {
        return Result.success(newsSearchService.getAiBrief());
    }
}