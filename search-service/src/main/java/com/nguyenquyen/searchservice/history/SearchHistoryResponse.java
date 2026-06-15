package com.nguyenquyen.searchservice.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryResponse {
    private String query;
    private String searchType;
    private Integer searchCount;
    private Instant lastSearchedAt;
}