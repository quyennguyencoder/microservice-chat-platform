package com.nguyenquyen.searchservice.user;

import com.nguyenquyen.searchservice.kafka.event.UserEvent;

import java.util.Optional;

public interface UserIndexService {

    void indexUser(UserEvent event);

    void updateUser(UserEvent event);


    void deleteUser(String userId);

    Optional<UserDocument> findById(String userId);

    long count();
}
