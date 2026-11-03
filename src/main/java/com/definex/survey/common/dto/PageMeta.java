package com.definex.survey.common.dto;

import org.springframework.data.domain.Page;

/** Sayfalı liste yanıtlarının ortak sayfa bilgisi. {@code page} 1'den başlar. */
public record PageMeta(int page, int size, long totalItems, int totalPages) {

    public static PageMeta from(Page<?> page) {
        return new PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
