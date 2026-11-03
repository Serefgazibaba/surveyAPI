package com.definex.survey.survey.repository;

import com.definex.survey.survey.entity.Survey;
import com.definex.survey.survey.entity.SurveyStatus;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Anket listesi filtreleri. */
public final class SurveySpecifications {

    private SurveySpecifications() {
    }

    public static Specification<Survey> ownedBy(UUID ownerId) {
        return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    /** Durum verilmezse {@code ARCHIVED} hariç tümü. */
    public static Specification<Survey> statusIn(Collection<SurveyStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return (root, query, cb) -> cb.notEqual(root.get("status"), SurveyStatus.ARCHIVED);
        }
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    /** Başlıkta büyük/küçük harf duyarsız arama; boşsa filtre uygulanmaz. */
    public static Specification<Survey> titleContains(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String pattern = "%" + escapeLike(q.strip().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\');
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
