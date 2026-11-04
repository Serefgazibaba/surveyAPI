package com.definex.survey.report.service;

import com.definex.survey.exception.ApiException;
import com.definex.survey.exception.NotFoundException;
import com.definex.survey.report.dto.ReportSummaryResponse;
import com.definex.survey.report.dto.SurveyReportResponse;
import com.definex.survey.survey.entity.InvitationStatus;
import com.definex.survey.survey.entity.Survey;
import com.definex.survey.survey.entity.SurveyStatus;
import com.definex.survey.survey.repository.InvitationRepository;
import com.definex.survey.survey.repository.SurveyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tamamlanan davetlerden anlık survey raporu üretir.
 *
 * <p>Yanıt/answer entity'leri eklenene kadar soru kırılımı güvenli biçimde boş tutulur;
 * tamamlanmamış taslak kayıtları rapora dahil edilmez.
 */
@Service
public class ReportSurveyService {

    private static final int DEFAULT_ANONYMITY_THRESHOLD = 5;

    private final SurveyRepository surveyRepository;
    private final InvitationRepository invitationRepository;
    private final Clock clock;

    public ReportSurveyService(SurveyRepository surveyRepository,
                               InvitationRepository invitationRepository,
                               Clock clock) {
        this.surveyRepository = surveyRepository;
        this.invitationRepository = invitationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SurveyReportResponse get(UUID ownerId, UUID surveyId, int textAnswerLimit) {
        Survey survey = surveyRepository.findByIdAndOwnerId(surveyId, ownerId)
                .orElseThrow(() -> new NotFoundException("SURVEY_NOT_FOUND", "Anket bulunamadı"));
        if (survey.getStatus() == SurveyStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "REPORT_NOT_AVAILABLE",
                    "Taslak anket için rapor oluşturulamaz");
        }

        long invitationCount = invitationRepository.countBySurveyId(surveyId);
        long deliveredInvitationCount =
                invitationRepository.countBySurveyIdAndStatusNot(surveyId, InvitationStatus.CANCELLED);
        long responseCount =
                invitationRepository.countBySurveyIdAndStatus(surveyId, InvitationStatus.COMPLETED);
        double participationRate = deliveredInvitationCount == 0 ? 0
                : percentage(responseCount, deliveredInvitationCount);
        boolean thresholdMet = responseCount >= DEFAULT_ANONYMITY_THRESHOLD;

        ReportSummaryResponse summary = new ReportSummaryResponse(
                invitationCount, deliveredInvitationCount, responseCount, participationRate, null, null);
        return new SurveyReportResponse(
                survey.getId(), survey.getTitle(), survey.getStatus(), survey.getResponseMode(),
                survey.getEndsAt(), Instant.now(clock), summary, thresholdMet,
                DEFAULT_ANONYMITY_THRESHOLD, List.of());
    }

    private static double percentage(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

