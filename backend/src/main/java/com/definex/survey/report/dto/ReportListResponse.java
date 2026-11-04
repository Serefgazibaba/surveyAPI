package com.definex.survey.report.dto;

import com.definex.survey.common.dto.PageMeta;
import com.definex.survey.survey.entity.ResponseMode;
import com.definex.survey.survey.entity.SurveyStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Tüm raporlar: taslak olmayan anketlerin özet sayıları. */
public record ReportListResponse(List<Item> items, PageMeta page) {

    /**
     * @param participationRate yanıt / maili gönderilen davet × 100 (BR-10), bir ondalık
     * @param lastResponseAt    son yanıt zamanı; yanıt yoksa yer almaz
     */
    public record Item(
            UUID surveyId,
            String title,
            SurveyStatus status,
            ResponseMode responseMode,
            Instant endsAt,
            long invitationCount,
            long deliveredInvitationCount,
            long responseCount,
            double participationRate,
            Instant lastResponseAt) {
    }
}
