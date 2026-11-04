package com.definex.survey.participant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * Uygulamada kayıtlı olmayan (veya kayıtlı olduğu bilinmeyen) katılımcı: ad + e-posta.
 * E-posta doğrulamadan önce boşluklardan arındırılır ve küçük harfe çevrilir; aynı adres tek katılımcıdır.
 */
public record ParticipantInput(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Email @Size(max = 254) String email) {

    public ParticipantInput {
        fullName = fullName == null ? null : fullName.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
