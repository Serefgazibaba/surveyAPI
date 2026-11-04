package com.definex.survey.survey.entity;

/** Ekranda hangi aksiyonların aktif olacağı (FR-11). Yalnızca uygulanmış uçların aksiyonları kullanılır. */
public enum SurveyAction {
    EDIT,
    PUBLISH,
    DELETE,
    /** Rapor görüntüleme ve CSV (taslak dışı tüm durumlar). */
    REPORT
}
