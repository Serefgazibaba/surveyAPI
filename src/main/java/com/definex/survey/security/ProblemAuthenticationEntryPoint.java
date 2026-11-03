package com.definex.survey.security;

import com.definex.survey.exception.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Token yok, geçersiz veya süresi dolmuş: 401 + ProblemDetail. */
@Component
class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailResponseWriter writer;

    ProblemAuthenticationEntryPoint(ProblemDetailResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        writer.write(request, response, ProblemDetailFactory.create(
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Oturum geçersiz", null));
    }
}
