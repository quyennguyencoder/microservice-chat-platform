package com.nguyenquyen.searchservice.message;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nguyenquyen.searchservice.search.SearchCriteria;
import com.nguyenquyen.searchservice.search.SearchResult;

@RestController
@RequestMapping("/api/v1/search/messages")
@RequiredArgsConstructor
public class MessageSearchController {

    private final MessageSearchService messageSearchService;

    @GetMapping
    public ResponseEntity<SearchResult<MessageSearchResult>> searchMessages(
            @RequestParam String q,
            @RequestParam(required = false) java.util.UUID chatId,
            @RequestParam(defaultValue = "RELEVANCE") SearchCriteria.SortBy sortBy,
            @RequestParam(defaultValue = "DESC") SearchCriteria.SortOrder sortOrder,
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) Integer page,
            @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) Integer size,
            @RequestParam(defaultValue = "true") Boolean fuzzy,
            @RequestParam(defaultValue = "true") Boolean highlight
    ) {
        SearchCriteria criteria = SearchCriteria.builder()
                .query(q)
                .chatId(chatId)
                .type(SearchCriteria.SearchType.MESSAGES)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .page(page)
                .size(size)
                .fuzzy(fuzzy)
                .highlight(highlight)
                .build();

        return ResponseEntity.ok(messageSearchService.searchMessages(criteria));
    }
}
