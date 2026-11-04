package com.definex.survey.survey.service;

import com.definex.survey.survey.repository.InvitationMailQueryRepository;
import com.definex.survey.survey.repository.InvitationRepository;
import com.definex.survey.survey.repository.SurveyCount;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rapor modülü için davet verileri (salt okuma). Sahiplik kontrolü çağıran tarafta
 * ({@link SurveyService#get} / {@link SurveyService#list}) yapılmış olmalıdır.
 */
@Service
public class SurveyReportDataService {

    private final InvitationRepository invitationRepository;
    private final InvitationMailQueryRepository mailQueryRepository;

    public SurveyReportDataService(InvitationRepository invitationRepository,
                                   InvitationMailQueryRepository mailQueryRepository) {
        this.invitationRepository = invitationRepository;
        this.mailQueryRepository = mailQueryRepository;
    }

    /** Anket başına davet sayısı ve maili başarıyla gönderilen davet sayısı; tek seferde. */
    @Transactional(readOnly = true)
    public Map<UUID, InvitationStats> invitationStats(Collection<UUID> surveyIds) {
        Map<UUID, InvitationStats> result = new HashMap<>();
        if (surveyIds.isEmpty()) {
            return result;
        }
        Map<UUID, Long> invited = new HashMap<>();
        invitationRepository.countBySurveyIds(surveyIds).forEach((SurveyCount c) -> invited.put(c.surveyId(), c.count()));
        Map<UUID, Long> delivered = mailQueryRepository.countDeliveredBySurveyIds(surveyIds);
        for (UUID id : surveyIds) {
            result.put(id, new InvitationStats(invited.getOrDefault(id, 0L), delivered.getOrDefault(id, 0L)));
        }
        return result;
    }

    /** Davet → katılımcı (yalnızca kayıtlı moddaki yanıtlar için anlamlıdır). */
    @Transactional(readOnly = true)
    public Map<UUID, UUID> participantsByInvitation(Collection<UUID> invitationIds) {
        Map<UUID, UUID> result = new HashMap<>();
        if (invitationIds.isEmpty()) {
            return result;
        }
        invitationRepository.findParticipantIds(invitationIds).forEach(row -> result.put((UUID) row[0], (UUID) row[1]));
        return result;
    }

    /** @param delivered maili {@code SENT} olan davet sayısı (katılım oranının paydası, BR-10) */
    public record InvitationStats(long invited, long delivered) {
    }
}
