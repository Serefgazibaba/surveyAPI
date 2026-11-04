package com.definex.survey.user.repository;

import com.definex.survey.user.entity.Answer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Rapor sorguları (salt okuma). Dağılımlar veritabanında {@code GROUP BY} ile hesaplanır; yanıtlar belleğe
 * entity olarak yüklenmez. Yanıtlanmamış sorular için cevap satırı yoktur.
 */
public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    /** Satır: [questionId, count] — soruyu yanıtlayan kişi sayısı. */
    @Query("select a.questionId, count(a) from Answer a join a.response r where r.surveyId = :surveyId group by a.questionId")
    List<Object[]> countAnswered(@Param("surveyId") UUID surveyId);

    /** Satır: [questionId, numericValue, count] — Likert dağılımı. */
    @Query("""
            select a.questionId, a.numericValue, count(a) from Answer a join a.response r
            where r.surveyId = :surveyId and a.numericValue is not null
            group by a.questionId, a.numericValue""")
    List<Object[]> countNumeric(@Param("surveyId") UUID surveyId);

    /** Satır: [questionId, optionId, count] — seçenek başına seçen kişi sayısı. */
    @Query("""
            select a.questionId, o, count(a) from Answer a join a.response r join a.optionIds o
            where r.surveyId = :surveyId
            group by a.questionId, o""")
    List<Object[]> countOptions(@Param("surveyId") UUID surveyId);

    /** Satır: [questionId, textValue, submittedAt] — serbest metin ve "Diğer" metinleri, en yeni önce. */
    @Query("""
            select a.questionId, a.textValue, r.submittedAt from Answer a join a.response r
            where r.surveyId = :surveyId and a.textValue is not null
            order by r.submittedAt desc, r.id""")
    List<Object[]> findTexts(@Param("surveyId") UUID surveyId);

    /** Satır: [responseId, answerId, questionId, numericValue, textValue] — verilen yanıtların cevapları. */
    @Query("""
            select r.id, a.id, a.questionId, a.numericValue, a.textValue from Answer a join a.response r
            where r.id in :responseIds""")
    List<Object[]> findByResponseIds(@Param("responseIds") Collection<UUID> responseIds);

    /** Satır: [answerId, optionId] — verilen yanıtların seçilen seçenekleri. */
    @Query("select a.id, o from Answer a join a.response r join a.optionIds o where r.id in :responseIds")
    List<Object[]> findOptionsByResponseIds(@Param("responseIds") Collection<UUID> responseIds);
}
