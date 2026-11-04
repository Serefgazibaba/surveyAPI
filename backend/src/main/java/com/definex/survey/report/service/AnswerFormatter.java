package com.definex.survey.report.service;

import com.definex.survey.survey.dto.OptionResponse;
import com.definex.survey.survey.dto.QuestionResponse;
import com.definex.survey.survey.entity.QuestionType;
import com.definex.survey.user.service.ResponseQueryService.AnswerRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bir cevabın okunur metni (rapordaki katılımcı yanıtları ve CSV hücreleri aynı biçimi kullanır):
 * Likert → puan; seçimli → seçenek etiketleri (anket sırasıyla, {@code ", "} ile) ve "Diğer: …"; metin → metin.
 */
final class AnswerFormatter {

    private final Map<UUID, QuestionResponse> questions = new HashMap<>();
    private final Map<UUID, Integer> optionOrder = new HashMap<>();

    AnswerFormatter(List<QuestionResponse> surveyQuestions) {
        for (QuestionResponse q : surveyQuestions) {
            questions.put(q.id(), q);
            if (q.options() != null) {
                q.options().forEach(o -> optionOrder.put(o.id(), o.sortOrder()));
            }
        }
    }

    String format(AnswerRecord answer) {
        QuestionResponse q = questions.get(answer.questionId());
        if (q == null || q.type() == null) {
            return answer.textValue() != null ? answer.textValue() : "";
        }
        if (q.type() == QuestionType.LIKERT) {
            return answer.numericValue() == null ? "" : String.valueOf(answer.numericValue());
        }
        if (q.type().hasOptions()) {
            List<String> parts = new ArrayList<>();
            Map<UUID, String> labels = new HashMap<>();
            q.options().forEach(o -> labels.put(o.id(), o.label() == null ? "" : o.label()));
            answer.optionIds().stream()
                    .sorted(Comparator.comparingInt(id -> optionOrder.getOrDefault(id, Integer.MAX_VALUE)))
                    .map(id -> labels.getOrDefault(id, ""))
                    .forEach(parts::add);
            if (answer.textValue() != null) {
                parts.add("Diğer: " + answer.textValue());
            }
            return String.join(", ", parts);
        }
        return answer.textValue() == null ? "" : answer.textValue();
    }

    static List<OptionResponse> sortedOptions(QuestionResponse q) {
        return q.options() == null ? List.of()
                : q.options().stream().sorted(Comparator.comparingInt(OptionResponse::sortOrder)).toList();
    }
}
