package com.definex.survey.auth.repository;

import com.definex.survey.auth.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** {@code email} normalize edilmiş (trim + küçük harf) olmalıdır. */
    Optional<User> findByEmail(String email);
}
