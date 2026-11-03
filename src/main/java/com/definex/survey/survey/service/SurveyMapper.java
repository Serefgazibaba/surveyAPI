package com.definex.survey.survey.service;

import com.definex.survey.survey.dto.OptionResponse;
import com.definex.survey.survey.dto.QuestionResponse;
import com.definex.survey.survey.dto.SurveyDetailResponse;
import com.definex.survey.survey.dto.SurveySummaryResponse;
import com.definex.survey.survey.entity.Question;
import com.definex.survey.survey.entity.Survey;
import java.util.List;

/** Entity → DTO dönüşümleri. Transaction içinde çağrılmalıdır (sorular lazy yüklenir). */
final class SurveyMapper {

    private SurveyMapper() {
    }

    static SurveyDetailResponse toDetail(Survey survey) {
        List<QuestionResponse> questions = survey.getQuestions().stream()
                .map(SurveyMapper::toQuestion)
                .toList();
        return new SurveyDetailResponse(
                survey.getId(),
                survey.getTitle(),
                survey.getDescription(),
                survey.getStatus(),
                survey.getResponseMode(),
                survey.getEndsAt(),
                survey.getPublishedAt(),
                SurveyActionPolicy.allowedActions(survey.getStatus()),
                survey.getCreatedAt(),
                survey.getUpdatedAt(),
                questions);
    }

    static SurveySummaryResponse toSummary(Survey survey, long questionCount) {
        return new SurveySummaryResponse(
                survey.getId(),
                survey.getTitle(),
                survey.getStatus(),
                survey.getResponseMode(),
                survey.getEndsAt(),
                questionCount,
                SurveyActionPolicy.allowedActions(survey.getStatus()),
                survey.getCreatedAt(),
                survey.getUpdatedAt());
    }

    private static QuestionResponse toQuestion(Question question) {
        List<OptionResponse> options = question.getOptions().stream()
                .map(option -> new OptionResponse(option.getId(), option.getLabel(), option.getSortOrder()))
                .toList();
        return new QuestionResponse(
                question.getId(),
                question.getSortOrder() + 1,
                question.getText(),
                question.getType(),
                question.isRequired(),
                question.getSettings(),
                options);
    }
}
