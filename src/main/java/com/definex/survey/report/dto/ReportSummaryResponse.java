package com.definex.survey.report.dto;

/**
 * Anket raporunun davet ve tamamlanma özeti.
 */
public record ReportSummaryResponse(
        long invitationCount,
        long deliveredInvitationCount,
        long responseCount,
        double participationRate,
        Double averageSatisfactionScore,
        Long averageCompletionSeconds) {
}

