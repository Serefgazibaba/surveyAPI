package com.definex.survey.survey.dto;

import com.definex.survey.participant.dto.ParticipantInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Yayınla. Katılımcılar iki kaynaktan gelir; ikisi birlikte de kullanılabilir, toplamda en az bir kişi gerekir.
 *
 * @param userIds      kayıtlı kullanıcılardan seçilenler ({@code GET /admin/getAllUsers}); tekrarlananlar tekilleştirilir
 * @param participants uygulamada kayıtlı olmayan kişiler (ad + e-posta); aynı e-posta tek davete düşer
 */
public record PublishSurveyRequest(
        @NotNull UUID surveyId,
        @Size(max = 1000) List<@NotNull UUID> userIds,
        @Valid @Size(max = 1000) List<@NotNull ParticipantInput> participants) {

    public List<UUID> userIdsOrEmpty() {
        return userIds == null ? List.of() : userIds;
    }

    public List<ParticipantInput> participantsOrEmpty() {
        return participants == null ? List.of() : participants;
    }
}
