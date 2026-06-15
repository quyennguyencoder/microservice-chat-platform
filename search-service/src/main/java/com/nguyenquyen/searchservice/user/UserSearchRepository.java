package com.nguyenquyen.searchservice.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSearchRepository extends ElasticsearchRepository<UserDocument, String> {

    @Query("""
        {
          "bool": {
            "should": [
              { "match": { "username": { "query": "?0", "boost": 3 } } },
              { "match": { "description": { "query": "?0", "boost": 1 } } }
            ],
            "minimum_should_match": 1
          }
        }
        """)
    Page<UserDocument> searchByQuery(String query, Pageable pageable);

    Page<UserDocument> findByUsernameContaining(String username, Pageable pageable);
}