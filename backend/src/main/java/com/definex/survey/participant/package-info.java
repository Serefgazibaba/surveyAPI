/**
 * Katılımcılar (FR-40): ankete davet edilen kişiler. Uygulamada kayıtlı bir kullanıcı ({@code user_id} dolu) veya
 * yalnızca ad + e-postasıyla eklenen dış kişi olabilir. Aynı e-posta tek katılımcıdır. Bu modülün REST ucu yoktur;
 * survey (yayınlama) ve report modülleri {@code ParticipantDirectoryService} üzerinden kullanır.
 */
package com.definex.survey.participant;
