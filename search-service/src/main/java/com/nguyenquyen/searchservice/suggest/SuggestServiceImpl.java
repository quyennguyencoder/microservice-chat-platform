package com.nguyenquyen.searchservice.suggest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nguyenquyen.searchservice.user.UserDocument;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestServiceImpl implements SuggestService {

    private final ElasticsearchClient elasticsearchClient;


    @Value("${index.users.name:users}")
    private String usersIndex;

    @Override
    public SuggestResponse suggest(String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            return SuggestResponse.builder()
                    .suggestions(Collections.emptyList())
                    .build();
        }

        try {
            Set<String> seen = new HashSet<>();
            List<SuggestResponse.Suggestion> suggestions = new ArrayList<>();


            addUserSuggestions(query, suggestions, seen, limit);

            suggestions.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

            return SuggestResponse.builder()
                    .suggestions(suggestions.stream().limit(limit).toList())
                    .build();

        } catch (IOException e) {
            log.error("Suggest failed: query='{}'", query, e);
            return SuggestResponse.builder()
                    .suggestions(Collections.emptyList())
                    .build();
        }
    }


    private void addUserSuggestions(String query, List<SuggestResponse.Suggestion> suggestions,
                                    Set<String> seen, int limit) throws IOException {
        Query prefixQuery = PrefixQuery.of(p -> p
                .field("username")
                .value(query.toLowerCase())
        )._toQuery();

        SearchResponse<UserDocument> response = elasticsearchClient.search(
                s -> s.index(usersIndex)
                        .query(prefixQuery)
                        .size(limit / 2)
                        .source(src -> src.filter(f -> f
                                .includes("username", "imageId"))),
                UserDocument.class
        );

        for (Hit<UserDocument> hit : response.hits().hits()) {
            UserDocument doc = hit.source();
            if (doc == null) continue;

            if (doc.getUsername() != null &&
                    seen.add("user:" + doc.getUsername().toLowerCase())) {

                suggestions.add(SuggestResponse.Suggestion.builder()
                        .text(doc.getUsername())
                        .type(SuggestResponse.SuggestionType.USERNAME)
                        .imageId(doc.getImageId())
                        .score(hit.score() != null ? hit.score().floatValue() * 0.7f : 0f)
                        .build());
            }
        }
    }
}

