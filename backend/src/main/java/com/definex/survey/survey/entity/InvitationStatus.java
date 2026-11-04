package com.definex.survey.survey.entity;

public enum InvitationStatus {
    /** Davet oluşturuldu, katılımcı henüz giriş yapmadı. */
    PENDING,
    /** Katılımcı davet linkiyle giriş yaptı. */
    STARTED,
    /** Yanıt gönderildi; davet bir daha kullanılamaz (BR-01). */
    COMPLETED,
    CANCELLED
}
