package com.definex.survey.survey.dto;

import com.definex.survey.common.dto.PageMeta;
import java.util.List;

public record SurveyListResponse(List<SurveySummaryResponse> items, PageMeta page) {
}
