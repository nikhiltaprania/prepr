package com.springrag.user.service;

import java.util.List;
import java.util.UUID;
import com.springrag.user.domain.User;
import com.springrag.user.domain.UserRole;
import com.springrag.user.dto.CreateUserRequest;
import com.springrag.user.dto.UpdateUserRequest;

public interface UserService {

    User createUser(CreateUserRequest request);

    User updateUser(UUID userId, UpdateUserRequest request);

    /** Admin-only: change a user's security role. */
    User updateRole(UUID userId, UserRole role);

    User getUser(UUID userId);

    List<User> getAllUsers();

    void deleteUser(UUID userId);
}