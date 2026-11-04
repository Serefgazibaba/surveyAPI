package com.definex.survey.user.entity;

import com.definex.survey.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bir sorunun cevabı. Likert → {@code numericValue}; serbest metin veya "Diğer" → {@code textValue};
 * seçimli sorularda seçilen seçenekler {@code answer_options} tablosundadır.
 */
@Entity
@Table(name = "answers",
        uniqueConstraints = @UniqueConstraint(name = "uk_answers_response_question", columnNames = {"response_id", "question_id"}))
public class Answer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "response_id", nullable = false)
    private Response response;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    private Integer numericValue;

    @Column(columnDefinition = "text")
    private String textValue;

    @ElementCollection
    @CollectionTable(name = "answer_options", joinColumns = @JoinColumn(name = "answer_id"))
    @Column(name = "option_id", nullable = false)
    private Set<UUID> optionIds = new LinkedHashSet<>();

    protected Answer() {
    }

    Answer(Response response, UUID questionId, Integer numericValue, String textValue, List<UUID> optionIds) {
        this.response = response;
        this.questionId = questionId;
        this.numericValue = numericValue;
        this.textValue = textValue;
        if (optionIds != null) {
            this.optionIds.addAll(optionIds);
        }
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public Integer getNumericValue() {
        return numericValue;
    }

    public String getTextValue() {
        return textValue;
    }

    public Set<UUID> getOptionIds() {
        return optionIds;
    }
}
