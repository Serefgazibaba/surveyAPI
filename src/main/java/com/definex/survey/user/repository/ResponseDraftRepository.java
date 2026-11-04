package com.definex.survey.user.repository;

import com.definex.survey.user.entity.ResponseDraft;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponseDraftRepository extends JpaRepository<ResponseDraft, UUID> {

    Optional<ResponseDraft> findByInvitationId(UUID invitationId);
}
