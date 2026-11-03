package com.definex.survey.auth.dto;

import com.definex.survey.common.dto.PageMeta;
import java.util.List;

public record UserListResponse(List<UserSummaryResponse> items, PageMeta page) {
}
