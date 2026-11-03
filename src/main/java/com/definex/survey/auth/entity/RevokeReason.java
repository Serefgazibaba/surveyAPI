package com.definex.survey.auth.entity;

public enum RevokeReason {
    LOGOUT,
    IDLE_TIMEOUT,
    EXPIRED,
    TOKEN_REUSE,
    /** Kullanıcı pasifleştirildi veya rolü uca uygun değil. */
    ACCOUNT_DISABLED
}
