package com.businessmanager.backend.security.service;

import com.businessmanager.backend.security.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleService {
    Role createRole(Role role);
    Role updateRole(Long id, Role updatedRole);
    Role getRoleById(Long id);
    Page<Role> searchRoles(String search, Pageable pageable);
    void deactivateRole(Long id);
}
