package com.definex.survey.user.dto;

import java.time.Instant;

public record UserDraftResponse(Instant savedAt, int answeredCount, int totalQuestionCount) {
}
