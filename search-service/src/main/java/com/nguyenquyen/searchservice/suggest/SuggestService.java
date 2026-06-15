package com.nguyenquyen.searchservice.suggest;

public interface SuggestService {

    SuggestResponse suggest(String query, int limit);
}