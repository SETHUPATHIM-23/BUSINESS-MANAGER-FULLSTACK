package com.businessmanager.backend.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;

@Data
public class RoleUpdateRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    private Set<Long> permissionIds;
}
