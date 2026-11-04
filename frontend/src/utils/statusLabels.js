// Backend enum değerleri → Türkçe etiket ve rozet rengi.

const TABLES = {
  survey: {
    DRAFT: { label: "Taslak", tone: "gray" },
    PUBLISHED: { label: "Yayında", tone: "blue" },
    EXPIRED: { label: "Süresi Doldu", tone: "amber" },
    CLOSED: { label: "Kapandı", tone: "green" },
    ARCHIVED: { label: "Arşiv", tone: "gray" },
  },
  email: {
    QUEUED: { label: "Kuyrukta", tone: "gray" },
    SENDING: { label: "Gönderiliyor", tone: "blue" },
    SENT: { label: "Gönderildi", tone: "green" },
    FAILED: { label: "Başarısız", tone: "red" },
  },
  invitation: {
    PENDING: { label: "Bekliyor", tone: "gray" },
    STARTED: { label: "Başladı", tone: "blue" },
    COMPLETED: { label: "Tamamladı", tone: "green" },
    CANCELLED: { label: "İptal", tone: "amber" },
  },
};

export function statusMeta(kind, value) {
  return TABLES[kind]?.[value] || { label: value || "—", tone: "gray" };
}

export function statusOptions(kind) {
  return Object.entries(TABLES[kind] || {}).map(([value, meta]) => ({ value, label: meta.label }));
}

export const QUESTION_TYPES = [
  { value: "LIKERT", label: "Likert" },
  { value: "SINGLE_CHOICE", label: "Tekli Seçim" },
  { value: "MULTIPLE_CHOICE", label: "Çoklu Seçim" },
  { value: "FREE_TEXT", label: "Serbest Metin" },
];

export function questionTypeLabel(type) {
  return QUESTION_TYPES.find(t => t.value === type)?.label || "Tip seçilmedi";
}

export const hasOptions = (type) => type === "SINGLE_CHOICE" || type === "MULTIPLE_CHOICE";
