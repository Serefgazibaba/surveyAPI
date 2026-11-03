package com.definex.survey.survey.repository;

import com.definex.survey.survey.entity.Survey;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface SurveyRepository extends JpaRepository<Survey, UUID>, JpaSpecificationExecutor<Survey> {

    /** Sahiplik kontrolü: başka bir admin'in anketi bulunamaz (BR-09). */
    Optional<Survey> findByIdAndOwnerId(UUID id, UUID ownerId);

    /** Liste ekranı için soru sayıları; tek sorguda (N+1 olmadan). */
    @Query("""
            select new com.definex.survey.survey.repository.SurveyCount(q.survey.id, count(q))
            from Question q
            where q.survey.id in :surveyIds
            group by q.survey.id
            """)
    List<SurveyCount> countQuestions(@Param("surveyIds") Collection<UUID> surveyIds);

    /** Publish için satır kilidiyle getirir; aynı anda gelen iki publish isteğinden yalnızca biri geçer. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select s from Survey s where s.id = :id and s.ownerId = :ownerId")
    Optional<Survey> findForUpdateByIdAndOwnerId(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

    /** Bitiş tarihi geçmiş yayındaki anketleri {@code EXPIRED} yapar. */
    @Modifying
    @Query("""
            update Survey s set s.status = com.definex.survey.survey.entity.SurveyStatus.EXPIRED, s.updatedAt = :now
            where s.status = com.definex.survey.survey.entity.SurveyStatus.PUBLISHED and s.endsAt <= :now
            """)
    int expirePublished(@Param("now") Instant now);
}
