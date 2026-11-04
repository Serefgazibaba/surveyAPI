package com.definex.survey.user.repository;

import com.definex.survey.user.entity.Response;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponseRepository extends JpaRepository<Response, UUID> {
}
