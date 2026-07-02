package com.springrag.user.web;

import com.springrag.user.domain.User;
import com.springrag.user.dto.CreateUserRequest;
import com.springrag.user.dto.UpdateRoleRequest;
import com.springrag.user.dto.UpdateUserRequest;
import com.springrag.user.dto.UserMapper;
import com.springrag.user.dto.UserResponse;
import com.springrag.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Admin-only provisioning. Self-service signup goes through {@code POST /api/auth/register}
     * (which sets credentials); this creates a profile without a password.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return UserMapper.toResponse(userService.createUser(request));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@access.isCurrentUser(#userId) or hasRole('ADMIN')")
    public UserResponse getUser(@PathVariable UUID userId) {
        return UserMapper.toResponse(userService.getUser(userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream().map(UserMapper::toResponse).toList();
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@access.isCurrentUser(#userId) or hasRole('ADMIN')")
    public UserResponse updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(userId, request);
        return UserMapper.toResponse(user);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@access.isCurrentUser(#userId) or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /** Admin-only: change a user's security role. */
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRole(@PathVariable UUID userId, @Valid @RequestBody UpdateRoleRequest request) {
        return UserMapper.toResponse(userService.updateRole(userId, request.getRole()));
    }
}
