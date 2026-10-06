package com.businessmanager.backend.security.dto;

import com.businessmanager.backend.security.enums.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserDto {
    private Long id;
    private String username;
    private UserStatus status;
    private Integer failedLoginCount;
    private LocalDateTime lockedUntil;
    private Set<RoleDto> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
