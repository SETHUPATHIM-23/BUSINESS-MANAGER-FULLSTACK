package com.businessmanager.backend.security.dto;

import lombok.Data;

@Data
public class PermissionDto {
    private Long id;
    private String code;
    private String description;
}
