package com.definex.survey.exception;

import org.springframework.http.HttpStatus;

/**
 * Kaynak yok veya kullanıcının sahipliğinde değil. Sahiplik ihlalinde de 404 döner (varlık sızdırılmaz, BR-09).
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String code, String title) {
        super(HttpStatus.NOT_FOUND, code, title);
    }
}
