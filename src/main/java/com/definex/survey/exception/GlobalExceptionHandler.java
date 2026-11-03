package com.definex.survey.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Merkezi hata yönetimi. Tüm yanıtlar RFC 7807 {@link ProblemDetail} formatındadır ve {@code code} alanı taşır.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        return ProblemDetailFactory.create(ex.getStatus(), ex.getCode(), ex.getMessage(), null, ex.getErrors());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ProblemDetail> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity.status(ex.getStatus())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(handleApiException(ex));
    }

    /** {@code @RequestBody @Valid} doğrulama hataları: alan bazında listelenir. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldErrorItem> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldErrorItem(
                        error instanceof FieldError fieldError ? fieldError.getField() : null,
                        error.getCode(),
                        error.getDefaultMessage()))
                .toList();
        ProblemDetail body = ProblemDetailFactory.create(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "İstek doğrulanamadı", null, errors);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** {@code @Validated} ile doğrulanan path/query parametre hataları. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorItem> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorItem(v.getPropertyPath().toString(),
                        v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        v.getMessage()))
                .toList();
        return ProblemDetailFactory.create(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "İstek doğrulanamadı", null, errors);
    }

    /**
     * {@code @PreAuthorize} kaynaklı hatalar controller içinde fırlar; genel handler'a düşüp 500 olmasın diye
     * burada yakalanır.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetailFactory.create(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bu işlem için yetkiniz yok", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return ProblemDetailFactory.create(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Oturum geçersiz", null);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Beklenmeyen hata", ex);
        return ProblemDetailFactory.create(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Beklenmeyen bir hata oluştu", null);
    }

    /**
     * Spring MVC'nin kendi hatalarına (404, 405, 415, okunamayan JSON vb.) da {@code code} ve {@code type} ekler.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        // Spring'in standart hataları gövdeyi null gönderir; ProblemDetail istisnanın kendisinden alınır.
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.getBody();
        }
        if (body instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey(ProblemDetailFactory.CODE))) {
            String code = defaultCode(statusCode);
            problem.setType(ProblemDetailFactory.typeOf(code));
            problem.setProperty(ProblemDetailFactory.CODE, code);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static String defaultCode(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return switch (resolved == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolved) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case NOT_FOUND -> "NOT_FOUND";
            case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
            case NOT_ACCEPTABLE -> "NOT_ACCEPTABLE";
            case UNSUPPORTED_MEDIA_TYPE -> "UNSUPPORTED_MEDIA_TYPE";
            default -> resolved == null ? "ERROR" : resolved.name();
        };
    }
}
