package com.definex.survey.user.service;

import com.definex.survey.user.repository.AnswerRepository;
import com.definex.survey.user.repository.ResponseRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gönderilmiş yanıtlar üzerinde salt okuma sorguları (rapor modülü için). Ara kayıtlar hiçbir sorguya girmez (BR-03).
 * Sahiplik kontrolü çağıran tarafta yapılmış olmalıdır.
 */
@Service
public class ResponseQueryService {

    private final ResponseRepository responseRepository;
    private final AnswerRepository answerRepository;

    public ResponseQueryService(ResponseRepository responseRepository, AnswerRepository answerRepository) {
        this.responseRepository = responseRepository;
        this.answerRepository = answerRepository;
    }

    /** Anket başına yanıt sayısı, ortalama süre ve son yanıt zamanı; yanıtı olmayan anket haritada yer almaz. */
    @Transactional(readOnly = true)
    public Map<UUID, ResponseStats> stats(Collection<UUID> surveyIds) {
        Map<UUID, ResponseStats> result = new HashMap<>();
        if (surveyIds.isEmpty()) {
            return result;
        }
        for (Object[] row : responseRepository.stats(surveyIds)) {
            Double avg = (Double) row[2];
            result.put((UUID) row[0], new ResponseStats((Long) row[1], avg, (Instant) row[3]));
        }
        return result;
    }

    /** Soru bazlı dağılımlar ve metin cevapları (serbest metin + "Diğer"), tek seferde. */
    @Transactional(readOnly = true)
    public AnswerAggregates aggregates(UUID surveyId) {
        Map<UUID, Long> answered = new HashMap<>();
        answerRepository.countAnswered(surveyId).forEach(r -> answered.put((UUID) r[0], (Long) r[1]));

        Map<UUID, Map<Integer, Long>> numeric = new HashMap<>();
        answerRepository.countNumeric(surveyId).forEach(r ->
                numeric.computeIfAbsent((UUID) r[0], k -> new HashMap<>()).put((Integer) r[1], (Long) r[2]));

        Map<UUID, Map<UUID, Long>> options = new HashMap<>();
        answerRepository.countOptions(surveyId).forEach(r ->
                options.computeIfAbsent((UUID) r[0], k -> new HashMap<>()).put((UUID) r[1], (Long) r[2]));

        Map<UUID, List<TextAnswer>> texts = new HashMap<>();
        answerRepository.findTexts(surveyId).forEach(r ->
                texts.computeIfAbsent((UUID) r[0], k -> new ArrayList<>()).add(new TextAnswer((String) r[1], (Instant) r[2])));

        return new AnswerAggregates(answered, numeric, options, texts);
    }

    /** Tüm yanıtlar, cevaplarıyla; en yeni önce. Büyük anketlerde {@link #forEachBatch} kullanılır. */
    @Transactional(readOnly = true)
    public List<ResponseRecord> all(UUID surveyId) {
        List<ResponseRecord> result = new ArrayList<>();
        forEachBatch(surveyId, 500, result::addAll);
        return result;
    }

    /** Yanıtları cevaplarıyla birlikte partiler halinde verir (CSV akışı için); en yeni önce. */
    @Transactional(readOnly = true)
    public void forEachBatch(UUID surveyId, int batchSize, Consumer<List<ResponseRecord>> consumer) {
        for (int page = 0; ; page++) {
            List<Object[]> headers = responseRepository.findHeaders(surveyId, PageRequest.of(page, batchSize));
            if (headers.isEmpty()) {
                return;
            }
            consumer.accept(load(headers));
            if (headers.size() < batchSize) {
                return;
            }
        }
    }

    private List<ResponseRecord> load(List<Object[]> headers) {
        List<UUID> ids = headers.stream().map(h -> (UUID) h[0]).toList();
        Map<UUID, List<UUID>> optionsByAnswer = new HashMap<>();
        answerRepository.findOptionsByResponseIds(ids).forEach(r ->
                optionsByAnswer.computeIfAbsent((UUID) r[0], k -> new ArrayList<>()).add((UUID) r[1]));
        Map<UUID, List<AnswerRecord>> answersByResponse = new HashMap<>();
        for (Object[] r : answerRepository.findByResponseIds(ids)) {
            answersByResponse.computeIfAbsent((UUID) r[0], k -> new ArrayList<>()).add(new AnswerRecord(
                    (UUID) r[2], (Integer) r[3], (String) r[4], optionsByAnswer.getOrDefault((UUID) r[1], List.of())));
        }
        Map<UUID, ResponseRecord> ordered = new LinkedHashMap<>();
        for (Object[] h : headers) {
            UUID id = (UUID) h[0];
            ordered.put(id, new ResponseRecord(id, (UUID) h[1], (Instant) h[2], (Integer) h[3],
                    answersByResponse.getOrDefault(id, List.of())));
        }
        return new ArrayList<>(ordered.values());
    }

    /** @param averageDurationSeconds süresi olan yanıtların ortalaması; hiçbirinde yoksa {@code null} */
    public record ResponseStats(long count, Double averageDurationSeconds, Instant lastSubmittedAt) {
    }

    public record TextAnswer(String text, Instant submittedAt) {
    }

    /**
     * @param answered soru → yanıtlayan kişi sayısı
     * @param numeric  soru → (Likert değeri → adet)
     * @param options  soru → (seçenek → seçen kişi sayısı)
     * @param texts    soru → metin cevapları (serbest metin ve "Diğer"), en yeni önce
     */
    public record AnswerAggregates(
            Map<UUID, Long> answered,
            Map<UUID, Map<Integer, Long>> numeric,
            Map<UUID, Map<UUID, Long>> options,
            Map<UUID, List<TextAnswer>> texts) {
    }

    /** @param invitationId yalnızca kayıtlı modda dolu (BR-06) */
    public record ResponseRecord(UUID responseId, UUID invitationId, Instant submittedAt, Integer durationSeconds,
                                 List<AnswerRecord> answers) {
    }

    public record AnswerRecord(UUID questionId, Integer numericValue, String textValue, List<UUID> optionIds) {
    }
}
