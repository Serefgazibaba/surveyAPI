package com.definex.survey.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 128) String password) {

    /** Baştaki/sondaki boşluklar doğrulamadan önce atılır. */
    public AdminLoginRequest {
        email = email == null ? null : email.strip();
    }

    @Override
    public String toString() {
        return "AdminLoginRequest[email=" + email + ", password=***]";
    }
}
