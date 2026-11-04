package com.definex.survey.auth.dto;

import com.definex.survey.auth.entity.User;
import com.definex.survey.auth.entity.UserRole;
import java.util.UUID;

public record AdminUserResponse(UUID id, String fullName, String email, UserRole role) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
