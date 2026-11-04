package com.definex.survey.user.repository;

import com.definex.survey.user.entity.Response;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResponseRepository extends JpaRepository<Response, UUID> {

    /** Satır: [surveyId, count, avg(durationSeconds), max(submittedAt)]; süresi olmayan yanıtlar ortalamaya girmez. */
    @Query("""
            select r.surveyId, count(r), avg(r.durationSeconds), max(r.submittedAt) from Response r
            where r.surveyId in :surveyIds group by r.surveyId""")
    List<Object[]> stats(@Param("surveyIds") Collection<UUID> surveyIds);

    /** Satır: [id, invitationId, submittedAt, durationSeconds]; en yeni önce, sayfa sayfa (dışa aktarım partileri). */
    @Query("""
            select r.id, r.invitationId, r.submittedAt, r.durationSeconds from Response r
            where r.surveyId = :surveyId order by r.submittedAt desc, r.id""")
    List<Object[]> findHeaders(@Param("surveyId") UUID surveyId, Pageable pageable);
}
