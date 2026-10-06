package com.businessmanager.backend.security.controller;

import com.businessmanager.backend.security.dto.*;
import com.businessmanager.backend.security.entity.Permission;
import com.businessmanager.backend.security.entity.Role;
import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.enums.UserStatus;
import com.businessmanager.backend.security.mapper.AuditLogEntryMapper;
import com.businessmanager.backend.security.mapper.PermissionMapper;
import com.businessmanager.backend.security.mapper.RoleMapper;
import com.businessmanager.backend.security.mapper.UserMapper;
import com.businessmanager.backend.security.repository.PermissionRepository;
import com.businessmanager.backend.security.service.AuditLogService;
import com.businessmanager.backend.security.service.PermissionService;
import com.businessmanager.backend.security.service.RoleService;
import com.businessmanager.backend.security.service.UserService;
import com.businessmanager.backend.dashboard.service.DashboardConfigService;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;
    private final PermissionRepository permissionRepository;
    private final DashboardConfigService dashboardConfigService;

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final AuditLogEntryMapper auditLogMapper;

    // --- Users ---

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('SYSTEM_READ')")
    public ResponseEntity<Page<UserDto>> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<User> users = userService.searchUsers(search, status, pageable);
        return ResponseEntity.ok(users.map(userMapper::toDto));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_READ')")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userMapper.toDto(userService.getUserById(id)));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('SYSTEM_WRITE')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(request.getPassword());
        
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = request.getRoleIds().stream()
                    .map(roleService::getRoleById)
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        User saved = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDto(saved));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_WRITE')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        User updatedUser = new User();
        updatedUser.setUsername(request.getUsername());
        updatedUser.setPasswordHash(request.getPassword());
        updatedUser.setStatus(request.getStatus());

        if (request.getRoleIds() != null) {
            Set<Role> roles = request.getRoleIds().stream()
                    .map(roleService::getRoleById)
                    .collect(Collectors.toSet());
            updatedUser.setRoles(roles);
        }

        User saved = userService.updateUser(id, updatedUser);
        return ResponseEntity.ok(userMapper.toDto(saved));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_WRITE')")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    // --- Roles ---

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('SYSTEM_READ')")
    public ResponseEntity<Page<RoleDto>> searchRoles(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Role> roles = roleService.searchRoles(search, pageable);
        return ResponseEntity.ok(roles.map(roleMapper::toDto));
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_READ')")
    public ResponseEntity<RoleDto> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(roleMapper.toDto(roleService.getRoleById(id)));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('SYSTEM_WRITE')")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody RoleCreateRequest request) {
        Role role = new Role();
        role.setName(request.getName());

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = request.getPermissionIds().stream()
                    .map(pid -> permissionRepository.findById(pid)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission not found with ID: " + pid)))
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        Role saved = roleService.createRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleMapper.toDto(saved));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_WRITE')")
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        Role role = new Role();
        role.setName(request.getName());

        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = request.getPermissionIds().stream()
                    .map(pid -> permissionRepository.findById(pid)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission not found with ID: " + pid)))
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        Role saved = roleService.updateRole(id, role);
        return ResponseEntity.ok(roleMapper.toDto(saved));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_WRITE')")
    public ResponseEntity<Void> deactivateRole(@PathVariable Long id) {
        roleService.deactivateRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles/{id}/dashboard-config")
    @PreAuthorize("hasAuthority('SYSTEM_READ')")
    public ResponseEntity<java.util.List<String>> getRoleDashboardConfig(@PathVariable Long id) {
        return ResponseEntity.ok(dashboardConfigService.getRoleWidgets(id));
    }

    @PutMapping("/roles/{id}/dashboard-config")
    @PreAuthorize("hasAuthority('SYSTEM_WRITE')")
    public ResponseEntity<Void> updateRoleDashboardConfig(@PathVariable Long id, @RequestBody java.util.List<String> widgets) {
        dashboardConfigService.updateRoleWidgets(id, widgets);
        return ResponseEntity.ok().build();
    }

    // --- Permissions ---

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('SYSTEM_READ')")
    public ResponseEntity<Page<PermissionDto>> searchPermissions(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Permission> permissions = permissionService.searchPermissions(search, pageable);
        return ResponseEntity.ok(permissions.map(permissionMapper::toDto));
    }

    // --- Audit Logs ---

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('SYSTEM_READ')")
    public ResponseEntity<Page<AuditLogEntryDto>> searchAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<com.businessmanager.backend.security.entity.AuditLogEntry> logs = auditLogService.searchAuditLogs(
                username, moduleName, actionType, startDate, endDate, pageable);
        return ResponseEntity.ok(logs.map(auditLogMapper::toDto));
    }
}
