import { hasOptions } from "./statusLabels.js";

// Backend SurveyDetailResponse ⇄ builder form state dönüşümleri.
// Form'daki her soru/seçenek bir `key` taşır (React anahtarı); backend `id`'si varsa korunur.

let seq = 0;
const newKey = () => `k${++seq}`;

/** Backend'in QuestionSettingsValidator varsayılanlarıyla aynı. */
export function defaultSettings(type) {
  switch (type) {
    case "LIKERT": return { scale: 5, minLabel: "Kesinlikle Katılmıyorum", maxLabel: "Kesinlikle Katılıyorum" };
    case "SINGLE_CHOICE": return { allowOther: false, display: "RADIO" };
    case "MULTIPLE_CHOICE": return { allowOther: false, minSelect: 0 };
    case "FREE_TEXT": return { multiline: false, maxLength: 1000 };
    default: return undefined;
  }
}

export const newOption = (label = "") => ({ key: newKey(), label });

export function newQuestion(type = "LIKERT") {
  return {
    key: newKey(), text: "", type, required: false,
    settings: defaultSettings(type),
    options: hasOptions(type) ? [newOption(), newOption()] : [],
  };
}

/** Kopya: id'ler atılır, yeni soru olarak eklenir. */
export function cloneQuestion(q) {
  return { ...q, id: undefined, key: newKey(), settings: q.settings ? { ...q.settings } : undefined, options: q.options.map(o => ({ key: newKey(), label: o.label })) };
}

export function emptySurvey() {
  return { id: undefined, title: "", description: "", endsAt: null, responseMode: "ANONYMOUS", status: "DRAFT", allowedActions: ["EDIT", "PUBLISH"], participantCount: 0, questions: [] };
}

export function toForm(detail) {
  return {
    id: detail.id,
    title: detail.title ?? "",
    description: detail.description ?? "",
    endsAt: detail.endsAt ?? null,
    // Mod seçilmemiş taslak (ör. şablondan oluşturulan) yeni anketteki gibi Anonim varsayılanıyla açılır;
    // ilk kaydetmede gönderilir.
    responseMode: detail.responseMode ?? (detail.status === "DRAFT" ? "ANONYMOUS" : null),
    status: detail.status,
    allowedActions: detail.allowedActions ?? [],
    participantCount: detail.participantCount ?? 0,
    questions: (detail.questions ?? []).map(q => ({
      key: newKey(),
      id: q.id,
      text: q.text ?? "",
      type: q.type ?? null,
      required: !!q.required,
      settings: q.settings ?? undefined,
      options: [...(q.options ?? [])].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)).map(o => ({ key: newKey(), id: o.id, label: o.label ?? "" })),
    })),
  };
}

/** Form → POST /admin/upsertSurvey gövdesi. Dizi sırası = gösterim sırası. */
export function toPayload(form) {
  return {
    id: form.id,
    title: form.title.trim(),
    description: form.description?.trim() || null,
    endsAt: form.endsAt || null,
    responseMode: form.responseMode || null,
    questions: form.questions.map(q => ({
      id: q.id,
      text: q.text,
      type: q.type || null,
      required: !!q.required,
      settings: q.type ? q.settings : undefined,
      options: hasOptions(q.type) ? q.options.map(o => ({ id: o.id, label: o.label })) : undefined,
    })),
  };
}

// ---- Şablon ⇄ builder form. Şablon soruları kimlik taşımaz; form `title` alanı şablon adıdır.

export function emptyTemplate() {
  return { id: undefined, title: "", description: "", questions: [] };
}

/** TemplateDetail → form (soru/seçenek id'si yok; yalnızca React anahtarı üretilir). */
export function templateToForm(detail) {
  const { id, title, description, questions } = toForm({ ...detail, title: detail.name });
  return { id, title, description, questions };
}

/** Form → POST /admin/upsertTemplate gövdesi; sorular içeriğin tamamıdır. */
export function toTemplatePayload(form) {
  const { id, title, description, questions } = toPayload(form);
  return {
    id,
    name: title,
    description,
    questions: questions.map(q => ({
      text: q.text, type: q.type, required: q.required, settings: q.settings,
      options: q.options?.map(o => ({ label: o.label })),
    })),
  };
}

/** Düzenleme modu: "edit" (taslak/yeni), "textOnly" (yayında), "readOnly" (diğer). */
export function editMode(form) {
  if (!form.id || form.status === "DRAFT") return "edit";
  if (form.status === "PUBLISHED") return "textOnly";
  return "readOnly";
}
