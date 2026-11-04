package com.definex.survey.user.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Formla birlikte dönen ara kayıt; katılımcı kaldığı yerden devam eder (FR-63). */
public record UserDraft(UUID currentQuestionId, List<AnswerInput> answers, Instant savedAt) {
}
