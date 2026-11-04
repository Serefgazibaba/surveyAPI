package com.definex.survey.template.controller;

import com.definex.survey.survey.dto.SurveyDetailResponse;
import com.definex.survey.template.dto.CreateSurveyFromTemplateRequest;
import com.definex.survey.template.dto.SaveAsTemplateRequest;
import com.definex.survey.template.dto.TemplateDetailResponse;
import com.definex.survey.template.dto.TemplateListResponse;
import com.definex.survey.template.dto.TemplateSaveRequest;
import com.definex.survey.template.service.TemplateService;
import com.definex.survey.template.service.TemplateService.SaveResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/admin")
@Validated
@Tag(name = "Admin - Templates", description = "Anket şablonları")
public class AdminTemplateController {

    private final TemplateService templateService;

    public AdminTemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping("/saveAsTemplate")
    @Operation(operationId = "adminSaveAsTemplate", summary = "Anketi şablon olarak kaydet",
            description = """
                    Anketin başlık, açıklama ve sorularından yeni şablon oluşturur (201). Katılımcı, bitiş tarihi ve
                    yanıt modu şablona alınmaz. `name` / `description` boşsa anketinkiler kullanılır.""")
    public ResponseEntity<TemplateDetailResponse> saveAsTemplate(@AuthenticationPrincipal Jwt jwt,
                                                                 @Valid @RequestBody SaveAsTemplateRequest request) {
        TemplateDetailResponse template = templateService.saveFromSurvey(ownerId(jwt), request);
        return ResponseEntity.created(location("/admin/templates/{id}", template.id())).body(template);
    }

    @PostMapping("/upsertTemplate")
    @Operation(operationId = "adminUpsertTemplate", summary = "Şablonu kaydet (upsert: oluştur / güncelle)",
            description = """
                    Gövdede `id` yoksa yeni şablon oluşturulur (201), varsa güncellenir (200). `questions` şablonun
                    sorularının tamamıdır; mevcut içerik bu listeyle değiştirilir. Yalnızca `name` zorunludur.""")
    public ResponseEntity<TemplateDetailResponse> upsert(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody TemplateSaveRequest request) {
        SaveResult result = templateService.save(ownerId(jwt), request);
        if (!result.created()) {
            return ResponseEntity.ok(result.template());
        }
        return ResponseEntity.created(location("/admin/templates/{id}", result.template().id())).body(result.template());
    }

    @GetMapping("/templates")
    @Operation(operationId = "adminListTemplates", summary = "Şablonları listele",
            description = "Admin'in kendi şablonlarını son güncellenene göre sayfalı döner.")
    public TemplateListResponse list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Şablon adında arama") @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return templateService.list(ownerId(jwt), q, page, size);
    }

    @GetMapping("/templates/{templateId}")
    @Operation(operationId = "adminGetTemplate", summary = "Şablonu getir", description = "Şablonu sorularıyla döner.")
    public TemplateDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID templateId) {
        return templateService.get(ownerId(jwt), templateId);
    }

    @DeleteMapping("/templates/{templateId}")
    @Operation(operationId = "adminDeleteTemplate", summary = "Şablonu sil",
            description = "Şablondan daha önce oluşturulmuş anketler etkilenmez.")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID templateId) {
        templateService.delete(ownerId(jwt), templateId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/createSurveyFromTemplate")
    @Operation(operationId = "adminCreateSurveyFromTemplate", summary = "Şablondan anket oluştur",
            description = """
                    Şablonun içeriğiyle yeni taslak (DRAFT) anket oluşturur (201). Yanıt `upsertSurvey` ile aynıdır;
                    anket buradan sonra `upsertSurvey` ile düzenlenir. `title` boşsa şablonun adı kullanılır.""")
    public ResponseEntity<SurveyDetailResponse> createSurvey(@AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody CreateSurveyFromTemplateRequest request) {
        SurveyDetailResponse survey = templateService.createSurvey(ownerId(jwt), request);
        return ResponseEntity.created(location("/admin/surveys/{id}", survey.id())).body(survey);
    }

    private static UUID ownerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static URI location(String path, UUID id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).buildAndExpand(id).toUri();
    }
}
