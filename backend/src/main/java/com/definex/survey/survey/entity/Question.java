package com.definex.survey.survey.entity;

import com.definex.survey.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Anket sorusu. {@code text} ve {@code type} taslakta boş olabilir. {@code settings} tipe özel ayarlardır (jsonb),
 * bkz. {@code QuestionSettingsValidator}.
 */
@Entity
@Table(name = "questions", indexes = @Index(name = "ix_questions_survey_id", columnList = "survey_id"))
public class Question extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(nullable = false)
    private int sortOrder;

    @Column(length = 1000)
    private String text;

    @Convert(converter = QuestionTypeConverter.class)
    @Column(length = 30)
    private QuestionType type;

    @Column(nullable = false)
    private boolean required;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> settings;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<QuestionOption> options = new ArrayList<>();

    protected Question() {
    }

    public Question(Survey survey) {
        this.survey = survey;
    }

    public void update(String text, QuestionType type, boolean required, Map<String, Object> settings) {
        this.text = text;
        this.type = type;
        this.required = required;
        this.settings = settings;
    }

    public void updateText(String text) {
        this.text = text;
    }

    void placeAt(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** Seçenek listesini verilen sırayla değiştirir; listede olmayanlar silinir. */
    public void replaceOptions(List<QuestionOption> ordered) {
        options.clear();
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).placeAt(i);
            options.add(ordered.get(i));
        }
    }

    public Survey getSurvey() {
        return survey;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getText() {
        return text;
    }

    public QuestionType getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public List<QuestionOption> getOptions() {
        return options;
    }
}
