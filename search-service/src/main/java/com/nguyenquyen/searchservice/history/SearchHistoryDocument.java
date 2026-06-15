package com.nguyenquyen.searchservice.history;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;

@Document(indexName = "#{@environment.getProperty('index.search-history.name', 'search_history')}")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Text)
    private String query;

    @Field(type = FieldType.Keyword)
    private String normalizedQuery;

    @Field(type = FieldType.Keyword)
    private String searchType;

    @Field(type = FieldType.Integer)
    @Builder.Default
    private Integer searchCount = 1;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant lastSearchedAt;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant createdAt;
}