package com.nguyenquyen.searchservice.user;

import com.nguyenquyen.searchservice.search.SearchCriteria;
import com.nguyenquyen.searchservice.search.SearchResult;

public interface UserSearchService {

    SearchResult<UserSearchResult> searchUsers(SearchCriteria criteria);

    InternalSearchResult<UserSearchResult> searchUsersInternal(SearchCriteria criteria, int from, int size);

    record InternalSearchResult<T>(java.util.List<T> results, long totalHits) {}
}
