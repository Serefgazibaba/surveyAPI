package com.definex.survey.template.entity;

import com.definex.survey.common.entity.BaseEntity;
import com.definex.survey.template.dto.TemplateQuestion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Yeniden kullanılabilir anket iskeleti (FR-30). İçerik, kaydedildiği andaki soruların snapshot'ıdır:
 * kaynak anket değişse veya silinse şablon değişmez; şablon silinse ondan üretilmiş anketler etkilenmez (FR-31).
 */
@Entity
@Table(name = "templates", indexes = @Index(name = "ix_templates_owner_id", columnList = "owner_id"))
public class Template extends BaseEntity {

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<TemplateQuestion> content = new ArrayList<>();

    protected Template() {
    }

    public Template(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void replace(String name, String description, List<TemplateQuestion> content) {
        this.name = name;
        this.description = description;
        this.content = new ArrayList<>(content);
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<TemplateQuestion> getContent() {
        return content;
    }
}
