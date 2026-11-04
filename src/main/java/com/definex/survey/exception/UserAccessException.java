package com.definex.survey.exception;

import org.springframework.http.HttpStatus;

/**
 * Katılımcının ankete erişim hatası (geçersiz davet, süresi dolmuş/kapalı anket, zaten yanıtlanmış).
 * Hata sayfası için {@code surveyTitle} ve {@code contactInfo} alanları yanıta eklenir (spec: UserAccessProblem).
 */
public class UserAccessException extends ApiException {

    public static final String SURVEY_TITLE = "surveyTitle";
    public static final String CONTACT_INFO = "contactInfo";

    private final String surveyTitle;

    public UserAccessException(HttpStatus status, String code, String title, String surveyTitle) {
        super(status, code, title);
        this.surveyTitle = surveyTitle;
    }

    /** Davet bulunamadıysa {@code null}. */
    public String getSurveyTitle() {
        return surveyTitle;
    }
}
