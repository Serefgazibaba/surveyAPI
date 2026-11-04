package com.definex.survey.survey.dto;

import com.definex.survey.common.dto.PageMeta;
import com.definex.survey.mail.entity.EmailStatus;
import java.util.List;
import java.util.Map;

/** @param summary durum filtreleri uygulanmadan, mail durumu başına sayılar (sekme rozetleri için) */
public record MailListResponse(List<MailListItemResponse> items, Map<EmailStatus, Long> summary, PageMeta page) {
}
