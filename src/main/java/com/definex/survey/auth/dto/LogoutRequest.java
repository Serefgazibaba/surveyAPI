package com.definex.survey.auth.dto;

import jakarta.validation.constraints.Size;

/** Opsiyonel: verilirse ve aynı kullanıcıya aitse bu refresh token'ın oturumu da iptal edilir. */
public record LogoutRequest(@Size(max = 256) String refreshToken) {

    @Override
    public String toString() {
        return "LogoutRequest[refreshToken=***]";
    }
}
