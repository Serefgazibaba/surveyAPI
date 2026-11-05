import { useState } from "react";
import Modal from "../../components/Modal.jsx";
import Button from "../../components/Button.jsx";
import IconButton from "../../components/IconButton.jsx";
import Field from "../../components/Field.jsx";
import TextArea from "../../components/TextArea.jsx";
import TextInput from "../../components/TextInput.jsx";
import Select from "../../components/Select.jsx";
import Toggle from "../../components/Toggle.jsx";
import Badge from "../../components/Badge.jsx";
import { QUESTION_TYPES, hasOptions } from "../../utils/statusLabels.js";
import { defaultSettings, newOption } from "../../utils/surveyForm.js";

const SCALES = Array.from({ length: 8 }, (_, i) => ({ value: String(i + 3), label: `${i + 3} puan` }));
const DISPLAYS = [{ value: "RADIO", label: "Seçenek listesi" }, { value: "DROPDOWN", label: "Açılır liste" }];
const toInt = v => (v === "" || v == null ? undefined : Number(v));

/** Solda etiket (ve isteğe bağlı açıklama), sağda aç/kapa anahtarı. */
function SwitchRow({ label, hint, checked, disabled, onChange }) {
  return <div className="switch-row">
    <span>{label}{hint && <small>{hint}</small>}</span>
    <Toggle label={typeof label === "string" ? label : undefined} checked={!!checked} disabled={disabled} onChange={onChange} />
  </div>;
}

/**
 * S-04 · Soru oluşturma / düzenleme.
 * textOnly: yayındaki ankette yalnızca soru ve seçenek metinleri değişebilir.
 * errors: { [alanYolu]: mesaj } (questions[i]. öneki atılmış)
 */
export default function QuestionModal({ initial, isNew, textOnly, errors = {}, onSave, onClose }) {
  const [q, setQ] = useState(initial);
  const s = q.settings || {};
  const locked = textOnly;
  const setSetting = (key, value) => setQ(prev => ({ ...prev, settings: { ...prev.settings, [key]: value } }));

  const changeType = type => setQ(prev => ({
    ...prev,
    type: type || null,
    settings: defaultSettings(type),
    options: hasOptions(type) ? (prev.options.length ? prev.options : [newOption(), newOption()]) : [],
  }));
  const setOption = (i, label) => setQ(prev => ({ ...prev, options: prev.options.map((o, j) => (j === i ? { ...o, label } : o)) }));
  const removeOption = i => setQ(prev => ({ ...prev, options: prev.options.filter((_, j) => j !== i) }));
  const moveOption = (i, d) => setQ(prev => {
    const options = [...prev.options];
    [options[i], options[i + d]] = [options[i + d], options[i]];
    return { ...prev, options };
  });

  return <Modal wide title={isNew ? "Yeni Bir Anket Sorusu Oluşturma" : "Soruyu Düzenle"}
    subtitle={locked ? "Anket yayında: yalnızca metinler değiştirilebilir." : undefined}
    onClose={onClose}
    footer={<><Button onClick={onClose}>Vazgeç</Button><Button variant="primary" onClick={() => onSave(q)}>{isNew ? "Oluştur" : "Uygula"}</Button></>}>

    <Field label="Soru" error={errors.text}>
      <TextArea placeholder="Soru metnini yazın..." maxLength={1000} autoFocus value={q.text} onChange={e => setQ({ ...q, text: e.target.value })} />
    </Field>
    <Field label="Cevap Tipi" error={errors.type}>
      <Select placeholder="Cevap Tipi Seçiniz..." options={QUESTION_TYPES} value={q.type} disabled={locked} onChange={changeType} />
    </Field>
    <SwitchRow label="Zorunlu soru" checked={q.required} disabled={locked} onChange={v => setQ({ ...q, required: v })} />

    {q.type === "LIKERT" && <>
      <Field label="Ölçek" error={errors["settings.scale"]}>
        <Select options={SCALES} value={String(s.scale ?? 5)} disabled={locked} onChange={v => setSetting("scale", Number(v))} />
      </Field>
      <div className="grid-2">
        <Field label="En düşük değer etiketi" error={errors["settings.minLabel"]}>
          <TextInput maxLength={100} value={s.minLabel ?? ""} disabled={locked} onChange={e => setSetting("minLabel", e.target.value)} />
        </Field>
        <Field label="En yüksek değer etiketi" error={errors["settings.maxLabel"]}>
          <TextInput maxLength={100} value={s.maxLabel ?? ""} disabled={locked} onChange={e => setSetting("maxLabel", e.target.value)} />
        </Field>
      </div>
    </>}

    {hasOptions(q.type) && <>
      <Field as="div" label={<>Seçenekler <Badge tone="gray">{q.options.length}</Badge></>} error={errors.options} hint="En az iki seçenek gereklidir.">
        <div className="option-list">
          {q.options.map((o, i) => <div className="option-row" key={o.key}>
            <TextInput placeholder={`Seçenek ${i + 1}`} maxLength={200} value={o.label} aria-label={`Seçenek ${i + 1}`}
              onChange={e => setOption(i, e.target.value)} className={errors[`options[${i}].label`] ? "has-error" : ""} />
            {!locked && <>
              <IconButton icon="up" label="Yukarı taşı" disabled={i === 0} onClick={() => moveOption(i, -1)} />
              <IconButton icon="down" label="Aşağı taşı" disabled={i === q.options.length - 1} onClick={() => moveOption(i, 1)} />
              <IconButton icon="trash" label="Seçeneği sil" danger onClick={() => removeOption(i)} />
            </>}
          </div>)}
        </div>
      </Field>
      {!locked && <Button size="sm" icon="plus" disabled={q.options.length >= 100} onClick={() => setQ({ ...q, options: [...q.options, newOption()] })}>Seçenek ekle</Button>}
      <SwitchRow label={'"Diğer" seçeneği'} hint="Katılımcı listede olmayan bir cevabı kendisi yazabilir." checked={s.allowOther} disabled={locked} onChange={v => setSetting("allowOther", v)} />
    </>}

    {q.type === "SINGLE_CHOICE" && <Field label="Görünüm">
      <Select options={DISPLAYS} value={s.display ?? "RADIO"} disabled={locked} onChange={v => setSetting("display", v)} />
    </Field>}

    {q.type === "MULTIPLE_CHOICE" && <div className="grid-2">
      <Field label="En az seçim" error={errors["settings.minSelect"]}>
        <TextInput type="number" min={0} value={s.minSelect ?? 0} disabled={locked} onChange={e => setSetting("minSelect", toInt(e.target.value) ?? 0)} />
      </Field>
      <Field label="En fazla seçim" hint="Boş bırakılırsa sınır yok." error={errors["settings.maxSelect"]}>
        <TextInput type="number" min={1} value={s.maxSelect ?? ""} disabled={locked} onChange={e => setSetting("maxSelect", toInt(e.target.value))} />
      </Field>
    </div>}

    {q.type === "FREE_TEXT" && <>
      <Field label="Maksimum karakter" error={errors["settings.maxLength"]}>
        <TextInput type="number" min={1} max={5000} value={s.maxLength ?? 1000} disabled={locked} onChange={e => setSetting("maxLength", toInt(e.target.value))} />
      </Field>
      <SwitchRow label="Çok satırlı cevap" checked={s.multiline} disabled={locked} onChange={v => setSetting("multiline", v)} />
    </>}
  </Modal>;
}
