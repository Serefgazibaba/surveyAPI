package com.definex.survey.survey.repository;

import com.definex.survey.mail.entity.EmailStatus;
import com.definex.survey.survey.entity.InvitationStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Davet maili listesi (salt okuma). Davet, anket ve mail kayıtlarını tek sorguda birleştirir; durum filtreleri ve
 * sayfalama veritabanında yapılır.
 *
 * <p>Bilinçli modül istisnası: {@code mail} modülünün {@code EmailMessage} entity'si yalnızca okumak için join edilir.
 * Mail kayıtlarına yazma işlemi yalnızca {@code MailService} üzerinden yapılır.
 */
@Repository
public class InvitationMailQueryRepository {

    private static final String SELECT_ROW = """
            select new com.definex.survey.survey.repository.InvitationMailRow(
                m.id, i.id, s.id, s.title, s.status, s.endsAt, m.recipientName, m.recipientEmail,
                m.status, i.status, m.attemptCount, m.lastError, m.lastAttemptAt, m.sentAt, m.updatedAt, m.createdAt)
            """;
    private static final String FROM = """
            from com.definex.survey.mail.entity.EmailMessage m
            join Invitation i on i.id = m.invitationId
            join i.survey s
            """;

    private final EntityManager entityManager;

    public InvitationMailQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<InvitationMailRow> find(MailFilter filter, int offset, int limit) {
        Map<String, Object> params = new HashMap<>();
        String where = where(filter, params, true);
        TypedQuery<InvitationMailRow> query = entityManager.createQuery(
                SELECT_ROW + FROM + where + " order by m.lastAttemptAt desc nulls last, m.createdAt desc, m.id",
                InvitationMailRow.class);
        params.forEach(query::setParameter);
        return query.setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public long count(MailFilter filter) {
        Map<String, Object> params = new HashMap<>();
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(m) " + FROM + where(filter, params, true), Long.class);
        params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    /** Durum başına mail sayısı; durum filtreleri uygulanmaz (sekme rozetleri için). */
    public Map<EmailStatus, Long> countByEmailStatus(MailFilter filter) {
        Map<String, Object> params = new HashMap<>();
        TypedQuery<Object[]> query = entityManager.createQuery(
                "select m.status, count(m) " + FROM + where(filter, params, false) + " group by m.status",
                Object[].class);
        params.forEach(query::setParameter);
        Map<EmailStatus, Long> counts = new EnumMap<>(EmailStatus.class);
        for (EmailStatus status : EmailStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : query.getResultList()) {
            counts.put((EmailStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    /** Rapor: anket başına maili başarıyla gönderilmiş ({@code SENT}) davet sayısı (BR-10 paydası). */
    public Map<UUID, Long> countDeliveredBySurveyIds(Collection<UUID> surveyIds) {
        Map<UUID, Long> counts = new HashMap<>();
        if (surveyIds.isEmpty()) {
            return counts;
        }
        entityManager.createQuery("select s.id, count(m) " + FROM
                        + " where s.id in :ids and m.status = :sent group by s.id", Object[].class)
                .setParameter("ids", surveyIds)
                .setParameter("sent", EmailStatus.SENT)
                .getResultList()
                .forEach(row -> counts.put((UUID) row[0], (Long) row[1]));
        return counts;
    }

    /** Yeniden gönderim kontrolü için verilen maillerin satırları (yalnızca bu admin'in). */
    public List<InvitationMailRow> findByEmailIds(UUID ownerId, Collection<UUID> emailIds) {
        return entityManager.createQuery(
                        SELECT_ROW + FROM + " where s.ownerId = :ownerId and m.id in :ids", InvitationMailRow.class)
                .setParameter("ownerId", ownerId)
                .setParameter("ids", emailIds)
                .getResultList();
    }

    private static String where(MailFilter filter, Map<String, Object> params, boolean withStatusFilters) {
        StringBuilder where = new StringBuilder(" where s.ownerId = :ownerId");
        params.put("ownerId", filter.ownerId());
        if (filter.surveyId() != null) {
            where.append(" and s.id = :surveyId");
            params.put("surveyId", filter.surveyId());
        }
        if (filter.q() != null && !filter.q().isBlank()) {
            where.append(" and (lower(m.recipientName) like :q escape '\\' or lower(m.recipientEmail) like :q escape '\\')");
            params.put("q", "%" + filter.q().strip().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        if (withStatusFilters && !isEmpty(filter.emailStatuses())) {
            where.append(" and m.status in :emailStatuses");
            params.put("emailStatuses", filter.emailStatuses());
        }
        if (withStatusFilters && !isEmpty(filter.invitationStatuses())) {
            where.append(" and i.status in :invitationStatuses");
            params.put("invitationStatuses", filter.invitationStatuses());
        }
        return where.toString();
    }

    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    /** Verilmeyen (null/boş) filtre uygulanmaz. */
    public record MailFilter(
            UUID ownerId,
            UUID surveyId,
            String q,
            Collection<EmailStatus> emailStatuses,
            Collection<InvitationStatus> invitationStatuses) {
    }
}
