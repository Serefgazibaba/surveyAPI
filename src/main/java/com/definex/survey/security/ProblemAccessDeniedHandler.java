package com.definex.survey.security;

import com.definex.survey.exception.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Kimlik doğrulanmış ama rol yetersiz (ör. USER token'ı ile /admin/**): 403 + ProblemDetail. */
@Component
class ProblemAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailResponseWriter writer;

    ProblemAccessDeniedHandler(ProblemDetailResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        writer.write(request, response, ProblemDetailFactory.create(
                HttpStatus.FORBIDDEN, "FORBIDDEN", "Bu işlem için yetkiniz yok", null));
    }
}
