package com.definex.survey.survey.entity;

import com.definex.survey.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Anket. Taslak olarak eksik alanlarla kaydedilebilir; tamlık yayınlamada kontrol edilir.
 * Sahibi {@code ownerId} (admin'in user id'si). Modül sınırı nedeniyle auth'un User entity'sine ilişki kurulmaz.
 */
@Entity
@Table(name = "surveys", indexes = @Index(name = "ix_surveys_owner_id", columnList = "owner_id"))
public class Survey extends BaseEntity {

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurveyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ResponseMode responseMode;

    private Instant endsAt;

    private Instant publishedAt;

    private Instant closedAt;

    private Instant archivedAt;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<Question> questions = new ArrayList<>();

    protected Survey() {
    }

    public Survey(UUID ownerId) {
        this.ownerId = ownerId;
        this.status = SurveyStatus.DRAFT;
    }

    public void updateDetails(String title, String description, Instant endsAt, ResponseMode responseMode) {
        this.title = title;
        this.description = description;
        this.endsAt = endsAt;
        this.responseMode = responseMode;
    }

    /** Yayındaki ankette yalnızca metin alanları değişebilir. */
    public void updateTexts(String title, String description) {
        this.title = title;
        this.description = description;
    }

    /** {@code DRAFT} → {@code PUBLISHED}. Ön koşullar çağıran service'te kontrol edilir. */
    public void publish(Instant now) {
        this.status = SurveyStatus.PUBLISHED;
        this.publishedAt = now;
    }

    /** Soru listesini verilen sırayla değiştirir; listede olmayan sorular silinir (orphanRemoval). */
    public void replaceQuestions(List<Question> ordered) {
        questions.clear();
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).placeAt(i);
            questions.add(ordered.get(i));
        }
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public SurveyStatus getStatus() {
        return status;
    }

    public ResponseMode getResponseMode() {
        return responseMode;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public List<Question> getQuestions() {
        return questions;
    }
}
