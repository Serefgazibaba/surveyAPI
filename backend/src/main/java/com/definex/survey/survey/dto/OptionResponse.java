package com.definex.survey.survey.dto;

import java.util.UUID;

public record OptionResponse(UUID id, String label, int sortOrder) {
}
