package com.definex.survey.auth.service;

import com.definex.survey.auth.dto.UserListResponse;
import com.definex.survey.auth.dto.UserSummaryResponse;
import com.definex.survey.auth.entity.User;
import com.definex.survey.auth.entity.UserRole;
import com.definex.survey.auth.entity.UserStatus;
import com.definex.survey.auth.repository.UserRepository;
import com.definex.survey.common.dto.PageMeta;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Katılımcı (USER) sorguları. Diğer modüller kullanıcı bilgisine bu service üzerinden erişir;
 * auth'un entity ve repository'leri modül dışına açılmaz.
 */
@Service
public class UserDirectoryService {

    private final UserRepository userRepository;

    public UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Ankete davet edilebilecek kullanıcılar: {@code USER} rolünde ve aktif; ada göre sıralı. */
    @Transactional(readOnly = true)
    public UserListResponse listInvitable(String q, int page, int size) {
        Specification<User> spec = invitable().and(nameOrEmailContains(q));
        Page<User> result = userRepository.findAll(spec,
                PageRequest.of(page - 1, size, Sort.by("fullName").and(Sort.by("id"))));
        return new UserListResponse(result.map(UserSummaryResponse::from).getContent(), PageMeta.from(result));
    }

    /** Verilen id'lerden davet edilebilir olanlar; bulunamayan, USER olmayan veya pasif olanlar haritada yer almaz. */
    @Transactional(readOnly = true)
    public Map<UUID, UserSummaryResponse> findInvitableByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Specification<User> spec = invitable().and((root, query, cb) -> root.get("id").in(ids));
        return userRepository.findAll(spec).stream()
                .map(UserSummaryResponse::from)
                .collect(Collectors.toMap(UserSummaryResponse::id, Function.identity()));
    }

    /**
     * Verilen e-postalardan davet edilebilir (aktif USER) hesaplar; anahtar küçük harfli e-postadır.
     * Elle girilen katılımcı adresi kayıtlı bir kullanıcıya aitse katılımcıyı o kullanıcıya bağlamak için kullanılır.
     */
    @Transactional(readOnly = true)
    public Map<String, UserSummaryResponse> findInvitableByEmails(Collection<String> emails) {
        if (emails.isEmpty()) {
            return Map.of();
        }
        List<String> lower = emails.stream().map(e -> e.toLowerCase(Locale.ROOT)).toList();
        Specification<User> spec = invitable().and((root, query, cb) -> cb.lower(root.get("email")).in(lower));
        return userRepository.findAll(spec).stream()
                .map(UserSummaryResponse::from)
                .collect(Collectors.toMap(u -> u.email().toLowerCase(Locale.ROOT), Function.identity(), (a, b) -> a));
    }

    private static Specification<User> invitable() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("role"), UserRole.USER),
                cb.equal(root.get("status"), UserStatus.ACTIVE));
    }

    private static Specification<User> nameOrEmailContains(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String pattern = "%" + q.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                cb.like(root.get("email"), pattern, '\\'));
    }
}
