package com.definex.survey.survey.repository;

import java.util.UUID;

/** Anket başına sayım (soru, davet). */
public record SurveyCount(UUID surveyId, long count) {
}
