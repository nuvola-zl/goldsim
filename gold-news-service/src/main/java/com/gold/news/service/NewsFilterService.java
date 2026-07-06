package com.gold.news.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class NewsFilterService {
    
    /** 黄金相关关键词 */
//    private static final List<String> GOLD_KEYWORDS = Arrays.asList(
//            "黄金", "金价", "现货黄金", "XAU", "au", "足金", "K金",
//            "白银", "银价", "现货白银", "XAG", "ag",
//            "铂金", "钯金", "贵金属"
//    );
    // 改成：去掉容易误触发的"au"和"ag"，保留明确的词
    private static final List<String> GOLD_KEYWORDS = Arrays.asList(
            "黄金", "金价", "现货黄金", "XAU", "足金", "K金",
            "白银", "银价", "现货白银", "XAG",
            "铂金", "钯金", "贵金属"
    );
    
    /** 宏观影响因素 */
    private static final List<String> MACRO_KEYWORDS = Arrays.asList(
            "美联储", "加息", "降息", "通胀", "CPI", "PPI",
            "避险", "美元指数", "美元", "地缘政治", "战争", "冲突",
            "央行", "货币政策", "量化宽松", "QE",
            "沪港", "上海黄金交易所", "香港黄金"  // 新增
    );
    
    /**
     * 判断是否黄金相关新闻
     * 适配 description 为空的情况，主要用 title 判断
     */
    public boolean isGoldRelated(String title, String content) {
        if (title == null) {
            log.info("过滤结果：title为null，返回false");
            return false;
        }

        String text = (title + " " + (content == null ? "" : content)).toLowerCase();
        log.info("过滤检查：原始title=[{}], 拼接后text=[{}]", title, text);

        boolean goldMatch = GOLD_KEYWORDS.stream().anyMatch(kw -> {
            boolean match = text.contains(kw.toLowerCase());
            if (match) log.info("  命中关键词：{}", kw);
            return match;
        });

        boolean macroMatch = MACRO_KEYWORDS.stream().anyMatch(kw -> {
            boolean match = text.contains(kw.toLowerCase());
            if (match) log.info("  命中关键词：{}", kw);
            return match;
        });

        boolean result = goldMatch || macroMatch;
        log.info("过滤结果：goldMatch={}, macroMatch={}, 最终={}", goldMatch, macroMatch, result);
        return result;
    }
    /**
     * 分类标签
     */
    public String classify(String title, String content) {
        String text = (title + " " + (content == null ? "" : content)).toLowerCase();
        
        if (containsAny(text, "黄金", "金价", "xau", "au", "足金", "k金")) {
            return "黄金";
        }
        if (containsAny(text, "白银", "银价", "xag", "ag")) {
            return "白银";
        }
        if (containsAny(text, "铂金")) {
            return "铂金";
        }
        if (containsAny(text, "美联储", "加息", "降息", "通胀", "美元", "央行", "沪港", "上海黄金交易所")) {
            return "宏观";
        }
        return "其他";
    }
    
    /**
     * 提取标签
     */
    public String extractTags(String title, String content) {
        String text = (title + " " + (content == null ? "" : content)).toLowerCase();
        List<String> allKeywords = Arrays.asList(
                "黄金", "白银", "铂金", "美联储", "加息", "降息",
                "通胀", "避险", "美元", "地缘政治", "央行",
                "上海黄金交易所", "香港", "沪港"
        );
        
        StringBuilder tags = new StringBuilder();
        for (String keyword : allKeywords) {
            if (text.contains(keyword)) {
                if (tags.length() > 0) tags.append(",");
                tags.append(keyword);
            }
        }
        return tags.toString();
    }
    
    private boolean containsAny(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
    }
}