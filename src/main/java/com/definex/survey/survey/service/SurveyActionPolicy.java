package com.definex.survey.survey.service;

import com.definex.survey.survey.entity.SurveyAction;
import com.definex.survey.survey.entity.SurveyStatus;
import java.util.List;

/**
 * Duruma göre ekranda aktif olacak aksiyonlar (FR-11). Yalnızca uygulanmış uçların aksiyonları listelenir;
 * rapor, kapatma vb. uçlar yazıldıkça eklenir.
 */
final class SurveyActionPolicy {

    private SurveyActionPolicy() {
    }

    static List<SurveyAction> allowedActions(SurveyStatus status) {
        return switch (status) {
            case DRAFT -> List.of(SurveyAction.EDIT, SurveyAction.PUBLISH, SurveyAction.DELETE);
            case PUBLISHED -> List.of(SurveyAction.EDIT);
            case EXPIRED, CLOSED, ARCHIVED -> List.of();
        };
    }
}
