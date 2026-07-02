package com.springrag.user.dto;

import com.springrag.user.domain.UserRole;
import jakarta.validation.constraints.NotNull;

public class UpdateRoleRequest {

    @NotNull
    private UserRole role;

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
