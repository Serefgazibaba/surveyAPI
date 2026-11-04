package com.definex.survey.participant.repository;

import com.definex.survey.participant.entity.Participant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    List<Participant> findByEmailIn(Collection<String> emails);

    List<Participant> findByUserIdIn(Collection<UUID> userIds);
}
