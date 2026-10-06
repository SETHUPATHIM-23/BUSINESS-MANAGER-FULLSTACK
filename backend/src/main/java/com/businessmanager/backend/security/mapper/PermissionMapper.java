package com.businessmanager.backend.security.mapper;

import com.businessmanager.backend.security.dto.PermissionDto;
import com.businessmanager.backend.security.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionMapper {
    PermissionDto toDto(Permission permission);
    Permission toEntity(PermissionDto dto);
}
