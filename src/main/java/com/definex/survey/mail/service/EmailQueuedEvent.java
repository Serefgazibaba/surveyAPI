package com.definex.survey.mail.service;

import java.util.UUID;

/** Bir mail kuyruğa alındı; transaction commit edilince gönderilir. */
public record EmailQueuedEvent(UUID emailId) {
}
