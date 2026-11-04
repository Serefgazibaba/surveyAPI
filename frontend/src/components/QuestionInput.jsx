import Select from "./Select.jsx";
import TextInput from "./TextInput.jsx";
import TextArea from "./TextArea.jsx";

const OTHER = "__other__";

function ChoiceList({ question, value, onChange, multiple, disabled }) {
  const selected = value.optionIds || [];
  const otherPicked = typeof value.otherText === "string";
  const allowOther = question.settings?.allowOther;
  const inputType = multiple ? "checkbox" : "radio";

  const pick = id => {
    if (multiple) {
      onChange({ ...value, optionIds: selected.includes(id) ? selected.filter(x => x !== id) : [...selected, id] });
    } else {
      onChange({ optionIds: [id], otherText: undefined });
    }
  };
  const pickOther = () => {
    if (multiple) onChange({ ...value, otherText: otherPicked ? undefined : "" });
    else onChange({ optionIds: [], otherText: value.otherText ?? "" });
  };

  return <div className="choice-list" role={multiple ? "group" : "radiogroup"}>
    {question.options.map(o => <label key={o.id} className={selected.includes(o.id) ? "chosen" : undefined}>
      <input type={inputType} name={question.id} checked={selected.includes(o.id)} disabled={disabled} onChange={() => pick(o.id)} />
      {o.label}
    </label>)}
    {allowOther && <label className={otherPicked ? "chosen" : undefined}>
      <input type={inputType} name={question.id} checked={otherPicked} disabled={disabled} onChange={pickOther} />
      Diğer
    </label>}
    {allowOther && otherPicked && <TextInput placeholder="Lütfen belirtin" maxLength={500} autoFocus disabled={disabled}
      value={value.otherText} onChange={e => onChange({ ...value, otherText: e.target.value })} />}
  </div>;
}

/**
 * Bir sorunun cevap alanı (tip + settings'e göre). Katılımcı formu ve builder önizlemesi kullanır.
 * value: { numericValue?, optionIds?, otherText?, text? }, onChange(nextValue)
 */
export default function QuestionInput({ question, value = {}, onChange, disabled }) {
  const s = question.settings || {};
  switch (question.type) {
    case "LIKERT": {
      const scale = s.scale || 5;
      return <>
        <div className="likert-options" style={{ gridTemplateColumns: `repeat(${scale}, 1fr)` }} role="radiogroup">
          {Array.from({ length: scale }, (_, i) => i + 1).map(n => <button type="button" key={n} disabled={disabled}
            role="radio" aria-checked={value.numericValue === n}
            className={value.numericValue === n ? "chosen" : undefined} onClick={() => onChange({ numericValue: n })}>
            <span className="radio-dot" />{n}
          </button>)}
        </div>
        <div className="likert-ends"><span>1 · {s.minLabel}</span><span>{scale} · {s.maxLabel}</span></div>
      </>;
    }
    case "SINGLE_CHOICE":
      if (s.display === "DROPDOWN") {
        const current = typeof value.otherText === "string" ? OTHER : value.optionIds?.[0] ?? "";
        const options = question.options.map(o => ({ value: o.id, label: o.label }));
        if (s.allowOther) options.push({ value: OTHER, label: "Diğer" });
        return <>
          <Select placeholder="Seçiniz..." options={options} value={current} disabled={disabled}
            onChange={v => onChange(v === OTHER ? { optionIds: [], otherText: value.otherText ?? "" } : { optionIds: v ? [v] : [], otherText: undefined })} />
          {current === OTHER && <TextInput className="mt" placeholder="Lütfen belirtin" maxLength={500} disabled={disabled}
            value={value.otherText} onChange={e => onChange({ ...value, otherText: e.target.value })} />}
        </>;
      }
      return <ChoiceList question={question} value={value} onChange={onChange} disabled={disabled} />;
    case "MULTIPLE_CHOICE":
      return <ChoiceList question={question} value={value} onChange={onChange} multiple disabled={disabled} />;
    case "FREE_TEXT": {
      const max = s.maxLength || 1000;
      const set = e => onChange({ text: e.target.value });
      return s.multiline
        ? <TextArea placeholder="Cevabınızı yazın..." maxLength={max} showCount value={value.text ?? ""} disabled={disabled} onChange={set} />
        : <><TextInput className="mt" placeholder="Cevabınızı yazın..." maxLength={max} value={value.text ?? ""} disabled={disabled} onChange={set} />
          <div className="char-count">{(value.text ?? "").length} / {max}</div></>;
    }
    default:
      return <p className="text-muted">Bu soru için cevap tipi seçilmemiş.</p>;
  }
}
