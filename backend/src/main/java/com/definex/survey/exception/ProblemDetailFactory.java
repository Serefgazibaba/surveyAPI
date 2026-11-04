package com.definex.survey.exception;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * RFC 7807 hata gövdesi üretimi. Tüm hatalar (controller, security filter) bu formatı kullanır:
 * {@code type, title, status, detail, instance, code, errors[]}.
 */
public final class ProblemDetailFactory {

    public static final String TYPE_BASE = "https://survey.example.com/problems/";
    public static final String CODE = "code";
    public static final String ERRORS = "errors";

    private ProblemDetailFactory() {
    }

    public static ProblemDetail create(HttpStatusCode status, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(typeOf(code));
        problem.setTitle(title);
        if (detail != null) {
            problem.setDetail(detail);
        }
        problem.setProperty(CODE, code);
        return problem;
    }

    public static ProblemDetail create(HttpStatusCode status, String code, String title, String detail,
                                       List<FieldErrorItem> errors) {
        ProblemDetail problem = create(status, code, title, detail);
        if (errors != null && !errors.isEmpty()) {
            problem.setProperty(ERRORS, errors);
        }
        return problem;
    }

    /** {@code VALIDATION_ERROR} → {@code https://.../problems/validation-error} */
    public static URI typeOf(String code) {
        return URI.create(TYPE_BASE + code.toLowerCase(Locale.ROOT).replace('_', '-'));
    }
}
