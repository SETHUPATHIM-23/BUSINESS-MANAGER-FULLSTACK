package com.businessmanager.backend.security.service;

import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    User createUser(User user);
    User updateUser(Long id, User updatedUser);
    User getUserById(Long id);
    User getUserByUsername(String username);
    Page<User> searchUsers(String search, UserStatus status, Pageable pageable);
    void deactivateUser(Long id);
}
