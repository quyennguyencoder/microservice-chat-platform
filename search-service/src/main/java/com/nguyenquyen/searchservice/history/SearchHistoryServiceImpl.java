package com.nguyenquyen.searchservice.history;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${index.search-history.name:search_history}")
    private String historyIndex;

    @Override
    public List<SearchHistoryResponse> getUserSearchHistory(String userId, int limit) {
        try {
            co.elastic.clients.elasticsearch.core.SearchRequest request = co.elastic.clients.elasticsearch.core.SearchRequest.of(s -> s
                    .index(historyIndex)
                    .query(q -> q
                            .term(t -> t
                                    .field("userId")
                                    .value(userId)
                            )
                    )
                    .sort(so -> so
                            .field(f -> f
                                    .field("lastSearchedAt")
                                    .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                            )
                    )
                    .size(limit)
            );

            co.elastic.clients.elasticsearch.core.SearchResponse<SearchHistoryDocument> response = elasticsearchClient.search(request, SearchHistoryDocument.class);

            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(java.util.Objects::nonNull)
                    .map(doc -> SearchHistoryResponse.builder()
                            .query(doc.getQuery())
                            .searchType(doc.getSearchType())
                            .searchCount(doc.getSearchCount())
                            .lastSearchedAt(doc.getLastSearchedAt())
                            .build())
                    .toList();
        } catch (java.io.IOException e) {
            log.error("Failed to fetch search history for user: {}", userId, e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public void deleteUserSearchHistory(String userId) {
        try {
            elasticsearchClient.deleteByQuery(d -> d
                    .index(historyIndex)
                    .query(q -> q
                            .term(t -> t
                                    .field("userId")
                                    .value(userId)
                            )
                    )
            );
            log.info("Search history deleted for user: {}", userId);
        } catch (java.io.IOException e) {
            log.error("Failed to delete search history for user: {}", userId, e);
        }
    }
}