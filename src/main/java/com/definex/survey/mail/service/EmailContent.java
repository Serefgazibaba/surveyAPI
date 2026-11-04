package com.definex.survey.mail.service;

/** Mail konusu ve gövdesi; HTML ve düz metin birlikte gönderilir. */
public record EmailContent(String subject, String html, String text) {
}
