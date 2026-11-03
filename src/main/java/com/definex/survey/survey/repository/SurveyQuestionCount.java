package com.definex.survey.survey.repository;

import java.util.UUID;

public record SurveyQuestionCount(UUID surveyId, long questionCount) {
}
