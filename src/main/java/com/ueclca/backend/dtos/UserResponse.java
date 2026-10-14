package com.ueclca.backend.dtos;

import com.ueclca.backend.models.User;

public record UserResponse(Long id, String email, String fullName, String role, boolean enabled) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole().name(), user.isEnabled());
    }
}
