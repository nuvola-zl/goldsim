package com.gold.news.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Document(indexName = "news_article")
@Setting(settingPath = "news-setting.json")
public class NewsArticleDoc {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String summary;

    @Field(type = FieldType.Keyword)
    private String source;

    @Field(type = FieldType.Keyword)
    private String category;

    // 改成
    @Field(type = FieldType.Text)
    private String tags;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Date publishTime;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Date createTime;
}