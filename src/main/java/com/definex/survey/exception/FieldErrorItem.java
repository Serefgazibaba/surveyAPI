package com.definex.survey.exception;

/**
 * Alan bazında hata. {@code field}: alan yolu (ör. {@code sections[0].title}) veya soru id'si; alana bağlı değilse boş.
 */
public record FieldErrorItem(String field, String code, String message) {
}
