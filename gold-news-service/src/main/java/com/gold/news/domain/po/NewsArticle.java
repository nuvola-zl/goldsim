package com.gold.news.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("news_article")
public class NewsArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 摘要/正文（Jsoup抓取或降级用标题） */
    private String summary;

    /** 来源：澎湃新闻、新浪财经等 */
    private String source;

    /** 原文链接 */
    private String sourceUrl;

    /** 分类：黄金、白银、铂金、宏观、其他 */
    private String category;

    /** 标签，逗号分隔，如：黄金,美联储,加息 */
    private String tags;

    /** 浏览量 */
    private Integer viewCount;

    /** 状态：0-草稿 1-已发布 2-下线 */
    private Integer status;

    /** 原文发布时间 */
    private LocalDateTime publishTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}