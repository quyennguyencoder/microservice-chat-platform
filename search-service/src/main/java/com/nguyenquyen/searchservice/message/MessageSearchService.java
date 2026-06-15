package com.nguyenquyen.searchservice.message;

import com.nguyenquyen.searchservice.search.SearchCriteria;
import com.nguyenquyen.searchservice.search.SearchResult;

public interface MessageSearchService {

    void indexMessage(MessageDocument message);

    void deleteMessage(String messageId);

    SearchResult<MessageSearchResult> searchMessages(SearchCriteria criteria);
}
