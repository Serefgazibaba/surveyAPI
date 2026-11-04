package com.definex.survey.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Nihai cevapların tamamı; ara kayıtla birleştirilmez. */
public record UserSubmitRequest(@NotNull @Valid @Size(max = 200) List<AnswerInput> answers) {
}
