package com.businessmanager.backend.security.mapper;

import com.businessmanager.backend.security.dto.UserDto;
import com.businessmanager.backend.security.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {RoleMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserDto toDto(User user);
    User toEntity(UserDto dto);
}
