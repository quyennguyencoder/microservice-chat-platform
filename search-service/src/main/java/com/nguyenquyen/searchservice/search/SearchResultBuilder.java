package com.nguyenquyen.searchservice.search;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchResultBuilder {

    public <T> SearchResult<T> build(
            List<T> results,
            long totalHits,
            int page,
            int size,
            long took,
            String query
    ) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalHits / size) : 0;

        return SearchResult.<T>builder()
                .results(results)
                .totalHits(totalHits)
                .totalPages(totalPages)
                .currentPage(page)
                .pageSize(size)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .took(took)
                .query(query)
                .build();
    }
}
