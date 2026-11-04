package com.definex.survey.template.service;

import com.definex.survey.common.dto.PageMeta;
import com.definex.survey.exception.NotFoundException;
import com.definex.survey.survey.dto.OptionResponse;
import com.definex.survey.survey.dto.OptionSaveInput;
import com.definex.survey.survey.dto.QuestionResponse;
import com.definex.survey.survey.dto.QuestionSaveInput;
import com.definex.survey.survey.dto.SurveyDetailResponse;
import com.definex.survey.survey.dto.SurveySaveRequest;
import com.definex.survey.survey.service.SurveyService;
import com.definex.survey.template.dto.CreateSurveyFromTemplateRequest;
import com.definex.survey.template.dto.SaveAsTemplateRequest;
import com.definex.survey.template.dto.TemplateDetailResponse;
import com.definex.survey.template.dto.TemplateListResponse;
import com.definex.survey.template.dto.TemplateOption;
import com.definex.survey.template.dto.TemplateQuestion;
import com.definex.survey.template.dto.TemplateSaveRequest;
import com.definex.survey.template.dto.TemplateSummaryResponse;
import com.definex.survey.template.entity.Template;
import com.definex.survey.template.repository.TemplateRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Şablon yönetimi (FR-30, FR-31). Her Admin yalnızca kendi şablonlarını görür; başkasının şablonu 404 döner.
 * Anket okuma ve oluşturma {@link SurveyService} üzerinden yapılır.
 */
@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateContentValidator contentValidator;
    private final SurveyService surveyService;

    public TemplateService(TemplateRepository templateRepository, TemplateContentValidator contentValidator,
                           SurveyService surveyService) {
        this.templateRepository = templateRepository;
        this.contentValidator = contentValidator;
        this.surveyService = surveyService;
    }

    /** Anketin o anki başlık, açıklama ve sorularından snapshot alır; katılımcı, tarih ve yanıt modu alınmaz. */
    @Transactional
    public TemplateDetailResponse saveFromSurvey(UUID ownerId, SaveAsTemplateRequest request) {
        SurveyDetailResponse survey = surveyService.get(ownerId, request.surveyId());
        List<TemplateQuestion> content = survey.questions().stream().map(TemplateService::fromSurveyQuestion).toList();

        Template template = new Template(ownerId);
        template.replace(
                isBlank(request.name()) ? survey.title() : request.name().strip(),
                isBlank(request.description()) ? survey.description() : request.description(),
                content);
        return toDetail(templateRepository.saveAndFlush(template));
    }

    /**
     * Upsert: {@code id} yoksa yeni şablon oluşturur, varsa adı, açıklamayı ve içeriği tamamen değiştirir.
     *
     * @return kaydedilen şablon ve yeni oluşturulup oluşturulmadığı
     */
    @Transactional
    public SaveResult save(UUID ownerId, TemplateSaveRequest request) {
        boolean created = request.id() == null;
        Template template = created ? new Template(ownerId) : findOwned(ownerId, request.id());
        List<TemplateQuestion> content = contentValidator.toContent(request.questionsOrEmpty());
        template.replace(request.name().strip(), request.description(), content);
        return new SaveResult(toDetail(templateRepository.saveAndFlush(template)), created);
    }

    @Transactional(readOnly = true)
    public TemplateListResponse list(UUID ownerId, String q, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Template> result = isBlank(q)
                ? templateRepository.findByOwnerId(ownerId, pageable)
                : templateRepository.searchByName(ownerId, likePattern(q), pageable);
        return new TemplateListResponse(result.map(TemplateService::toSummary).getContent(), PageMeta.from(result));
    }

    @Transactional(readOnly = true)
    public TemplateDetailResponse get(UUID ownerId, UUID templateId) {
        return toDetail(findOwned(ownerId, templateId));
    }

    /** Şablondan üretilmiş anketler etkilenmez (FR-31); aralarında bağ tutulmaz. */
    @Transactional
    public void delete(UUID ownerId, UUID templateId) {
        templateRepository.delete(findOwned(ownerId, templateId));
    }

    /** Şablon içeriğiyle yeni taslak anket oluşturur; sorular ve seçenekler yeni kimliklerle yazılır. */
    @Transactional
    public SurveyDetailResponse createSurvey(UUID ownerId, CreateSurveyFromTemplateRequest request) {
        Template template = findOwned(ownerId, request.templateId());
        List<QuestionSaveInput> questions = template.getContent().stream().map(TemplateService::toSurveyQuestion).toList();
        SurveySaveRequest surveyRequest = new SurveySaveRequest(
                null,
                isBlank(request.title()) ? template.getName() : request.title().strip(),
                template.getDescription(),
                null,
                null,
                questions);
        return surveyService.save(ownerId, surveyRequest).survey();
    }

    // ---------------------------------------------------------------- dönüşümler

    private static TemplateQuestion fromSurveyQuestion(QuestionResponse question) {
        List<TemplateOption> options = question.options() == null ? List.of() : question.options().stream()
                .sorted(Comparator.comparingInt(OptionResponse::sortOrder))
                .map(option -> new TemplateOption(option.label()))
                .toList();
        return new TemplateQuestion(question.text(), question.type(), question.required(),
                question.settings() == null ? null : new LinkedHashMap<>(question.settings()), options);
    }

    private static QuestionSaveInput toSurveyQuestion(TemplateQuestion question) {
        List<OptionSaveInput> options = question.optionsOrEmpty().stream()
                .map(option -> new OptionSaveInput(null, option.label()))
                .toList();
        return new QuestionSaveInput(null, question.text(), question.type(), question.required(),
                question.settings() == null ? null : new LinkedHashMap<>(question.settings()),
                options.isEmpty() ? null : new ArrayList<>(options));
    }

    private static TemplateSummaryResponse toSummary(Template template) {
        return new TemplateSummaryResponse(template.getId(), template.getName(), template.getDescription(),
                template.getContent().size(), template.getCreatedAt(), template.getUpdatedAt());
    }

    private static TemplateDetailResponse toDetail(Template template) {
        List<TemplateQuestion> content = template.getContent();
        List<TemplateDetailResponse.Question> questions = IntStream.range(0, content.size())
                .mapToObj(i -> {
                    TemplateQuestion q = content.get(i);
                    List<TemplateOption> opts = q.optionsOrEmpty();
                    List<TemplateDetailResponse.Option> options = IntStream.range(0, opts.size())
                            .mapToObj(j -> new TemplateDetailResponse.Option(opts.get(j).label(), j))
                            .toList();
                    return new TemplateDetailResponse.Question(i + 1, q.text(), q.type(), q.required(), q.settings(),
                            options);
                })
                .toList();
        return new TemplateDetailResponse(template.getId(), template.getName(), template.getDescription(),
                content.size(), template.getCreatedAt(), template.getUpdatedAt(), questions);
    }

    // ---------------------------------------------------------------- yardımcılar

    private Template findOwned(UUID ownerId, UUID templateId) {
        return templateRepository.findByIdAndOwnerId(templateId, ownerId)
                .orElseThrow(() -> new NotFoundException("TEMPLATE_NOT_FOUND", "Şablon bulunamadı"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String likePattern(String q) {
        String escaped = q.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    public record SaveResult(TemplateDetailResponse template, boolean created) {
    }
}
