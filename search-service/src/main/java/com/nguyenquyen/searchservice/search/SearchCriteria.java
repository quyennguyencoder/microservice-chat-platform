package com.nguyenquyen.searchservice.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {

    @Size(min = 1, max = 200, message = "Query must be between 1 and 200 characters")
    private String query;

    private SearchType type;

    // Specific to user search
    private Boolean friendsOnly;

    // Specific to message search
    private UUID chatId;

    private Instant createdFrom;

    private Instant createdTo;

    @Builder.Default
    private SortBy sortBy = SortBy.RELEVANCE;

    @Builder.Default
    private SortOrder sortOrder = SortOrder.DESC;

    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private Boolean fuzzy = true;

    @Builder.Default
    private Boolean highlight = true;

    public enum SearchType {
        USERS, MESSAGES
    }

    public enum SortBy {
        RELEVANCE, RECENT
    }

    public enum SortOrder {
        ASC, DESC
    }
}
