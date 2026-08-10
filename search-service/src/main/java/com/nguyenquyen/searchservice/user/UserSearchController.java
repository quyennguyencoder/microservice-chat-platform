package com.nguyenquyen.searchservice.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nguyenquyen.common.dto.response.ApiResponse;
import com.nguyenquyen.searchservice.search.SearchCriteria;
import com.nguyenquyen.searchservice.search.SearchResult;

@RestController
@RequestMapping("/api/v1/search/users")
@RequiredArgsConstructor
@Slf4j
public class UserSearchController {

    private final UserSearchService userSearchService;

    @GetMapping
    public ResponseEntity<ApiResponse<SearchResult<UserSearchResult>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "RELEVANCE") SearchCriteria.SortBy sortBy,
            @RequestParam(defaultValue = "DESC") SearchCriteria.SortOrder sortOrder,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "true") Boolean fuzzy,
            @RequestParam(required = false) Boolean friendsOnly
    ) {
        SearchCriteria criteria = SearchCriteria.builder()
                .query(q)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .page(page)
                .size(size)
                .fuzzy(fuzzy)
                .friendsOnly(friendsOnly)
                .build();

        log.debug("User search: query='{}', page={}, size={}", q, page, size);

        return ResponseEntity.ok(ApiResponse.success(userSearchService.searchUsers(criteria)));
    }
}
