package com.definex.survey.auth.dto;

import com.definex.survey.auth.entity.User;
import java.util.UUID;

/** Ankete davet edilebilecek kullanıcı (katılımcı). */
public record UserSummaryResponse(UUID id, String fullName, String email) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
