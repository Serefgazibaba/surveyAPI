package com.definex.survey.survey.entity;

/** MVP soru tipleri. Kolon string olarak saklanır; yeni tipler eklenebilir (NFR-20). */
public enum QuestionType {
    LIKERT,
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    FREE_TEXT;

    /** Seçenek ({@code options}) alan tipler. */
    public boolean hasOptions() {
        return this == SINGLE_CHOICE || this == MULTIPLE_CHOICE;
    }
}
