package com.gold.news.repository;

import com.gold.news.doc.NewsArticleDoc;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsEsRepository extends ElasticsearchRepository<NewsArticleDoc, Long> {
    
    List<NewsArticleDoc> findByTitleContaining(String keyword);
    
    List<NewsArticleDoc> findByCategory(String category);
}