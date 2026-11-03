package com.definex.survey.survey.service;

import com.definex.survey.exception.FieldErrorItem;
import com.definex.survey.survey.entity.QuestionType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Soru tipine göre {@code settings} biçimini doğrular ve eksik alanları varsayılanlarla doldurur.
 * Bilinmeyen alan kabul edilmez. Tamlık kuralları (ör. seçenek sayısına göre {@code maxSelect}) yayınlamada kontrol edilir.
 *
 * <pre>
 * LIKERT          scale 3–10 (5), minLabel ≤100, maxLabel ≤100
 * SINGLE_CHOICE   allowOther (false), display RADIO|DROPDOWN (RADIO)
 * MULTIPLE_CHOICE allowOther (false), minSelect ≥0 (0), maxSelect ≥1 (yok), minSelect ≤ maxSelect
 * FREE_TEXT       multiline (false), maxLength 1–5000 (1000)
 * </pre>
 */
@Component
public class QuestionSettingsValidator {

    private static final Map<QuestionType, Set<String>> ALLOWED = Map.of(
            QuestionType.LIKERT, Set.of("scale", "minLabel", "maxLabel"),
            QuestionType.SINGLE_CHOICE, Set.of("allowOther", "display"),
            QuestionType.MULTIPLE_CHOICE, Set.of("allowOther", "minSelect", "maxSelect"),
            QuestionType.FREE_TEXT, Set.of("multiline", "maxLength"));

    /**
     * @param path   hata yolu öneki, ör. {@code questions[2].settings}
     * @param errors bulunan hatalar buraya eklenir
     * @return varsayılanlarla tamamlanmış ayarlar; tip yoksa {@code null}
     */
    public Map<String, Object> normalize(QuestionType type, Map<String, Object> input, String path,
                                         List<FieldErrorItem> errors) {
        if (type == null) {
            if (input != null && !input.isEmpty()) {
                errors.add(new FieldErrorItem(path, "NOT_ALLOWED", "Soru tipi seçilmeden ayar gönderilemez"));
            }
            return null;
        }
        Map<String, Object> in = input == null ? Map.of() : input;
        in.keySet().stream()
                .filter(key -> !ALLOWED.get(type).contains(key))
                .forEach(key -> errors.add(new FieldErrorItem(path + "." + key, "UNKNOWN_FIELD",
                        type + " sorusu için geçersiz ayar")));

        Reader r = new Reader(in, path, errors);
        Map<String, Object> out = new LinkedHashMap<>();
        switch (type) {
            case LIKERT -> {
                out.put("scale", r.integer("scale", 3, 10, 5));
                out.put("minLabel", r.string("minLabel", 100, "Kesinlikle Katılmıyorum"));
                out.put("maxLabel", r.string("maxLabel", 100, "Kesinlikle Katılıyorum"));
            }
            case SINGLE_CHOICE -> {
                out.put("allowOther", r.bool("allowOther", false));
                out.put("display", r.oneOf("display", Set.of("RADIO", "DROPDOWN"), "RADIO"));
            }
            case MULTIPLE_CHOICE -> {
                out.put("allowOther", r.bool("allowOther", false));
                Integer min = r.integer("minSelect", 0, Integer.MAX_VALUE, 0);
                Integer max = r.integer("maxSelect", 1, Integer.MAX_VALUE, null);
                out.put("minSelect", min);
                if (max != null) {
                    out.put("maxSelect", max);
                    if (min != null && min > max) {
                        errors.add(new FieldErrorItem(path + ".minSelect", "OUT_OF_RANGE",
                                "minSelect, maxSelect değerinden büyük olamaz"));
                    }
                }
            }
            case FREE_TEXT -> {
                out.put("multiline", r.bool("multiline", false));
                out.put("maxLength", r.integer("maxLength", 1, 5000, 1000));
            }
        }
        return out;
    }

    /** Tek bir ayar haritasından tip kontrollü okuma; hatalı değerde hata ekler ve varsayılanı döner. */
    private record Reader(Map<String, Object> in, String path, List<FieldErrorItem> errors) {

        Integer integer(String key, int min, int max, Integer fallback) {
            Object value = in.get(key);
            if (value == null) {
                return fallback;
            }
            if (!(value instanceof Integer || value instanceof Long)
                    || ((Number) value).longValue() < min || ((Number) value).longValue() > max) {
                errors.add(new FieldErrorItem(path + "." + key, "OUT_OF_RANGE",
                        max == Integer.MAX_VALUE ? "En az " + min + " olan bir tam sayı olmalı"
                                : min + " ile " + max + " arasında bir tam sayı olmalı"));
                return fallback;
            }
            return ((Number) value).intValue();
        }

        Boolean bool(String key, boolean fallback) {
            Object value = in.get(key);
            if (value == null) {
                return fallback;
            }
            if (!(value instanceof Boolean b)) {
                errors.add(new FieldErrorItem(path + "." + key, "INVALID_TYPE", "true veya false olmalı"));
                return fallback;
            }
            return b;
        }

        String string(String key, int maxLength, String fallback) {
            Object value = in.get(key);
            if (value == null) {
                return fallback;
            }
            if (!(value instanceof String s) || s.length() > maxLength) {
                errors.add(new FieldErrorItem(path + "." + key, "INVALID_TYPE",
                        "En fazla " + maxLength + " karakterlik metin olmalı"));
                return fallback;
            }
            return s;
        }

        String oneOf(String key, Set<String> allowed, String fallback) {
            Object value = in.get(key);
            if (value == null) {
                return fallback;
            }
            if (!(value instanceof String s) || !allowed.contains(s)) {
                errors.add(new FieldErrorItem(path + "." + key, "INVALID_VALUE",
                        "Geçerli değerler: " + String.join(", ", allowed.stream().sorted().toList())));
                return fallback;
            }
            return s;
        }
    }
}
