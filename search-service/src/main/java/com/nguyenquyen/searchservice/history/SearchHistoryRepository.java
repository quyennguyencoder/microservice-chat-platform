package com.nguyenquyen.searchservice.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends ElasticsearchRepository<SearchHistoryDocument, String> {

    Page<SearchHistoryDocument> findByUserIdOrderByLastSearchedAtDesc(String userId, Pageable pageable);

    Optional<SearchHistoryDocument> findByUserIdAndNormalizedQuery(String userId, String normalizedQuery);

    void deleteByUserId(String userId);
}