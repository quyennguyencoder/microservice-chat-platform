package com.nguyenquyen.searchservice.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult<T> {

    private List<T> results;

    private long totalHits;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    private boolean hasNext;
    private boolean hasPrevious;

    private long took;

    private String query;
}
