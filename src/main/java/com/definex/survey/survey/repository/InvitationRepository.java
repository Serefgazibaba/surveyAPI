package com.definex.survey.survey.repository;

import com.definex.survey.survey.entity.Invitation;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    long countBySurveyId(UUID surveyId);

    /** Davet linkindeki token'ın hash'iyle; anket de birlikte yüklenir. */
    @Query("select i from Invitation i join fetch i.survey where i.tokenHash = :tokenHash")
    Optional<Invitation> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Query("select i from Invitation i join fetch i.survey where i.id = :id")
    Optional<Invitation> findWithSurveyById(@Param("id") UUID id);

    /**
     * Yanıt gönderimi için davet satırını kilitler; aynı anda gelen iki gönderimden yalnızca biri geçer.
     * Anket ayrıca okunur (kilitlenmez).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select i from Invitation i where i.id = :id")
    Optional<Invitation> findForUpdateById(@Param("id") UUID id);

    /** Liste ekranı için davet sayıları; tek sorguda. */
    @Query("""
            select new com.definex.survey.survey.repository.SurveyCount(i.survey.id, count(i))
            from Invitation i
            where i.survey.id in :surveyIds
            group by i.survey.id
            """)
    List<SurveyCount> countBySurveyIds(@Param("surveyIds") Collection<UUID> surveyIds);
}
