package com.nguyenquyen.searchservice.history;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryRepository historyRepository;

    @Override
    public List<SearchHistoryResponse> getUserSearchHistory(String userId, int limit) {
        return historyRepository.findByUserIdOrderByLastSearchedAtDesc(
                        userId, PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(doc -> SearchHistoryResponse.builder()
                        .query(doc.getQuery())
                        .searchType(doc.getSearchType())
                        .searchCount(doc.getSearchCount())
                        .lastSearchedAt(doc.getLastSearchedAt())
                        .build())
                .toList();
    }

    @Override
    public void deleteUserSearchHistory(String userId) {
        historyRepository.deleteByUserId(userId);
        log.info("Search history deleted for user: {}", userId);
    }
}