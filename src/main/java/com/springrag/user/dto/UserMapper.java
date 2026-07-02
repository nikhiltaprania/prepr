package com.springrag.user.dto;

import com.springrag.user.domain.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole() != null ? user.getRole().name() : null);
        response.setTargetRole(user.getTargetRole());
        response.setExperienceYears(user.getExperienceYears());
        response.setCurrentCompany(user.getCurrentCompany());
        response.setTargetCompany(user.getTargetCompany());
        response.setBio(user.getBio());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
