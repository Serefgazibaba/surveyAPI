/**
 * Anket şablonları (FR-30, FR-31): /admin/templates/*, /admin/saveAsTemplate, /admin/upsertTemplate,
 * /admin/createSurveyFromTemplate. Şablon, anketten bağımsız bir snapshot'tır; survey modülüne yalnızca
 * {@code SurveyService} ve {@code QuestionSettingsValidator} üzerinden erişilir.
 */
package com.definex.survey.template;
