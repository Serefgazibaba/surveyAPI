package com.definex.survey.report.dto;

import com.definex.survey.survey.entity.QuestionType;
import com.definex.survey.survey.entity.ResponseMode;
import com.definex.survey.survey.entity.SurveyStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Rapor detayı (FR-70–74). Değeri olmayan alanlar yanıtta yer almaz.
 *
 * @param responses katılımcı bazlı yanıtlar; yalnızca kayıtlı modda (FR-74), anonim modda yer almaz (BR-06)
 */
public record SurveyReportResponse(
        UUID surveyId,
        String title,
        String description,
        SurveyStatus status,
        ResponseMode responseMode,
        Instant endsAt,
        Instant generatedAt,
        Summary summary,
        List<QuestionReport> questions,
        List<ParticipantResponse> responses) {

    /**
     * @param participationRate        yanıt / maili gönderilen davet × 100 (BR-10), bir ondalık
     * @param averageSatisfactionScore tüm Likert cevaplarının 1–5 ölçeğine çevrilmiş ortalaması; Likert cevabı yoksa yok
     * @param averageCompletionSeconds süresi olan yanıtların ortalaması
     */
    public record Summary(
            long invitationCount,
            long deliveredInvitationCount,
            long responseCount,
            double participationRate,
            Double averageSatisfactionScore,
            Long averageCompletionSeconds) {
    }

    /**
     * Tipe göre dolu alanlar: LIKERT → scale, average, distribution; SINGLE/MULTIPLE_CHOICE → options, otherCount,
     * otherAnswers; FREE_TEXT → answers.
     *
     * @param answerCount soruyu yanıtlayan kişi sayısı (yüzdelerin paydası)
     */
    public record QuestionReport(
            UUID questionId,
            int number,
            String text,
            QuestionType type,
            long answerCount,
            Integer scale,
            Double average,
            List<LikertBucket> distribution,
            List<OptionCount> options,
            Long otherCount,
            List<TextAnswer> otherAnswers,
            List<TextAnswer> answers) {
    }

    /** @param label yalnızca uç değerlerde (ölçeğin min/max etiketi) */
    public record LikertBucket(int value, String label, long count, double percentage) {
    }

    public record OptionCount(UUID optionId, String label, long count, double percentage) {
    }

    public record TextAnswer(String text, Instant submittedAt) {
    }

    /** @param answers yalnızca yanıtlanan sorular; {@code value} okunur metindir */
    public record ParticipantResponse(
            UUID responseId,
            Participant participant,
            Instant submittedAt,
            Integer durationSeconds,
            List<AnswerValue> answers) {
    }

    public record Participant(String fullName, String email) {
    }

    public record AnswerValue(UUID questionId, String value) {
    }
}
