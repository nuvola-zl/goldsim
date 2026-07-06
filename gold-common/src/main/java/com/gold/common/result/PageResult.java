package com.gold.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页结果封装
 */
@Data
@Builder
@NoArgsConstructor  // 新增无参构造，解决序列化报错
@AllArgsConstructor // 全参构造配套builder
public class PageResult<T> {
    private List<T> list;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer pages;

    /**
     * 手动构建（推荐用法：先转换数据列表，再构建）
     */
    public static <T> PageResult<T> build(List<T> list, Long total, Integer page, Integer size) {
        // 防除以0异常
        if (size == null || size <= 0) {
            size = 10;
        }
        long totalPages = (total + size - 1) / size;
        int pages = totalPages > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalPages;
        return PageResult.<T>builder()
                .list(list)
                .total(total)
                .page(page)
                .size(size)
                .pages(pages)
                .build();
    }
}