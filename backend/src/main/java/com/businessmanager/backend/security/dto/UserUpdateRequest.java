package com.businessmanager.backend.security.dto;

import com.businessmanager.backend.security.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.Set;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9_@.-]{3,50}$", message = "Username must be 3-50 characters long")
    private String username;

    private String password;

    @NotNull(message = "Status is required")
    private UserStatus status;

    private Set<Long> roleIds;
}
