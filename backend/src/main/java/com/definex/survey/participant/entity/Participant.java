package com.definex.survey.participant.entity;

import com.definex.survey.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Ankete davet edilen kişi. {@code email} küçük harfe çevrilmiş ve tekildir. {@code userId}, kişi uygulamada kayıtlı
 * bir kullanıcıysa doludur (users.id; modül sınırı nedeniyle JPA ilişkisi yok).
 */
@Entity
@Table(name = "participants")
public class Participant extends BaseEntity {

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "user_id", unique = true)
    private UUID userId;

    protected Participant() {
    }

    public Participant(String email, String fullName, UUID userId) {
        this.email = email;
        this.fullName = fullName;
        this.userId = userId;
    }

    /** Dış katılımcı olarak eklenmiş kişi sonradan kayıtlı kullanıcıyla eşleşirse bağlanır. */
    public void linkUser(UUID userId) {
        if (this.userId == null) {
            this.userId = userId;
        }
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public UUID getUserId() {
        return userId;
    }
}
