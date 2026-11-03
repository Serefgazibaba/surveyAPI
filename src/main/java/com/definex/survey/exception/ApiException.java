package com.definex.survey.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Service katmanından fırlatılan uygulama istisnalarının tabanı. {@link GlobalExceptionHandler} bunu
 * {@code status} ve {@code code} ile ProblemDetail'e çevirir.
 *
 * <p>Örnek: {@code throw new ApiException(HttpStatus.CONFLICT, "SURVEY_HAS_RESPONSES", "Yanıtı olan anket silinemez");}
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<FieldErrorItem> errors;

    public ApiException(HttpStatus status, String code, String title) {
        this(status, code, title, List.of());
    }

    public ApiException(HttpStatus status, String code, String title, List<FieldErrorItem> errors) {
        super(title);
        this.status = status;
        this.code = code;
        this.errors = List.copyOf(errors);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<FieldErrorItem> getErrors() {
        return errors;
    }
}
