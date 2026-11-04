import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import Button from "../../components/Button.jsx";
import Badge from "../../components/Badge.jsx";
import Field from "../../components/Field.jsx";
import TextInput from "../../components/TextInput.jsx";
import TextArea from "../../components/TextArea.jsx";
import Alert from "../../components/Alert.jsx";
import ErrorAlert from "../../components/ErrorAlert.jsx";
import ErrorState from "../../components/ErrorState.jsx";
import Spinner from "../../components/Spinner.jsx";
import { useToast } from "../../components/Toast.jsx";
import useAsync from "../../hooks/useAsync.js";
import { getTemplate, upsertTemplate, createSurveyFromTemplate } from "../../api/templates.js";
import { mapFieldErrors, indexFromPath } from "../../utils/errorMessages.js";
import { emptyTemplate, templateToForm, toTemplatePayload, newQuestion } from "../../utils/surveyForm.js";
import QuestionModal from "./QuestionModal.jsx";
import QuestionList from "./QuestionList.jsx";
import SurveyPreviewModal from "./SurveyPreviewModal.jsx";

const snapshot = form => JSON.stringify(toTemplatePayload(form));
const crumbs = [{ label: "Şablonlar", to: "/templates" }, { label: "Şablon" }];

/** Şablon oluşturma / düzenleme. Anket builder'ıyla aynı soru düzenleyicisi; tarih, yanıt modu ve katılımcı yok. */
export default function TemplateBuilderPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  const preloaded = id && location.state?.detail?.id === id ? location.state.detail : null;
  const initial = preloaded ? templateToForm(preloaded) : id ? null : emptyTemplate();
  const [form, setForm] = useState(initial);
  const [saved, setSaved] = useState(initial ? snapshot(initial) : null);
  const load = useAsync(() => getTemplate(id), [id], { enabled: !!id && !preloaded });
  useEffect(() => {
    if (!load.data) return;
    const f = templateToForm(load.data);
    setForm(f);
    setSaved(snapshot(f));
  }, [load.data]);

  const [saveError, setSaveError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [creating, setCreating] = useState(false);
  const [modal, setModal] = useState(null); // { index|null, question }
  const [preview, setPreview] = useState(false);

  const dirty = !!form && snapshot(form) !== saved;
  useEffect(() => {
    if (!dirty) return;
    const warn = e => { e.preventDefault(); e.returnValue = ""; };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  const fieldErrors = useMemo(() => mapFieldErrors(saveError), [saveError]);
  const rowMessages = useMemo(() => {
    const byIndex = {};
    (saveError?.errors || []).forEach(e => {
      const i = indexFromPath(e.field);
      if (i !== null) (byIndex[i] ||= []).push(e.message);
    });
    return byIndex;
  }, [saveError]);

  if (load.error) return <Shell><PageHead title="Şablon" crumbs={crumbs} /><ErrorState error={load.error} onRetry={load.reload} backTo={{ to: "/templates", label: "Şablonlara dön" }} /></Shell>;
  if (!form) return <Shell><Spinner size="lg" label="Şablon yükleniyor..." /></Shell>;

  const set = patch => setForm(f => ({ ...f, ...patch }));
  const setQuestions = fn => setForm(f => ({ ...f, questions: fn(f.questions) }));

  const save = async ({ quiet = false } = {}) => {
    if (!form.title.trim()) {
      setSaveError({ status: 400, code: "VALIDATION_ERROR", errors: [{ field: "name", code: "NotBlank", message: "Şablon adı boş bırakılamaz" }] });
      return null;
    }
    setSaving(true);
    setSaveError(null);
    try {
      const res = await upsertTemplate(toTemplatePayload(form));
      const f = templateToForm(res);
      setForm(f);
      setSaved(snapshot(f));
      if (!quiet) toast.success("Şablon kaydedildi.");
      if (!form.id) navigate(`/templates/${res.id}/edit`, { replace: true, state: { detail: res } });
      return res;
    } catch (err) {
      setSaveError(err);
      window.scrollTo({ top: 0, behavior: "smooth" });
      return null;
    } finally {
      setSaving(false);
    }
  };

  // Anket sunucudaki şablondan üretilir; kaydedilmemiş değişiklik varsa önce şablon kaydedilir.
  const createSurvey = async () => {
    const res = dirty || !form.id ? await save({ quiet: true }) : form;
    if (!res) return;
    setCreating(true);
    try {
      const survey = await createSurveyFromTemplate(res.id);
      toast.success("Şablondan yeni taslak anket oluşturuldu.");
      navigate(`/surveys/${survey.id}/edit`, { state: { detail: survey } });
    } catch (err) {
      setSaveError(err);
      setCreating(false);
    }
  };

  const openQuestion = index => setModal({ index, question: index === null ? newQuestion(null) : form.questions[index] });
  const applyQuestion = q => {
    setQuestions(list => (modal.index === null ? [...list, q] : list.map((x, i) => (i === modal.index ? q : x))));
    setModal(null);
  };
  const focusError = item => {
    const i = indexFromPath(item.field);
    if (i !== null && form.questions[i]) return openQuestion(i);
    document.getElementById(`field-${item.field}`)?.scrollIntoView({ behavior: "smooth", block: "center" });
  };
  const modalErrors = modal?.index != null
    ? Object.fromEntries(Object.entries(fieldErrors).filter(([k]) => indexFromPath(k) === modal.index).map(([k, v]) => [k.replace(/^questions\[\d+\]\.?/, ""), v]))
    : {};
  const requiredCount = form.questions.filter(q => q.required).length;

  return <Shell>
    <PageHead title={form.id ? "Şablonu Düzenle" : "Yeni Şablon"} crumbs={crumbs}>
      <Button icon="eye" onClick={() => setPreview(true)}>Önizle</Button>
      <Button icon="check" loading={saving} disabled={(form.id && !dirty) || creating} onClick={() => save()}>Şablonu Kaydet</Button>
      <Button variant="primary" icon="plus" loading={creating} disabled={saving} onClick={createSurvey}>Bu şablonla anket oluştur</Button>
    </PageHead>

    <ErrorAlert error={saveError} onItemClick={focusError} />
    <Alert tone="info" title="Şablon">Şablon, anketlerden bağımsız bir kopyadır. Şablonu değiştirmek veya silmek, ondan daha önce oluşturulmuş anketleri etkilemez.</Alert>

    <div className="builder-grid">
      <div>
        <section className="panel section-panel">
          <Field label="Şablon adı" id="name" error={fieldErrors.name}>
            <TextInput maxLength={200} value={form.title} onChange={e => set({ title: e.target.value })} placeholder="Örn. Eğitim Değerlendirme" />
          </Field>
          <Field label="Açıklama" id="description" error={fieldErrors.description}>
            <TextArea maxLength={2000} value={form.description} onChange={e => set({ description: e.target.value })} placeholder="Şablondan oluşturulan ankete açıklama olarak aktarılır" />
          </Field>
        </section>

        <section className="panel section-panel" id="field-questions">
          <div className="section-heading"><Badge tone="gray">Sorular</Badge><b>{form.questions.length} soru</b></div>
          {fieldErrors.questions && <p className="field-error">{fieldErrors.questions}</p>}
          <QuestionList questions={form.questions} setQuestions={setQuestions} structural canEdit
            rowMessages={rowMessages} onOpen={openQuestion} />
        </section>
      </div>

      <aside className="panel settings-panel">
        <h2>Şablon Özeti</h2>
        <div className="inline-between"><span>Soru</span><b>{form.questions.length}</b></div>
        <div className="inline-between"><span>Zorunlu soru</span><b>{requiredCount}</b></div>
        <hr />
        <small>Bitiş tarihi, yanıt modu ve katılımcılar şablonda tutulmaz; şablondan oluşturulan ankette belirlenir.</small>
      </aside>
    </div>

    {modal && <QuestionModal initial={modal.question} isNew={modal.index === null}
      errors={modalErrors} onClose={() => setModal(null)} onSave={applyQuestion} />}
    {preview && <SurveyPreviewModal form={form} onClose={() => setPreview(false)} />}
  </Shell>;
}
