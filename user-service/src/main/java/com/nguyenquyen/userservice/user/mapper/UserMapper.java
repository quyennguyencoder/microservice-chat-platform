package com.nguyenquyen.userservice.user.mapper;

import com.nguyenquyen.userservice.user.dto.UserResponse;
import com.nguyenquyen.userservice.user.dto.UserUpdateRequest;
import com.nguyenquyen.userservice.user.entity.User;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "banner", ignore = true)
    void updateFromRequest(UserUpdateRequest request, @MappingTarget User user);
}
