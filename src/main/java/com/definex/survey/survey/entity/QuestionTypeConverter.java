package com.definex.survey.survey.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link QuestionType}'ı düz string olarak saklar. {@code @Enumerated} kullanılmaz, çünkü Hibernate kolona enum
 * değerlerini sabitleyen bir CHECK kısıtı ekler; yeni soru tipi eklendiğinde (NFR-20) bu kısıt insert'leri bozar.
 */
@Converter
public class QuestionTypeConverter implements AttributeConverter<QuestionType, String> {

    @Override
    public String convertToDatabaseColumn(QuestionType type) {
        return type == null ? null : type.name();
    }

    @Override
    public QuestionType convertToEntityAttribute(String value) {
        return value == null ? null : QuestionType.valueOf(value);
    }
}
