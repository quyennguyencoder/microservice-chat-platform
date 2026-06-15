package com.nguyenquyen.searchservice.user;

import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class UserSearchMapper {

    public UserSearchResult toSearchResult(Hit<UserDocument> hit) {
        UserDocument doc = hit.source();
        if (doc == null) return null;

        Map<String, List<String>> highlights = new HashMap<>();
        if (hit.highlight() != null) {
            highlights.putAll(hit.highlight());
        }

        return UserSearchResult.builder()
                .id(doc.getId())
                .username(doc.getUsername())
                .description(doc.getDescription())
                .imageId(doc.getImageId())
                .bannerImageId(doc.getBannerImageId())

                .createdAt(doc.getCreatedAt())
                .highlights(highlights)
                .score(hit.score() != null ? hit.score().floatValue() : null)
                .build();
    }

    public List<UserSearchResult> toSearchResultList(List<Hit<UserDocument>> hits) {
        return hits.stream()
                .map(this::toSearchResult)
                .filter(Objects::nonNull)
                .toList();
    }
}