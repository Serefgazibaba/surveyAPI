package com.definex.survey.report.service;

import com.definex.survey.auth.dto.UserSummaryResponse;
import com.definex.survey.auth.service.UserDirectoryService;
import com.definex.survey.exception.ApiException;
import com.definex.survey.exception.FieldErrorItem;
import com.definex.survey.report.dto.ReportListResponse;
import com.definex.survey.report.dto.SurveyReportResponse;
import com.definex.survey.report.dto.SurveyReportResponse.AnswerValue;
import com.definex.survey.report.dto.SurveyReportResponse.LikertBucket;
import com.definex.survey.report.dto.SurveyReportResponse.OptionCount;
import com.definex.survey.report.dto.SurveyReportResponse.Participant;
import com.definex.survey.report.dto.SurveyReportResponse.ParticipantResponse;
import com.definex.survey.report.dto.SurveyReportResponse.QuestionReport;
import com.definex.survey.report.dto.SurveyReportResponse.Summary;
import com.definex.survey.report.dto.SurveyReportResponse.TextAnswer;
import com.definex.survey.survey.dto.QuestionResponse;
import com.definex.survey.survey.dto.SurveyDetailResponse;
import com.definex.survey.survey.dto.SurveyListResponse;
import com.definex.survey.survey.dto.SurveySummaryResponse;
import com.definex.survey.survey.entity.QuestionType;
import com.definex.survey.survey.entity.ResponseMode;
import com.definex.survey.survey.entity.SurveyStatus;
import com.definex.survey.survey.service.SurveyReportDataService;
import com.definex.survey.survey.service.SurveyReportDataService.InvitationStats;
import com.definex.survey.survey.service.SurveyService;
import com.definex.survey.survey.service.SurveyService.SurveySort;
import com.definex.survey.user.service.ResponseQueryService;
import com.definex.survey.user.service.ResponseQueryService.AnswerAggregates;
import com.definex.survey.user.service.ResponseQueryService.ResponseRecord;
import com.definex.survey.user.service.ResponseQueryService.ResponseStats;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anket raporu (FR-70–74). Kendi tablosu yoktur; anket ve davet verisini survey modülünden, yanıtları user
 * modülünden, katılımcı adlarını auth modülünden okur. Sahiplik {@link SurveyService} üzerinden kontrol edilir.
 * Anonim modda kimlik hiçbir yerde dönmez (BR-06).
 */
@Service
public class ReportService {

    static final Set<SurveyStatus> REPORTABLE =
            EnumSet.of(SurveyStatus.PUBLISHED, SurveyStatus.EXPIRED, SurveyStatus.CLOSED, SurveyStatus.ARCHIVED);

    private final SurveyService surveyService;
    private final SurveyReportDataService surveyData;
    private final ResponseQueryService responses;
    private final UserDirectoryService users;
    private final Clock clock;

    public ReportService(SurveyService surveyService, SurveyReportDataService surveyData,
                         ResponseQueryService responses, UserDirectoryService users, Clock clock) {
        this.surveyService = surveyService;
        this.surveyData = surveyData;
        this.responses = responses;
        this.users = users;
        this.clock = clock;
    }

    /** Taslak olmayan kendi anketleri; {@code statuses} verilmezse raporlanabilir tüm durumlar. */
    @Transactional(readOnly = true)
    public ReportListResponse list(UUID ownerId, String q, Collection<SurveyStatus> statuses, int page, int size) {
        if (statuses != null && statuses.contains(SurveyStatus.DRAFT)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "İstek doğrulanamadı",
                    List.of(new FieldErrorItem("status", "INVALID_VALUE", "Taslak anketlerin raporu yoktur")));
        }
        Collection<SurveyStatus> filter = statuses == null || statuses.isEmpty() ? REPORTABLE : statuses;
        SurveyListResponse surveys = surveyService.list(ownerId, q, filter, SurveySort.UPDATED_AT_DESC, page, size);
        List<UUID> ids = surveys.items().stream().map(SurveySummaryResponse::id).toList();
        Map<UUID, InvitationStats> invitations = surveyData.invitationStats(ids);
        Map<UUID, ResponseStats> stats = responses.stats(ids);

