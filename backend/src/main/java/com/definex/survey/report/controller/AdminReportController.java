package com.definex.survey.report.controller;

import com.definex.survey.report.dto.ReportListResponse;
import com.definex.survey.report.dto.SurveyReportResponse;
import com.definex.survey.report.service.ReportCsvWriter;
import com.definex.survey.report.service.ReportService;
import com.definex.survey.survey.dto.SurveyDetailResponse;
import com.definex.survey.survey.entity.SurveyStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/admin/reports")
@Validated
@Tag(name = "Admin - Reports", description = "Anket raporu ve CSV dışa aktarım")
public class AdminReportController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ReportService reportService;
    private final ReportCsvWriter csvWriter;

    public AdminReportController(ReportService reportService, ReportCsvWriter csvWriter) {
        this.reportService = reportService;
        this.csvWriter = csvWriter;
    }

    @GetMapping
    @Operation(operationId = "adminListReports", summary = "Tüm raporlar",
            description = "Taslak olmayan kendi anketlerinin özet sayıları (davet, gönderilen, yanıt, katılım oranı); son güncellenen önce.")
    public ReportListResponse list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Anket başlığında arama") @RequestParam(required = false) @Size(max = 100) String q,
            @Parameter(description = "Durum filtresi; verilmezse PUBLISHED, EXPIRED, CLOSED, ARCHIVED")
            @RequestParam(required = false) List<SurveyStatus> status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reportService.list(ownerId(jwt), q, status, page, size);
    }

    @GetMapping("/{surveyId}")
    @Operation(operationId = "adminGetReport", summary = "Rapor detayı",
            description = """
                    Özet göstergeler, soru bazlı dağılımlar, metin cevapları ve (yalnızca kayıtlı modda) katılımcı bazlı
                    yanıtlar. Taslak anket için 409 REPORT_NOT_AVAILABLE.""")
    public SurveyReportResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID surveyId) {
        return reportService.get(ownerId(jwt), surveyId);
    }

    @GetMapping(value = "/{surveyId}/download", produces = "text/csv")
    @Operation(operationId = "adminDownloadReport", summary = "Raporu kaydet (CSV)",
            description = """
                    Ham yanıtlar CSV olarak iner: her yanıt bir satır, her soru bir sütun; UTF-8 BOM, `;` ayraç.
                    Kayıtlı modda ilk sütunlar ad ve e-postadır; anonim modda kimlik sütunu yoktur.""")
    public ResponseEntity<StreamingResponseBody> download(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID surveyId) {
        UUID ownerId = ownerId(jwt);
        // Sahiplik ve durum kontrolü akış başlamadan yapılır; hata ProblemDetail olarak döner.
        SurveyDetailResponse survey = reportService.reportableSurvey(ownerId, surveyId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(slug(survey.title()) + "-yanitlar.csv", StandardCharsets.UTF_8)
                .build();
        StreamingResponseBody body = out -> csvWriter.write(survey, ownerId, out);
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }

    private static UUID ownerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    /** "2026 Q4 Çalışan Memnuniyeti" → "2026-q4-calisan-memnuniyeti" */
    static String slug(String title) {
        String ascii = Normalizer.normalize(title.replace('ı', 'i').replace('İ', 'I'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.length() > 60) {
            slug = slug.substring(0, 60).replaceAll("-$", "");
        }
        return slug.isEmpty() ? "anket" : slug;
    }
}
