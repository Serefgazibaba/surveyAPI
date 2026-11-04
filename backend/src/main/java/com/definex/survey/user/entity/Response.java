package com.definex.survey.user.entity;

import com.definex.survey.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gönderilmiş (kesinleşmiş) yanıt. Anonim modda davetle bağ tutulmaz ({@code invitationId = null}) ve gönderim
 * zamanı saat başına yuvarlanır (BR-06); kayıtlı modda davete bağlıdır.
 */
@Entity
@Table(name = "responses", indexes = @Index(name = "ix_responses_survey_id", columnList = "survey_id"))
public class Response extends BaseEntity {

    @Column(name = "survey_id", nullable = false, updatable = false)
    private UUID surveyId;

    @Column(name = "invitation_id", unique = true, updatable = false)
    private UUID invitationId;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    private Integer durationSeconds;

    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    protected Response() {
    }

    public Response(UUID surveyId, UUID invitationId, Instant submittedAt, Integer durationSeconds) {
        this.surveyId = surveyId;
        this.invitationId = invitationId;
        this.submittedAt = submittedAt;
        this.durationSeconds = durationSeconds;
    }

    public void addAnswer(UUID questionId, Integer numericValue, String textValue, List<UUID> optionIds) {
        answers.add(new Answer(this, questionId, numericValue, textValue, optionIds));
    }

    public UUID getSurveyId() {
        return surveyId;
    }

    public UUID getInvitationId() {
        return invitationId;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public List<Answer> getAnswers() {
        return answers;
    }
}
