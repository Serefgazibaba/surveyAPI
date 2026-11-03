package com.definex.survey.survey.repository;

import com.definex.survey.survey.entity.Survey;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurveyRepository extends JpaRepository<Survey, UUID>, JpaSpecificationExecutor<Survey> {

    /** Sahiplik kontrolü: başka bir admin'in anketi bulunamaz (BR-09). */
    Optional<Survey> findByIdAndOwnerId(UUID id, UUID ownerId);

    /** Liste ekranı için soru sayıları; tek sorguda (N+1 olmadan). */
    @Query("""
            select new com.definex.survey.survey.repository.SurveyQuestionCount(q.survey.id, count(q))
            from Question q
            where q.survey.id in :surveyIds
            group by q.survey.id
            """)
    List<SurveyQuestionCount> countQuestions(@Param("surveyIds") Collection<UUID> surveyIds);
}
