package com.definex.survey.auth.dto;

/**
 * @param expiresIn        access token ömrü (saniye)
 * @param refreshExpiresIn refresh token'ın kalan geçerliliği (saniye): hareketsizlik penceresi (30 dk) ile oturumun
 *                         mutlak ömründen kısa olanı. Her refresh'te baştan başlar.
 */
public record AdminAuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        AdminUserResponse user) {

    public static final String BEARER = "Bearer";
}
