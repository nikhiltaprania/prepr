package com.springrag.user.service.impl;

import com.springrag.common.exception.DuplicateResourceException;
import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.user.domain.User;
import com.springrag.user.dto.CreateUserRequest;
import com.springrag.user.dto.UpdateUserRequest;
import com.springrag.user.repository.UserRepository;
import com.springrag.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setTargetRole(request.getTargetRole());
        user.setExperienceYears(request.getExperienceYears());
        user.setCurrentCompany(request.getCurrentCompany());
        user.setTargetCompany(request.getTargetCompany());
        user.setBio(request.getBio());
        return userRepository.save(user);
    }

    @Override
    public User updateUser(UUID userId, UpdateUserRequest request) {
        User user = getUser(userId);
        // NOTE: role and authProvider are intentionally NOT updatable here. Profile edits must never
        // change a user's security role (privilege-escalation guard). Role changes belong to a future
        // admin-only endpoint. `targetRole` below is the candidate's career goal, not a security role.
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setTargetRole(request.getTargetRole());
        user.setExperienceYears(request.getExperienceYears());
        user.setCurrentCompany(request.getCurrentCompany());
        user.setTargetCompany(request.getTargetCompany());
        user.setBio(request.getBio());
        return userRepository.save(user);
    }

    @Override
    public User updateRole(UUID userId, com.springrag.user.domain.UserRole role) {
        User user = getUser(userId);
        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(UUID userId) {
        User user = getUser(userId);
        userRepository.delete(user);
    }
}
