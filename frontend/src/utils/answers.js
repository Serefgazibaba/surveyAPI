// Katılımcı cevapları: form state ⇄ backend AnswerInput { questionId, numericValue, optionIds, otherText, text }.
// "Diğer" seçili olması otherText'in string olmasıyla temsil edilir (boş olabilir).

/** Backend'e gidecek temiz AnswerInput (boş alanlar atılır). Boş cevapta null. */
export function toAnswerInput(question, value) {
  if (!value) return null;
  const out = { questionId: question.id };
  switch (question.type) {
    case "LIKERT":
      if (value.numericValue == null) return null;
      out.numericValue = value.numericValue;
      break;
    case "SINGLE_CHOICE":
    case "MULTIPLE_CHOICE": {
      if (value.optionIds?.length) out.optionIds = value.optionIds;
      const other = value.otherText?.trim();
      if (other) out.otherText = other;
      if (!out.optionIds && !out.otherText) return null;
      break;
    }
    case "FREE_TEXT": {
      const text = value.text?.trim();
      if (!text) return null;
      out.text = text;
      break;
    }
    default:
      return null;
  }
  return out;
}

export function collectAnswers(questions, values) {
  return questions.map(q => toAnswerInput(q, values[q.id])).filter(Boolean);
}

/** Draft/submit cevaplarını form state'ine çevirir. */
export function fromAnswers(answers = []) {
  const map = {};
  answers.forEach(a => {
    map[a.questionId] = {
      numericValue: a.numericValue ?? undefined,
      optionIds: a.optionIds ?? [],
      otherText: a.otherText ?? undefined,
      text: a.text ?? undefined,
    };
  });
  return map;
}

/** İstemci tarafı kontrol (backend kurallarıyla aynı). Hata mesajı veya null. */
export function validateAnswer(question, value) {
  const answer = toAnswerInput(question, value);
  const otherPicked = typeof value?.otherText === "string";
  if (otherPicked && !value.otherText.trim()) return "\"Diğer\" için açıklama yazın";
  if (!answer) return question.required ? "Bu soru zorunludur" : null;
  if (question.type === "MULTIPLE_CHOICE") {
    const count = (answer.optionIds?.length || 0) + (answer.otherText ? 1 : 0);
    const { minSelect = 0, maxSelect } = question.settings || {};
    if (minSelect && count < minSelect) return `En az ${minSelect} seçim yapmalısınız`;
    if (maxSelect && count > maxSelect) return `En fazla ${maxSelect} seçim yapabilirsiniz`;
  }
  return null;
}
