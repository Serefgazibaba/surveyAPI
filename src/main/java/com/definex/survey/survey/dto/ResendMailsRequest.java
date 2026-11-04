package com.definex.survey.survey.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** @param emailIds yeniden gönderilecek mailler ({@code GET /admin/getMails}); tekrarlananlar tekilleştirilir */
public record ResendMailsRequest(@NotEmpty @Size(max = 100) List<@NotNull UUID> emailIds) {
}
