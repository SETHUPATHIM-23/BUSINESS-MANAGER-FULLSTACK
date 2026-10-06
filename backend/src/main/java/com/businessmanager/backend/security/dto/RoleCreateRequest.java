package com.businessmanager.backend.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;

@Data
public class RoleCreateRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    private Set<Long> permissionIds;
}
