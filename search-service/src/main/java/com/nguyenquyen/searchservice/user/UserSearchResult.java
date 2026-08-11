package com.nguyenquyen.searchservice.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResult {

    private String id;
    private String username;
    private String description;
    private String avatar;
    private String banner;
    private Instant createdAt;
    private Instant updatedAt;
}