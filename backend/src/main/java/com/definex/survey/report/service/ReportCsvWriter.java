package com.definex.survey.report.service;

import com.definex.survey.participant.dto.ParticipantSummary;
import com.definex.survey.survey.dto.QuestionResponse;
import com.definex.survey.survey.dto.SurveyDetailResponse;
import com.definex.survey.survey.entity.ResponseMode;
import com.definex.survey.user.service.ResponseQueryService;
import com.definex.survey.user.service.ResponseQueryService.AnswerRecord;
import com.definex.survey.user.service.ResponseQueryService.ResponseRecord;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ham yanıtlar CSV'si (FR-73, AS-06): her yanıt bir satır, her soru bir sütun. UTF-8 BOM ile yazılır (Excel'de Türkçe
 * karakterler korunur); ayraç {@code ;} (Türkçe Excel varsayılanı), alanlar RFC 4180'e göre tırnaklanır.
 * {@code = + - @} ile başlayan hücrelere {@code '} eklenir (CSV formül enjeksiyonu). Yanıtlar partiler halinde okunup
 * akışa yazılır; tüm anket belleğe alınmaz.
 */
@Component
public class ReportCsvWriter {

    private static final Logger log = LoggerFactory.getLogger(ReportCsvWriter.class);
    private static final char SEPARATOR = ';';
    private static final int BATCH_SIZE = 500;
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Europe/Istanbul"));

    private final ResponseQueryService responses;
    private final ReportService reportService;

    public ReportCsvWriter(ResponseQueryService responses, ReportService reportService) {
        this.responses = responses;
        this.reportService = reportService;
    }

    public void write(SurveyDetailResponse survey, UUID ownerId, OutputStream out) {
        boolean identified = survey.responseMode() == ResponseMode.IDENTIFIED;
        List<QuestionResponse> questions = survey.questions();
        AnswerFormatter formatter = new AnswerFormatter(questions);
        int[] rows = {0};
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            writer.write('﻿');

            List<String> header = new ArrayList<>();
            if (identified) {
                header.add("Ad Soyad");
                header.add("E-posta");
            }
            header.add("Gönderim (Europe/Istanbul)");
            header.add("Süre (sn)");
            questions.forEach(q -> header.add("S" + q.number() + ". " + (q.text() == null ? "" : q.text())));
            writeRow(writer, header);

            responses.forEachBatch(survey.id(), BATCH_SIZE, batch -> {
                Map<UUID, ParticipantSummary> people = identified ? reportService.participantsOf(survey, batch) : Map.of();
                for (ResponseRecord r : batch) {
                    Map<UUID, AnswerRecord> byQuestion = new HashMap<>();
                    r.answers().forEach(a -> byQuestion.put(a.questionId(), a));
                    List<String> cells = new ArrayList<>();
                    if (identified) {
                        ParticipantSummary user = people.get(r.responseId());
                        cells.add(user == null ? "" : user.fullName());
                        cells.add(user == null ? "" : user.email());
                    }
                    cells.add(TIME.format(r.submittedAt()));
                    cells.add(r.durationSeconds() == null ? "" : String.valueOf(r.durationSeconds()));
                    for (QuestionResponse q : questions) {
                        AnswerRecord a = byQuestion.get(q.id());
                        cells.add(a == null ? "" : formatter.format(a));
                    }
                    writeRow(writer, cells);
                    rows[0]++;
                }
            });
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // NFR-07: denetim kaydı tablosu gelene kadar log satırı (kişisel veri ve içerik yazılmaz).
        log.info("Rapor CSV indirildi: survey={} owner={} rows={}", survey.id(), ownerId, rows[0]);
    }

    private static void writeRow(Writer writer, List<String> cells) {
        try {
            for (int i = 0; i < cells.size(); i++) {
                if (i > 0) {
                    writer.write(SEPARATOR);
                }
                writer.write(escape(cells.get(i)));
            }
            writer.write("\r\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String v = value;
        char first = v.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            v = "'" + v;
        }
        boolean quote = v.indexOf(SEPARATOR) >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0;
        return quote ? '"' + v.replace("\"", "\"\"") + '"' : v;
    }
}
