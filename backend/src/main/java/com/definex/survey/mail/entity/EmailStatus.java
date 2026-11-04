package com.definex.survey.mail.entity;

public enum EmailStatus {
    /** Gönderilmeyi bekliyor (commit sonrası arka planda gönderilir). */
    QUEUED,
    /** Gönderiliyor. Uzun süre bu durumda kalırsa takılı sayılır. */
    SENDING,
    SENT,
    /** Gönderilemedi; admin yeniden gönderebilir. */
    FAILED
}
