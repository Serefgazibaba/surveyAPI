package com.definex.survey.user.dto;

import java.time.Instant;

/** Teşekkür ekranı (S-09) için. */
public record UserSubmitResponse(Instant submittedAt, String message) {
}
