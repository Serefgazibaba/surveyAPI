package com.definex.survey.survey.entity;

import com.definex.survey.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Seçimli sorunun seçeneği. "Diğer" seçeneği ayrı satır değil, soru ayarındaki {@code allowOther} ile ifade edilir. */
@Entity
@Table(name = "question_options", indexes = @Index(name = "ix_question_options_question_id", columnList = "question_id"))
public class QuestionOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private int sortOrder;

    @Column(length = 200)
    private String label;

    protected QuestionOption() {
    }

    public QuestionOption(Question question) {
        this.question = question;
    }

    public void updateLabel(String label) {
        this.label = label;
    }

    void placeAt(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Question getQuestion() {
        return question;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getLabel() {
        return label;
    }
}
