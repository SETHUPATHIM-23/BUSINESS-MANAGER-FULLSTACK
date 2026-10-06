package com.businessmanager.backend.security.service;

import com.businessmanager.backend.security.entity.Permission;
import com.businessmanager.backend.security.entity.Role;
import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.enums.UserStatus;
import com.businessmanager.backend.security.repository.PermissionRepository;
import com.businessmanager.backend.security.repository.RoleRepository;
import com.businessmanager.backend.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Verifying RBAC system initialization and Administrator role permissions...");
        try {
            // 1. Create or retrieve ROLE_ADMINISTRATOR
            Optional<Role> adminRoleOpt = roleRepository.findByName("ROLE_ADMINISTRATOR");
            Role adminRole;
            if (adminRoleOpt.isPresent()) {
                adminRole = adminRoleOpt.get();
            } else {
                Role role = new Role();
                role.setName("ROLE_ADMINISTRATOR");
                role.setCreatedBy("system");
                role.setUpdatedBy("system");
                adminRole = roleRepository.save(role);
            }

            if (adminRole == null) {
                return;
            }

            // 2. Grant all system permissions to ROLE_ADMINISTRATOR
            List<Permission> allPermissions = permissionRepository.findAll();
            if (adminRole.getPermissions() == null) {
                adminRole.setPermissions(new HashSet<>());
            }
            if (allPermissions != null && !allPermissions.isEmpty() && adminRole.getPermissions().size() < allPermissions.size()) {
                adminRole.getPermissions().addAll(allPermissions);
                Role savedRole = roleRepository.save(adminRole);
                if (savedRole != null) {
                    adminRole = savedRole;
                }
            }

            // 3. Ensure default admin accounts (sethu, admin) have ROLE_ADMINISTRATOR assigned
            List<String> adminUsernames = List.of("sethu", "admin");
            for (String username : adminUsernames) {
                final Role targetRole = adminRole;
                userRepository.findByUsername(username).ifPresent(user -> {
                    if (user != null) {
                        if (user.getRoles() == null) {
                            user.setRoles(new HashSet<>());
                        }
                        boolean hasAdminRole = user.getRoles().stream()
                                .anyMatch(r -> r != null && "ROLE_ADMINISTRATOR".equals(r.getName()));
                        if (!hasAdminRole) {
                            user.getRoles().add(targetRole);
                            userRepository.save(user);
                            log.info("Successfully linked user '{}' to ROLE_ADMINISTRATOR.", username);
                        }
                    }
                });
            }

            // 4. Seed initial administrator if database has zero users
            long count = userRepository.count();
            if (count == 0) {
                log.info("Zero users found. Creating default administrator 'sethu'...");
                User admin = new User();
                admin.setUsername("sethu");
                admin.setPasswordHash(passwordEncoder.encode("Sethupathi#*$1"));
                admin.setStatus(UserStatus.ACTIVE);
                admin.setRoles(Set.of(adminRole));
                admin.setCreatedBy("system-initializer");
                admin.setUpdatedBy("system-initializer");
                userRepository.save(admin);
                log.info("Default administrator 'sethu' created successfully.");
            }
        } catch (Exception e) {
            log.warn("AdminUserInitializer completed initialization check with notice: {}", e.getMessage());
        }
    }
}
