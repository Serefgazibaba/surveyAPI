package com.definex.survey.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * İstek geçerli ama bir iş kuralını ihlal ediyor (422). Bozulan kurallar {@code errors} içinde listelenir.
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String code, String title, List<FieldErrorItem> errors) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, title, errors);
    }
}
