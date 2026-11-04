package com.definex.survey.survey.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * @param id    mevcut seçenek için verilir; yeni seçenekte boş bırakılır
 * @param label taslakta boş olabilir
 */
public record OptionSaveInput(UUID id, @Size(max = 200) String label) {
}
