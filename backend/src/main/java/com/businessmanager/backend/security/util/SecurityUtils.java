package com.businessmanager.backend.security.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;

public class SecurityUtils {

    /**
     * Checks if the currently authenticated user has any of the specified roles.
     * @param roles the roles to check (e.g. "ROLE_ADMINISTRATOR", "ROLE_HR")
     * @return true if the user has at least one of the roles, false otherwise
     */
    public static boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String userRole = authority.getAuthority();
            for (String role : roles) {
                if (userRole.equals(role)) {
                    return true;
                }
            }
        }
        return false;
    }
}
