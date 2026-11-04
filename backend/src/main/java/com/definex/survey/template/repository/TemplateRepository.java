package com.definex.survey.template.repository;

import com.definex.survey.template.entity.Template;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateRepository extends JpaRepository<Template, UUID> {

    Optional<Template> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Template> findByOwnerId(UUID ownerId, Pageable pageable);

    /** @param pattern küçük harfli, kaçışlı LIKE kalıbı */
    @Query("select t from Template t where t.ownerId = :ownerId and lower(t.name) like :pattern escape '\\'")
    Page<Template> searchByName(@Param("ownerId") UUID ownerId, @Param("pattern") String pattern, Pageable pageable);
}
