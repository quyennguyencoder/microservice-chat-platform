package com.nguyenquyen.searchservice.history;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nguyenquyen.searchservice.history.SearchHistoryService;
import com.nguyenquyen.common.util.SecurityUtils;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search/history")
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryController {

    private final SearchHistoryService historyService;

    @GetMapping
    public ResponseEntity<List<SearchHistoryResponse>> getMyHistory(
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) Integer limit
    ) {
        String userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(historyService.getUserSearchHistory(userId, limit));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearMyHistory() {
        String userId = SecurityUtils.requireCurrentUserId();
        historyService.deleteUserSearchHistory(userId);
        return ResponseEntity.ok(Map.of("message", "Search history cleared"));
    }
}
