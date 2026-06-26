package com.nguyenquyen.searchservice.user;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import com.nguyenquyen.searchservice.client.UserServiceClient;
import com.nguyenquyen.searchservice.client.FriendshipResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nguyenquyen.searchservice.exception.IndexingException;
import com.nguyenquyen.searchservice.exception.SearchException;
import com.nguyenquyen.common.kafka.event.UserEvent;
import com.nguyenquyen.searchservice.search.SearchCriteria;
import com.nguyenquyen.searchservice.search.SearchResult;
import com.nguyenquyen.searchservice.search.SearchResultBuilder;
import com.nguyenquyen.common.util.SecurityUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSearchServiceImpl implements UserSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final UserSearchMapper userMapper;
    private final SearchResultBuilder resultBuilder;
    private final UserServiceClient userServiceClient;

    @Value("${index.users.name:users}")
    private String usersIndex;

    @Value("${search.highlight.pre-tag:<em>}")
    private String highlightPreTag;

    @Value("${search.highlight.post-tag:</em>}")
    private String highlightPostTag;

    @Value("${search.fuzzy.max-expansions:50}")
    private int fuzzyMaxExpansions;

    @Value("${search.fuzzy.prefix-length:2}")
    private int fuzzyPrefixLength;

    @Override
    public void indexUser(UserEvent event) {
        try {
            UserDocument document = mapToDocument(event);
            elasticsearchClient.index(i -> i
                    .index(usersIndex)
                    .id(document.getId())
                    .document(document)
            );
            log.info("User indexed: id={}, username={}", document.getId(), document.getUsername());
        } catch (Exception e) {
            log.error("Failed to index user: {}", event.getUserId(), e);
            throw new IndexingException("Failed to index user: " + event.getUserId(), e);
        }
    }

    @Override
    public void updateUser(UserEvent event) {
        try {
            String userId = event.getUserId();
            Optional<UserDocument> existingOpt = findById(userId);

            if (existingOpt.isEmpty()) {
                log.warn("User not found for update, indexing as new: {}", userId);
                indexUser(event);
                return;
            }

            UserDocument existing = existingOpt.get();
            updateDocumentFromEvent(existing, event);
            existing.setUpdatedAt(Instant.now());

            elasticsearchClient.index(i -> i
                    .index(usersIndex)
                    .id(existing.getId())
                    .document(existing)
            );
            log.info("User updated: id={}", userId);

        } catch (Exception e) {
            log.error("Failed to update user: {}", event.getUserId(), e);
            throw new IndexingException("Failed to update user: " + event.getUserId(), e);
        }
    }

    @Override
    public void deleteUser(String userId) {
        try {
            elasticsearchClient.delete(d -> d
                    .index(usersIndex)
                    .id(userId)
            );
            log.info("User deleted from index: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete user: {}", userId, e);
            throw new IndexingException("Failed to delete user: " + userId, e);
        }
    }

    @Override
    public Optional<UserDocument> findById(String userId) {
        try {
            co.elastic.clients.elasticsearch.core.GetResponse<UserDocument> response = 
                elasticsearchClient.get(g -> g.index(usersIndex).id(userId), UserDocument.class);
            if (response.found()) {
                return Optional.ofNullable(response.source());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to find user by id: {}", userId, e);
            return Optional.empty();
        }
    }

    @Override
    public long count() {
        try {
            co.elastic.clients.elasticsearch.core.CountResponse response = 
                elasticsearchClient.count(c -> c.index(usersIndex));
            return response.count();
        } catch (Exception e) {
            log.error("Failed to count users", e);
            return 0;
        }
    }

    private UserDocument mapToDocument(UserEvent event) {
        return UserDocument.builder()
                .id(event.getUserId())
                .username(event.getUsername())
                .email(event.getEmail())
                .description(event.getDescription())
                .imageId(event.getImageId())
                .bannerImageId(event.getBannerImageId())
                .createdAt(event.getTimestamp())
                .updatedAt(Instant.now())
                .build();
    }

    private void updateDocumentFromEvent(UserDocument document, UserEvent event) {
        if (event.getUsername() != null) {
            document.setUsername(event.getUsername());
        }
        if (event.getDescription() != null) {
            document.setDescription(event.getDescription());
        }
        if (event.getImageId() != null) {
            document.setImageId(event.getImageId());
        }
        if (event.getBannerImageId() != null) {
            document.setBannerImageId(event.getBannerImageId());
        }
    }

    @Override
    public SearchResult<UserSearchResult> searchUsers(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();

        try {
            InternalSearchResult<UserSearchResult> result = searchUsersInternal(
                    criteria,
                    criteria.getPage() * criteria.getSize(),
                    criteria.getSize()
            );

            long took = System.currentTimeMillis() - startTime;

            log.info("User search completed: query='{}', hits={}, took={}ms",
                    criteria.getQuery(), result.totalHits(), took);


            return resultBuilder.build(
                    result.results(),
                    result.totalHits(),
                    criteria.getPage(),
                    criteria.getSize(),
                    took,
                    criteria.getQuery()
            );

        } catch (Exception e) {
            log.error("User search failed: query='{}'", criteria.getQuery(), e);
            throw new SearchException("Search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InternalSearchResult<UserSearchResult> searchUsersInternal(
            SearchCriteria criteria, int from, int size) {

        try {
            Query query = buildUserQuery(criteria);

            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(usersIndex)
                    .query(query)
                    .from(from)
                    .size(size);

            addSorting(searchBuilder, criteria);

            if (Boolean.TRUE.equals(criteria.getHighlight())) {
                addHighlighting(searchBuilder);
            }

            SearchResponse<UserDocument> response = elasticsearchClient.search(
                    searchBuilder.build(),
                    UserDocument.class
            );

            List<UserSearchResult> results = userMapper.toSearchResultList(response.hits().hits());

            long totalHits = response.hits().total() != null
                    ? response.hits().total().value()
                    : 0;

            return new InternalSearchResult<>(results, totalHits);

        } catch (IOException e) {
            throw new SearchException("Failed to search users", e);
        }
    }

    private Query buildUserQuery(SearchCriteria criteria) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
            Query textQuery;
            if (Boolean.TRUE.equals(criteria.getFuzzy())) {
                textQuery = MultiMatchQuery.of(m -> m
                        .query(criteria.getQuery())
                        .fields("username^3", "description^1")
                        .fuzziness("AUTO")
                        .prefixLength(fuzzyPrefixLength)
                        .maxExpansions(fuzzyMaxExpansions)
                        .type(TextQueryType.BestFields)
                )._toQuery();
            } else {
                textQuery = MultiMatchQuery.of(m -> m
                        .query(criteria.getQuery())
                        .fields("username^3", "description^1")
                        .type(TextQueryType.BestFields)
                )._toQuery();
            }
            boolBuilder.must(textQuery);
        }

        if (Boolean.TRUE.equals(criteria.getFriendsOnly())) {
            List<FriendshipResponse> friends = userServiceClient.getFriends();
            if (friends.isEmpty()) {
                // If user has no friends, return no results
                boolBuilder.filter(f -> f.term(t -> t.field("_id").value("no_friends_match")));
            } else {
                List<co.elastic.clients.elasticsearch._types.FieldValue> friendIds = friends.stream()
                        .map(friend -> co.elastic.clients.elasticsearch._types.FieldValue.of(friend.userId()))
                        .toList();
                
                boolBuilder.filter(f -> f.terms(t -> t
                        .field("_id")
                        .terms(tv -> tv.value(friendIds))));
            }
        }

        return boolBuilder.build()._toQuery();
    }

    private void addSorting(SearchRequest.Builder builder, SearchCriteria criteria) {
        switch (criteria.getSortBy()) {
            case RECENT -> builder.sort(s -> s.field(f -> f
                    .field("createdAt")
                    .order(SortOrder.Desc)));
            default -> builder.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }
    }

    private void addHighlighting(SearchRequest.Builder builder) {
        builder.highlight(h -> h
                .preTags(highlightPreTag)
                .postTags(highlightPostTag)
                .fields("username", HighlightField.of(hf -> hf.numberOfFragments(0)))
                .fields("description", HighlightField.of(hf -> hf
                        .numberOfFragments(2)
                        .fragmentSize(150)))
        );
    }
}
