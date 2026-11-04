package com.definex.survey.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Ara kayıt; mevcut ara kaydın tamamının yerine geçer.
 *
 * @param currentQuestionId katılımcının kaldığı soru (opsiyonel)
 */
public record UserDraftRequest(UUID currentQuestionId, @NotNull @Valid @Size(max = 200) List<AnswerInput> answers) {
}
