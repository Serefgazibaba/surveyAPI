package com.definex.survey.survey.service;

import com.definex.survey.common.dto.PageMeta;
import com.definex.survey.exception.ApiException;
import com.definex.survey.exception.NotFoundException;
import com.definex.survey.survey.dto.SurveyDetailResponse;
import com.definex.survey.survey.dto.SurveyListResponse;
import com.definex.survey.survey.dto.SurveySaveRequest;
import com.definex.survey.survey.entity.Survey;
import com.definex.survey.survey.entity.SurveyStatus;
import com.definex.survey.survey.repository.SurveyQuestionCount;
import com.definex.survey.survey.repository.SurveyRepository;
import com.definex.survey.survey.repository.SurveySpecifications;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin anket işlemleri. Admin yalnızca kendi anketlerine erişir; başkasının anketi {@code 404} döner (BR-09).
 */
@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyContentMerger contentMerger;

    public SurveyService(SurveyRepository surveyRepository, SurveyContentMerger contentMerger) {
        this.surveyRepository = surveyRepository;
        this.contentMerger = contentMerger;
    }

    @Transactional(readOnly = true)
    public SurveyListResponse list(UUID ownerId, String q, Collection<SurveyStatus> statuses, SurveySort sort,
                                   int page, int size) {
        Specification<Survey> spec = Specification.where(SurveySpecifications.ownedBy(ownerId))
                .and(SurveySpecifications.statusIn(statuses))
                .and(SurveySpecifications.titleContains(q));
        Page<Survey> result = surveyRepository.findAll(spec, PageRequest.of(page - 1, size, sort.toSort()));

        Map<UUID, Long> counts = result.isEmpty() ? Map.of()
                : surveyRepository.countQuestions(result.map(Survey::getId).getContent()).stream()
                        .collect(Collectors.toMap(SurveyQuestionCount::surveyId, SurveyQuestionCount::questionCount));
        return new SurveyListResponse(
                result.map(survey -> SurveyMapper.toSummary(survey, counts.getOrDefault(survey.getId(), 0L)))
                        .getContent(),
                PageMeta.from(result));
    }

    @Transactional(readOnly = true)
    public SurveyDetailResponse get(UUID ownerId, UUID surveyId) {
        return SurveyMapper.toDetail(findOwned(ownerId, surveyId));
    }

    /**
     * Upsert: {@code id} yoksa yeni taslak anket oluşturur, varsa günceller.
     *
     * @return kaydedilen anket ve yeni oluşturulup oluşturulmadığı
     */
    @Transactional
    public SaveResult save(UUID ownerId, SurveySaveRequest request) {
        boolean created = request.id() == null;
        Survey survey = created ? new Survey(ownerId) : findOwned(ownerId, request.id());
        if (!created && survey.getStatus() != SurveyStatus.DRAFT && survey.getStatus() != SurveyStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "SURVEY_NOT_EDITABLE", "Bu durumdaki anket düzenlenemez");
        }

        contentMerger.apply(survey, request);
        // Yeni soru/seçeneklerin id'leri yanıtta dönsün diye hemen yazılır.
        survey = surveyRepository.saveAndFlush(survey);
        return new SaveResult(SurveyMapper.toDetail(survey), created);
    }

    /** Yalnızca taslak silinir; yayınlanmış ankete yanıt gelmiş olabilir. */
    @Transactional
    public void delete(UUID ownerId, UUID surveyId) {
        Survey survey = findOwned(ownerId, surveyId);
        if (survey.getStatus() != SurveyStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "SURVEY_NOT_DELETABLE", "Yalnızca taslak anketler silinebilir");
        }
        surveyRepository.delete(survey);
    }

    private Survey findOwned(UUID ownerId, UUID surveyId) {
        return surveyRepository.findByIdAndOwnerId(surveyId, ownerId)
                .orElseThrow(() -> new NotFoundException("SURVEY_NOT_FOUND", "Anket bulunamadı"));
    }

    public record SaveResult(SurveyDetailResponse survey, boolean created) {
    }

    /** Liste sıralaması; {@code -} öneki azalan. */
    public enum SurveySort {
        TITLE("title", Sort.Direction.ASC),
        TITLE_DESC("title", Sort.Direction.DESC),
        ENDS_AT("endsAt", Sort.Direction.ASC),
        ENDS_AT_DESC("endsAt", Sort.Direction.DESC),
        CREATED_AT("createdAt", Sort.Direction.ASC),
        CREATED_AT_DESC("createdAt", Sort.Direction.DESC),
        UPDATED_AT("updatedAt", Sort.Direction.ASC),
        UPDATED_AT_DESC("updatedAt", Sort.Direction.DESC);

        private final String property;
        private final Sort.Direction direction;

        SurveySort(String property, Sort.Direction direction) {
            this.property = property;
            this.direction = direction;
        }

        /** {@code -createdAt} → {@link #CREATED_AT_DESC}; tanınmazsa {@code null}. */
        public static SurveySort fromParam(String value) {
            for (SurveySort sort : values()) {
                if (sort.param().equals(value)) {
                    return sort;
                }
            }
            return null;
        }

        public String param() {
            return (direction == Sort.Direction.DESC ? "-" : "") + property;
        }

        Sort toSort() {
            // Aynı değerli kayıtlarda sayfalar arası tutarlılık için id ile ikincil sıralama
            return Sort.by(direction, property).and(Sort.by("id"));
        }
    }
}
