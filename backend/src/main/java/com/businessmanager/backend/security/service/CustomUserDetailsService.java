package com.businessmanager.backend.security.service;

import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.businessmanager.backend.security.enums.UserStatus;
import java.time.LocalDateTime;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final com.businessmanager.backend.security.repository.PermissionRepository permissionRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        boolean enabled = user.getStatus() != UserStatus.INACTIVE;
        boolean accountNonLocked = true;
        
        if (user.getStatus() == UserStatus.LOCKED) {
            if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
                accountNonLocked = false;
            } else if (user.getLockedUntil() != null && LocalDateTime.now().isAfter(user.getLockedUntil())) {
                // Lock expired, we will allow login. Reset happens on successful login.
                accountNonLocked = true;
            } else {
                accountNonLocked = false;
            }
        }

        java.util.Set<org.springframework.security.core.GrantedAuthority> authorities = new java.util.HashSet<>();
        boolean isAdmin = false;

        if (user.getRoles() != null) {
            for (com.businessmanager.backend.security.entity.Role role : user.getRoles()) {
                if (role.getName() != null) {
                    authorities.add(new SimpleGrantedAuthority(role.getName()));
                    if ("ROLE_ADMINISTRATOR".equalsIgnoreCase(role.getName()) || "ADMINISTRATOR".equalsIgnoreCase(role.getName())) {
                        isAdmin = true;
                    }
                }
                if (role.getPermissions() != null) {
                    for (com.businessmanager.backend.security.entity.Permission permission : role.getPermissions()) {
                        if (permission.getCode() != null) {
                            authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            try {
                permissionRepository.findAll().forEach(p -> {
                    if (p.getCode() != null) {
                        authorities.add(new SimpleGrantedAuthority(p.getCode()));
                    }
                });
            } catch (Exception e) {
                // Ignore fallback
            }
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                enabled,
                true,
                true,
                accountNonLocked,
                authorities
        );
    }
}
