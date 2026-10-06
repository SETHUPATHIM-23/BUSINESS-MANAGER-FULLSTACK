package com.businessmanager.backend.security.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.security.entity.Role;
import com.businessmanager.backend.security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    @AuditAction(action = "CREATE_ROLE", module = "ADMIN")
    public Role createRole(Role role) {
        if (roleRepository.existsByName(role.getName())) {
            throw new BusinessRuleException("Role name '" + role.getName() + "' already exists");
        }
        return roleRepository.save(role);
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE_ROLE", module = "ADMIN")
    public Role updateRole(Long id, Role updatedRole) {
        Role existingRole = getRoleById(id);
        
        if (!existingRole.getName().equals(updatedRole.getName()) && 
            roleRepository.existsByName(updatedRole.getName())) {
            throw new BusinessRuleException("Role name '" + updatedRole.getName() + "' already exists");
        }
        
        if ("ROLE_ADMINISTRATOR".equals(existingRole.getName()) && !existingRole.getName().equals(updatedRole.getName())) {
            throw new BusinessRuleException("Cannot rename the core ROLE_ADMINISTRATOR role");
        }

        existingRole.setName(updatedRole.getName());
        existingRole.setPermissions(updatedRole.getPermissions());
        
        return roleRepository.save(existingRole);
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Role> searchRoles(String search, Pageable pageable) {
        return roleRepository.searchRoles(search, pageable);
    }

    @Override
    @Transactional
    @AuditAction(action = "DEACTIVATE_ROLE", module = "ADMIN")
    public void deactivateRole(Long id) {
        Role existingRole = getRoleById(id);
        if ("ROLE_ADMINISTRATOR".equals(existingRole.getName())) {
            throw new BusinessRuleException("Cannot delete or deactivate the core administrator role");
        }
        throw new BusinessRuleException("Roles cannot be deleted to preserve audit integrity");
    }
}
