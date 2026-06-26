package com.nguyenquyen.searchservice.user;

import com.nguyenquyen.searchservice.search.SearchCriteria;
import com.nguyenquyen.searchservice.search.SearchResult;
import com.nguyenquyen.common.kafka.event.UserEvent;

import java.util.Optional;

public interface UserSearchService {

    void indexUser(UserEvent event);

    void updateUser(UserEvent event);

    void deleteUser(String userId);

    Optional<UserDocument> findById(String userId);

    long count();

    SearchResult<UserSearchResult> searchUsers(SearchCriteria criteria);

    InternalSearchResult<UserSearchResult> searchUsersInternal(SearchCriteria criteria, int from, int size);

    record InternalSearchResult<T>(java.util.List<T> results, long totalHits) {}
}
