package com.nguyenquyen.searchservice.message;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nguyenquyen.searchservice.exception.IndexingException;
import com.nguyenquyen.searchservice.exception.SearchException;
import com.nguyenquyen.searchservice.search.SearchCriteria;
import com.nguyenquyen.searchservice.search.SearchResult;
import com.nguyenquyen.searchservice.search.SearchResultBuilder;

import com.nguyenquyen.searchservice.client.ChatServiceClient;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageSearchServiceImpl implements MessageSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final MessageSearchMapper messageMapper;
    private final SearchResultBuilder resultBuilder;
    private final ChatServiceClient chatServiceClient;

    @Value("${index.messages.name:messages}")
    private String messagesIndex;

    @Override
    public void indexMessage(MessageDocument message) {
        try {
            elasticsearchClient.index(i -> i
                    .index(messagesIndex)
                    .id(message.getMessageId())
                    .document(message)
            );
            log.debug("Indexed message: {}", message.getMessageId());
        } catch (IOException e) {
            throw new IndexingException("Failed to index message: " + message.getMessageId(), e);
        }
    }

    @Override
    public void deleteMessage(String messageId) {
        try {
            elasticsearchClient.delete(d -> d
                    .index(messagesIndex)
                    .id(messageId)
            );
            log.debug("Deleted message: {}", messageId);
        } catch (IOException e) {
            throw new IndexingException("Failed to delete message: " + messageId, e);
        }
    }

    @Override
    public SearchResult<MessageSearchResult> searchMessages(SearchCriteria criteria) {
        List<java.util.UUID> userChatIds = null;

        if (criteria.getChatId() == null) {
            // Global message search: fetch all chat IDs the user is part of
            try {
                userChatIds = chatServiceClient.getMyChats().stream()
                        .map(com.nguyenquyen.searchservice.client.ChatResponse::id)
                        .toList();
                
                if (userChatIds.isEmpty()) {
                    return resultBuilder.build(
                            java.util.Collections.emptyList(),
                            0,
                            criteria.getPage(),
                            criteria.getSize(),
                            0,
                            criteria.getQuery()
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to fetch user chats for global search: {}", e.getMessage());
                throw new SearchException("Failed to fetch user chats", e);
            }
        } else {
            // Verify membership for specific chat
            try {
                chatServiceClient.getChatById(criteria.getChatId());
            } catch (feign.FeignException e) {
                log.warn("Access denied or chat not found for chat {}: {}", criteria.getChatId(), e.getMessage());
                throw new SearchException("Access denied or chat not found", e);
            }
        }

        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

            if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
                boolBuilder.must(MatchQuery.of(m -> m
                        .field("content")
                        .query(criteria.getQuery())
                        .fuzziness(criteria.getFuzzy() ? "AUTO" : "0")
                )._toQuery());
            }

            if (criteria.getChatId() != null) {
                boolBuilder.filter(TermQuery.of(t -> t
                        .field("chatId.keyword")
                        .value(criteria.getChatId().toString())
                )._toQuery());
            } else if (userChatIds != null && !userChatIds.isEmpty()) {
                List<co.elastic.clients.elasticsearch._types.FieldValue> chatIdsValues = userChatIds.stream()
                        .map(id -> co.elastic.clients.elasticsearch._types.FieldValue.of(id.toString()))
                        .toList();
                
                boolBuilder.filter(co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery.of(t -> t
                        .field("chatId.keyword")
                        .terms(v -> v.value(chatIdsValues))
                )._toQuery());
            }

            Query query = boolBuilder.build()._toQuery();

            SearchRequest request = SearchRequest.of(s -> s
                    .index(messagesIndex)
                    .query(query)
                    .from(criteria.getPage() * criteria.getSize())
                    .size(criteria.getSize())
                    .highlight(h -> h
                            .fields("content", f -> f
                                    .preTags("<em>")
                                    .postTags("</em>")
                            )
                    )
            );

            long startTime = System.currentTimeMillis();
            SearchResponse<MessageDocument> response = elasticsearchClient.search(request, MessageDocument.class);
            long took = System.currentTimeMillis() - startTime;

            List<MessageSearchResult> results = response.hits().hits().stream()
                    .map(this::mapHitToSearchResult)
                    .toList();

            long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;

            return resultBuilder.build(
                    results,
                    totalHits,
                    criteria.getPage(),
                    criteria.getSize(),
                    took,
                    criteria.getQuery()
            );

        } catch (IOException e) {
            throw new SearchException("Failed to execute message search", e);
        }
    }

    private MessageSearchResult mapHitToSearchResult(Hit<MessageDocument> hit) {
        MessageDocument doc = hit.source();
        if (doc == null) {
            return null;
        }

        MessageSearchResult result = messageMapper.toSearchResult(doc);

        if (hit.highlight() != null && hit.highlight().containsKey("content")) {
            result.setHighlightedContent(String.join("... ", hit.highlight().get("content")));
        } else {
            result.setHighlightedContent(doc.getContent());
        }

        return result;
    }
}
