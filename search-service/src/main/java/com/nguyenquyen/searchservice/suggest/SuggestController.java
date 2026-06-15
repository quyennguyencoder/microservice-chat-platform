package com.nguyenquyen.searchservice.suggest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search/suggest")
@RequiredArgsConstructor
@Slf4j
public class SuggestController {

    private final SuggestService suggestService;

    @GetMapping
    public ResponseEntity<SuggestResponse> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) Integer limit
    ) {
        log.debug("Suggest request: query='{}', limit={}", q, limit);
        return ResponseEntity.ok(suggestService.suggest(q, limit));
    }
}