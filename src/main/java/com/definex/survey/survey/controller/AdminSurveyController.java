package com.definex.survey.survey.controller;

import com.definex.survey.exception.ApiException;
import com.definex.survey.exception.FieldErrorItem;
import com.definex.survey.survey.dto.PublishSurveyRequest;
import com.definex.survey.survey.dto.PublishSurveyResponse;
import com.definex.survey.survey.dto.SurveyDetailResponse;
import com.definex.survey.survey.dto.SurveyListResponse;
import com.definex.survey.survey.dto.SurveySaveRequest;
import com.definex.survey.survey.entity.SurveyStatus;
import com.definex.survey.survey.service.SurveyPublishService;
import com.definex.survey.survey.service.SurveyService;
import com.definex.survey.survey.service.SurveyService.SaveResult;
import com.definex.survey.survey.service.SurveyService.SurveySort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@Tag(name = "Admin - Surveys", description = "Anket yönetimi")
public class AdminSurveyController {

    private final SurveyService surveyService;
    private final SurveyPublishService surveyPublishService;

    public AdminSurveyController(SurveyService surveyService, SurveyPublishService surveyPublishService) {
        this.surveyService = surveyService;
        this.surveyPublishService = surveyPublishService;
    }

    @GetMapping("/surveys")
    @Operation(operationId = "adminListSurveys", summary = "Anketleri listele",
            description = "Admin'in anketlerini sayfalı döner. `status` verilmezse ARCHIVED hariç tümü listelenir.")
    public SurveyListResponse list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Başlıkta arama") @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) List<SurveyStatus> status,
            @Parameter(description = "title, endsAt, createdAt, updatedAt; `-` öneki azalan")
            @RequestParam(defaultValue = "-updatedAt") String sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return surveyService.list(ownerId(jwt), q, status, parseSort(sort), page, size);
    }

    @PostMapping("/upsertSurvey")
    @Operation(operationId = "adminUpsertSurvey", summary = "Anketi kaydet (upsert: oluştur / güncelle)",
            description = """
                    Upsert. Gövdede `id` yoksa yeni taslak anket oluşturulur (201), varsa güncellenir (200).
                    Sorular ve seçenekler: `id`'li olan güncellenir, `id`'siz olan eklenir, listede olmayan silinir;
                    sıra dizideki sıradır. Kaydetmede yalnızca `title` zorunludur.""")
    public ResponseEntity<SurveyDetailResponse> upsert(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody SurveySaveRequest request) {
        SaveResult result = surveyService.save(ownerId(jwt), request);
        if (!result.created()) {
            return ResponseEntity.ok(result.survey());
        }
        // Location anketin kendi adresini gösterir (/admin/surveys/{id}), upsert ucunu değil.
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/admin/surveys/{id}").buildAndExpand(result.survey().id()).toUri();
        return ResponseEntity.created(location).body(result.survey());
    }

    @PostMapping("/publishSurvey")
    @Operation(operationId = "adminPublishSurvey", summary = "Anketi yayınla",
            description = """
                    Seçilen katılımcılar (GET /admin/getAllUsers) için davet oluşturur ve anketi DRAFT → PUBLISHED yapar.
                    Önce tamlık kuralları kontrol edilir; eksikler 422 SURVEY_INCOMPLETE ile tek seferde döner.
                    E-posta henüz gönderilmez: davete özel linkler yanıtta döner (geçici).""")
    public PublishSurveyResponse publish(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody PublishSurveyRequest request) {
        return surveyPublishService.publish(ownerId(jwt), request);
    }

    @GetMapping("/surveys/{surveyId}")
    @Operation(operationId = "adminGetSurvey", summary = "Anketi getir", description = "Anketi soruları ve seçenekleriyle döner.")
    public SurveyDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID surveyId) {
        return surveyService.get(ownerId(jwt), surveyId);
    }

    @DeleteMapping("/surveys/{surveyId}")
    @Operation(operationId = "adminDeleteSurvey", summary = "Anketi sil", description = "Yalnızca taslak (DRAFT) anketler silinebilir.")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID surveyId) {
        surveyService.delete(ownerId(jwt), surveyId);
        return ResponseEntity.noContent().build();
    }

    private static UUID ownerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static SurveySort parseSort(String value) {
        SurveySort sort = SurveySort.fromParam(value);
        if (sort == null) {
            String allowed = String.join(", ", Arrays.stream(SurveySort.values()).map(SurveySort::param).toList());
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "İstek doğrulanamadı",
                    List.of(new FieldErrorItem("sort", "INVALID_VALUE", "Geçerli değerler: " + allowed)));
        }
        return sort;
    }
}
