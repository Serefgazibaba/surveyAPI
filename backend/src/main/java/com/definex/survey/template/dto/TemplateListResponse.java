package com.definex.survey.template.dto;

import com.definex.survey.common.dto.PageMeta;
import java.util.List;

public record TemplateListResponse(List<TemplateSummaryResponse> items, PageMeta page) {
}
