package com.definex.survey.survey.dto;

import com.definex.survey.mail.entity.EmailStatus;
import java.util.List;
import java.util.UUID;

public record ResendMailsResponse(int requeuedCount, List<Item> items) {

    public record Item(UUID emailId, EmailStatus status) {
    }
}
