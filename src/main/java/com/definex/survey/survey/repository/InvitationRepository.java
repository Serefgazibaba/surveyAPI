package com.definex.survey.survey.repository;

import com.definex.survey.survey.entity.Invitation;
import com.definex.survey.survey.entity.InvitationStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    long countBySurveyId(UUID surveyId);

    long countBySurveyIdAndStatus(UUID surveyId, InvitationStatus status);

    long countBySurveyIdAndStatusNot(UUID surveyId, InvitationStatus status);

    /** Liste ekranı için davet sayıları; tek sorguda. */
    @Query("""
            select new com.definex.survey.survey.repository.SurveyCount(i.survey.id, count(i))
            from Invitation i
            where i.survey.id in :surveyIds
            group by i.survey.id
            """)
    List<SurveyCount> countBySurveyIds(@Param("surveyIds") Collection<UUID> surveyIds);
}
