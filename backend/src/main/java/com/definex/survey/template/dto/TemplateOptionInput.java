package com.definex.survey.template.dto;

import jakarta.validation.constraints.Size;

/** @param label taslakta boş olabilir */
public record TemplateOptionInput(@Size(max = 200) String label) {
}
