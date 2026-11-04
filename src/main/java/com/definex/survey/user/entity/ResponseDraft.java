package com.definex.survey.user.entity;

import com.definex.survey.common.entity.BaseEntity;
import com.definex.survey.user.dto.AnswerInput;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Gönderim öncesi ara kayıt; her davetin tek ara kaydı vardır. Gönderimde silinir (anonim modda yanıtla kişi
 * arasındaki son bağ da böylece kopar, BR-06). {@code createdAt} süre hesabının başlangıcıdır.
 */
@Entity
@Table(name = "response_drafts")
public class ResponseDraft extends BaseEntity {

    @Column(name = "invitation_id", nullable = false, unique = true, updatable = false)
    private UUID invitationId;

    @Column(name = "survey_id", nullable = false, updatable = false)
    private UUID surveyId;

    private UUID currentQuestionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<AnswerInput> answers = new ArrayList<>();

    protected ResponseDraft() {
    }

    public ResponseDraft(UUID invitationId, UUID surveyId) {
        this.invitationId = invitationId;
        this.surveyId = surveyId;
    }

    public void replace(UUID currentQuestionId, List<AnswerInput> answers) {
        this.currentQuestionId = currentQuestionId;
        this.answers = new ArrayList<>(answers);
    }

    public UUID getInvitationId() {
        return invitationId;
    }

    public UUID getSurveyId() {
        return surveyId;
    }

    public UUID getCurrentQuestionId() {
        return currentQuestionId;
    }

    public List<AnswerInput> getAnswers() {
        return answers;
    }
}
