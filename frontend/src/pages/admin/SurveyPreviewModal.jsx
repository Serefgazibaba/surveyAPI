import { useState } from "react";
import Modal from "../../components/Modal.jsx";
import Button from "../../components/Button.jsx";
import QuestionInput from "../../components/QuestionInput.jsx";

/** Builder önizlemesi: katılımcının göreceği soru alanları, kaydetmeden ve API'siz. */
export default function SurveyPreviewModal({ form, onClose }) {
  const [values, setValues] = useState({});
  const questions = form.questions.map(q => ({ ...q, id: q.id || q.key, options: q.options.map(o => ({ id: o.id || o.key, label: o.label })) }));
  return <Modal wide title={form.title || "Başlıksız anket"} subtitle="Önizleme: yanıtlar kaydedilmez." onClose={onClose}
    footer={<Button variant="primary" onClick={onClose}>Kapat</Button>}>
    {form.description && <p className="text-muted" style={{ margin: 0 }}>{form.description}</p>}
    {questions.length === 0 && <p className="text-muted">Henüz soru eklenmedi.</p>}
    {questions.map((q, i) => <section key={q.key} className="panel form-card">
      <h2 style={{ marginTop: 0 }}><span className="question-number">{i + 1}</span>{q.text || "Soru metni girilmedi"}{q.required && <em>*</em>}</h2>
      <QuestionInput question={q} value={values[q.id]} onChange={v => setValues(prev => ({ ...prev, [q.id]: v }))} />
    </section>)}
  </Modal>;
}
