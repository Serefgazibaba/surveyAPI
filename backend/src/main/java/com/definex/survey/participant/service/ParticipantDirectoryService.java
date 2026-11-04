package com.definex.survey.participant.service;

import com.definex.survey.auth.dto.UserSummaryResponse;
import com.definex.survey.auth.service.UserDirectoryService;
import com.definex.survey.participant.dto.ParticipantInput;
import com.definex.survey.participant.dto.ParticipantSummary;
import com.definex.survey.participant.entity.Participant;
import com.definex.survey.participant.repository.ParticipantRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Katılımcı kayıtları. Kayıtlı kullanıcılar ve ad + e-postayla eklenen dış kişiler aynı tabloda tutulur; e-posta
 * tekildir. Elle girilen adres aktif bir USER hesabına aitse katılımcı o kullanıcıya bağlanır.
 */
@Service
public class ParticipantDirectoryService {

    private final ParticipantRepository repository;
    private final UserDirectoryService users;

    public ParticipantDirectoryService(ParticipantRepository repository, UserDirectoryService users) {
        this.repository = repository;
        this.users = users;
    }

    /**
     * Davet edilecek katılımcıları bulur veya oluşturur. Önce kayıtlı kullanıcılar, ardından dış kişiler (istek
     * sırasıyla); aynı e-posta bir kez döner. Daha önce eklenmiş bir kişinin adı değiştirilmez.
     *
     * @param registered doğrulanmış kayıtlı kullanıcılar (aktif USER)
     * @param external   ad + e-posta ile eklenen kişiler (e-posta küçük harfli)
     */
    @Transactional
    public List<ParticipantSummary> resolve(List<UserSummaryResponse> registered, List<ParticipantInput> external) {
        // e-posta → (ad, kullanıcı); ilk gelen kazanır
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        for (UserSummaryResponse u : registered) {
            candidates.putIfAbsent(normalize(u.email()), new Candidate(normalize(u.email()), u.fullName(), u.id()));
        }
        List<String> externalEmails = external.stream().map(ParticipantInput::email)
                .filter(e -> !candidates.containsKey(e)).distinct().toList();
        Map<String, UserSummaryResponse> registeredByEmail = users.findInvitableByEmails(externalEmails);
        for (ParticipantInput p : external) {
            UserSummaryResponse user = registeredByEmail.get(p.email());
            candidates.putIfAbsent(p.email(), user != null
                    ? new Candidate(p.email(), user.fullName(), user.id())
                    : new Candidate(p.email(), p.fullName(), null));
        }
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<UUID> userIds = candidates.values().stream().map(Candidate::userId).filter(Objects::nonNull).toList();
        Map<UUID, Participant> byUser = userIds.isEmpty() ? Map.of() : repository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(Participant::getUserId, Function.identity()));
        Map<String, Participant> byEmail = repository.findByEmailIn(candidates.keySet()).stream()
                .collect(Collectors.toMap(Participant::getEmail, Function.identity()));

        List<Participant> result = new ArrayList<>();
        for (Candidate c : candidates.values()) {
            Participant existing = c.userId() != null && byUser.containsKey(c.userId())
                    ? byUser.get(c.userId())
                    : byEmail.get(c.email());
            if (existing == null) {
                existing = new Participant(c.email(), c.fullName(), c.userId());
            } else if (c.userId() != null) {
                existing.linkUser(c.userId());
            }
            result.add(existing);
        }
        return repository.saveAll(result).stream().map(ParticipantDirectoryService::toSummary).toList();
    }

    /** Rapor ve listeler için katılımcı adları. */
    @Transactional(readOnly = true)
    public Map<UUID, ParticipantSummary> findSummariesByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ParticipantSummary> result = new HashMap<>();
        repository.findAllById(ids).forEach(p -> result.put(p.getId(), toSummary(p)));
        return result;
    }

    private static ParticipantSummary toSummary(Participant p) {
        return new ParticipantSummary(p.getId(), p.getFullName(), p.getEmail(), p.getUserId());
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private record Candidate(String email, String fullName, UUID userId) {
    }
}
