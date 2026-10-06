package com.businessmanager.backend.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9_@.-]{3,50}$", message = "Username must be 3-50 characters long")
    private String username;

    private String newPassword;
}
