package com.gold.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.gold.news.domain.po.NewsArticle;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NewsMapper extends BaseMapper<NewsArticle> {
}