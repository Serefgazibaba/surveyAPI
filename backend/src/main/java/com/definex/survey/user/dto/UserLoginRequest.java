package com.definex.survey.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param invitationToken davet linkindeki token ({@code {FRONTEND}/s/{invitationToken}}) */
public record UserLoginRequest(@NotBlank @Size(max = 256) String invitationToken) {

    @Override
    public String toString() {
        return "UserLoginRequest[invitationToken=***]";
    }
}
