package com.nguyenquyen.searchservice.user;

import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class UserSearchMapper {

    public UserSearchResult toSearchResult(Hit<UserDocument> hit) {
        UserDocument doc = hit.source();
        if (doc == null) return null;



        return UserSearchResult.builder()
                .id(doc.getId())
                .username(doc.getUsername())
                .description(doc.getDescription())
                .avatar(doc.getAvatar())
                .banner(doc.getBanner())

                .createdAt(doc.getCreatedAt() != null ? Instant.ofEpochMilli(doc.getCreatedAt()) : null)
                .updatedAt(doc.getUpdatedAt() != null ? Instant.ofEpochMilli(doc.getUpdatedAt()) : null)
                .build();
    }

    public List<UserSearchResult> toSearchResultList(List<Hit<UserDocument>> hits) {
        return hits.stream()
                .map(this::toSearchResult)
                .filter(Objects::nonNull)
                .toList();
    }
}