        List<ReportListResponse.Item> items = surveys.items().stream().map(s -> {
            InvitationStats inv = invitations.get(s.id());
            ResponseStats rs = stats.get(s.id());
            long count = rs == null ? 0 : rs.count();
            return new ReportListResponse.Item(s.id(), s.title(), s.status(), s.responseMode(), s.endsAt(),
                    inv.invited(), inv.delivered(), count, rate(count, inv.delivered()),
                    rs == null ? null : rs.lastSubmittedAt());
        }).toList();
        return new ReportListResponse(items, surveys.page());
    }

    @Transactional(readOnly = true)
    public SurveyReportResponse get(UUID ownerId, UUID surveyId) {
        SurveyDetailResponse survey = reportableSurvey(ownerId, surveyId);
        InvitationStats inv = surveyData.invitationStats(List.of(surveyId)).get(surveyId);
        ResponseStats rs = responses.stats(List.of(surveyId)).get(surveyId);
        AnswerAggregates agg = responses.aggregates(surveyId);

        long count = rs == null ? 0 : rs.count();
        List<QuestionReport> questions = survey.questions().stream().map(q -> questionReport(q, agg)).toList();
        Summary summary = new Summary(inv.invited(), inv.delivered(), count, rate(count, inv.delivered()),
                satisfaction(survey.questions(), agg),
                rs == null || rs.averageDurationSeconds() == null ? null : Math.round(rs.averageDurationSeconds()));

        List<ParticipantResponse> participants = survey.responseMode() == ResponseMode.IDENTIFIED
                ? participantResponses(survey, responses.all(surveyId))
                : null;
        return new SurveyReportResponse(survey.id(), survey.title(), survey.description(), survey.status(),
                survey.responseMode(), survey.endsAt(), clock.instant(), summary, questions, participants);
    }

    /** CSV için anket (sahiplik ve durum kontrolü dahil); dosya akışı başlamadan çağrılır. */
    public SurveyDetailResponse reportableSurvey(UUID ownerId, UUID surveyId) {
        SurveyDetailResponse survey = surveyService.get(ownerId, surveyId);
        if (!REPORTABLE.contains(survey.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "REPORT_NOT_AVAILABLE", "Taslak anket için rapor oluşturulamaz");
        }
        return survey;
    }

    /** Kayıtlı modda yanıt → katılımcı; anonim modda boş. */
    Map<UUID, UserSummaryResponse> participantsOf(SurveyDetailResponse survey, List<ResponseRecord> records) {
        if (survey.responseMode() != ResponseMode.IDENTIFIED) {
            return Map.of();
        }
        List<UUID> invitationIds = records.stream().map(ResponseRecord::invitationId).filter(Objects::nonNull).toList();
        Map<UUID, UUID> userByInvitation = surveyData.participantsByInvitation(invitationIds);
        Map<UUID, UserSummaryResponse> userById = users.findSummariesByIds(userByInvitation.values());
        Map<UUID, UserSummaryResponse> result = new HashMap<>();
        for (ResponseRecord r : records) {
            UUID userId = r.invitationId() == null ? null : userByInvitation.get(r.invitationId());
            UserSummaryResponse user = userId == null ? null : userById.get(userId);
            if (user != null) {
                result.put(r.responseId(), user);
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- hesaplar

    private List<ParticipantResponse> participantResponses(SurveyDetailResponse survey, List<ResponseRecord> records) {
        Map<UUID, UserSummaryResponse> people = participantsOf(survey, records);
        AnswerFormatter formatter = new AnswerFormatter(survey.questions());
        Map<UUID, Integer> order = new HashMap<>();
        survey.questions().forEach(q -> order.put(q.id(), q.number()));
        return records.stream().map(r -> {
            UserSummaryResponse user = people.get(r.responseId());
            List<AnswerValue> answers = r.answers().stream()
                    .sorted((a, b) -> Integer.compare(order.getOrDefault(a.questionId(), Integer.MAX_VALUE),
                            order.getOrDefault(b.questionId(), Integer.MAX_VALUE)))
                    .map(a -> new AnswerValue(a.questionId(), formatter.format(a)))
                    .toList();
            return new ParticipantResponse(r.responseId(),
                    user == null ? null : new Participant(user.fullName(), user.email()),
                    r.submittedAt(), r.durationSeconds(), answers);
        }).toList();
    }

    private static QuestionReport questionReport(QuestionResponse q, AnswerAggregates agg) {
        long answered = agg.answered().getOrDefault(q.id(), 0L);
        List<TextAnswer> texts = agg.texts().getOrDefault(q.id(), List.of()).stream()
                .map(t -> new TextAnswer(t.text(), t.submittedAt())).toList();
        QuestionType type = q.type();
        if (type == QuestionType.LIKERT) {
            int scale = scaleOf(q);
            Map<Integer, Long> counts = agg.numeric().getOrDefault(q.id(), Map.of());
            List<LikertBucket> buckets = new ArrayList<>();
            long n = 0;
            long sum = 0;
            for (int v = 1; v <= scale; v++) {
                long c = counts.getOrDefault(v, 0L);
                n += c;
                sum += c * v;
                String label = v == 1 ? setting(q, "minLabel") : v == scale ? setting(q, "maxLabel") : null;
                buckets.add(new LikertBucket(v, label, c, percent(c, answered)));
            }
            Double average = n == 0 ? null : round(sum / (double) n, 2);
            return new QuestionReport(q.id(), q.number(), q.text(), type, answered, scale, average, buckets,
                    null, null, null, null);
        }
        if (type != null && type.hasOptions()) {
            Map<UUID, Long> counts = agg.options().getOrDefault(q.id(), Map.of());
            List<OptionCount> options = AnswerFormatter.sortedOptions(q).stream()
                    .map(o -> {
                        long c = counts.getOrDefault(o.id(), 0L);
                        return new OptionCount(o.id(), o.label(), c, percent(c, answered));
                    }).toList();
            return new QuestionReport(q.id(), q.number(), q.text(), type, answered, null, null, null,
                    options, (long) texts.size(), texts, null);
        }
        return new QuestionReport(q.id(), q.number(), q.text(), type, answered, null, null, null,
                null, null, null, texts);
    }

    /** Tüm Likert cevaplarının 1–5 ölçeğine çevrilmiş ortalaması (farklı ölçekli sorular birlikte ortalanır). */
    private static Double satisfaction(List<QuestionResponse> questions, AnswerAggregates agg) {
        double total = 0;
        long n = 0;
        for (QuestionResponse q : questions) {
            if (q.type() != QuestionType.LIKERT) {
                continue;
            }
            int scale = scaleOf(q);
            for (Map.Entry<Integer, Long> e : agg.numeric().getOrDefault(q.id(), Map.of()).entrySet()) {
                double normalized = scale <= 1 ? 5 : 1 + (e.getKey() - 1) * 4.0 / (scale - 1);
                total += normalized * e.getValue();
                n += e.getValue();
            }
        }
        return n == 0 ? null : round(total / n, 2);
    }

    private static int scaleOf(QuestionResponse q) {
        Object scale = q.settings() == null ? null : q.settings().get("scale");
        return scale instanceof Number number ? number.intValue() : 5;
    }

    private static String setting(QuestionResponse q, String key) {
        Object value = q.settings() == null ? null : q.settings().get(key);
        return value == null ? null : value.toString();
    }

    static double rate(long responses, long delivered) {
        return delivered == 0 ? 0 : round(responses * 100.0 / delivered, 1);
    }

    private static double percent(long count, long total) {
        return total == 0 ? 0 : round(count * 100.0 / total, 1);
    }

    private static double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }
}
