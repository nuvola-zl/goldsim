package com.gold.news.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;

@Slf4j
@Service
public class ContentFetchService {

    /**
     * 根据URL抓取文章正文
     * 支持：澎湃新闻、新浪财经、东方财富等
     */
    public String fetchContent(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        try {
            // 修复：补全 User-Agent，避免被反爬拦截
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .followRedirects(true)
                    .get();

            String content = extractContent(doc, url);

            if (content != null && !content.isEmpty()) {
                content = content.replaceAll("\s+", " ").trim();
                if (content.length() > 2000) {
                    content = content.substring(0, 2000) + "...";
                }
                return content;
            }

        } catch (SocketTimeoutException e) {
            log.warn("抓取正文超时：{}", url);
        } catch (IOException e) {
            log.warn("抓取正文失败：{}，原因：{}", url, e.getMessage());
        } catch (Exception e) {
            log.error("抓取正文异常：{}", url, e);
        }

        return "";
    }

    private String extractContent(Document doc, String url) {
        String domain = getDomain(url);

        switch (domain) {
            case "thepaper.cn":
                return extractThePaper(doc);
            case "sina.com.cn":
            case "sina.cn":
                return extractSina(doc);
            case "eastmoney.com":
                return extractEastMoney(doc);
            default:
                return extractGeneric(doc);
        }
    }

    private String extractThePaper(Document doc) {
        String[] selectors = {
                ".news_txt",
                "#newsContent",
                ".news_txt p",
                "article .txt",
                ".content_main .news_txt"
        };

        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                if (selector.endsWith("p")) {
                    StringBuilder sb = new StringBuilder();
                    for (Element p : elements) {
                        sb.append(p.text()).append("\n");
                    }
                    return sb.toString().trim();
                } else {
                    return elements.text().trim();
                }
            }
        }

        return extractGeneric(doc);
    }

    private String extractSina(Document doc) {
        Element content = doc.selectFirst("#artibody");
        if (content != null) {
            return content.text().trim();
        }
        return extractGeneric(doc);
    }

    private String extractEastMoney(Document doc) {
        Element content = doc.selectFirst(".newsContent");
        if (content != null) {
            return content.text().trim();
        }
        return extractGeneric(doc);
    }

    private String extractGeneric(Document doc) {
        String bestContent = "";
        int maxLength = 0;

        for (Element element : doc.select("div, article")) {
            String text = element.text().trim();
            if (text.length() > maxLength && text.length() > 200) {
                maxLength = text.length();
                bestContent = text;
            }
        }

        return bestContent;
    }

    private String getDomain(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            if (host != null) {
                return host.replace("www.", "");
            }
        } catch (Exception e) {
            log.warn("解析域名失败：{}", url);
        }
        return "";
    }
}