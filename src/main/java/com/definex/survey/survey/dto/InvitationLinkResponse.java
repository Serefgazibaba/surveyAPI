package com.definex.survey.survey.dto;

import java.util.UUID;

/**
 * Davet linki. E-posta gönderimi eklenene kadar geçicidir; link tek başına ankete erişim sağladığı için hassastır.
 */
public record InvitationLinkResponse(UUID userId, String email, String fullName, String invitationUrl) {
}
