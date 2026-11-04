import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import Button from "../../components/Button.jsx";
import Badge from "../../components/Badge.jsx";
import StatusBadge from "../../components/StatusBadge.jsx";
import Field from "../../components/Field.jsx";
import TextInput from "../../components/TextInput.jsx";
import TextArea from "../../components/TextArea.jsx";
import Toggle from "../../components/Toggle.jsx";
import DateTimeInput from "../../components/DateTimeInput.jsx";
import ComingSoon from "../../components/ComingSoon.jsx";
import Alert from "../../components/Alert.jsx";
import ErrorAlert from "../../components/ErrorAlert.jsx";
import ErrorState from "../../components/ErrorState.jsx";
import Spinner from "../../components/Spinner.jsx";
import { useToast } from "../../components/Toast.jsx";
import useAsync from "../../hooks/useAsync.js";
import { getSurvey, upsertSurvey } from "../../api/surveys.js";
import { mapFieldErrors, indexFromPath } from "../../utils/errorMessages.js";
import { saveAsTemplate } from "../../api/templates.js";
import { emptySurvey, toForm, toPayload, newQuestion, editMode } from "../../utils/surveyForm.js";
import QuestionModal from "./QuestionModal.jsx";
import QuestionList from "./QuestionList.jsx";
import SaveAsTemplateModal from "./SaveAsTemplateModal.jsx";
import SurveyPreviewModal from "./SurveyPreviewModal.jsx";

const snapshot = form => JSON.stringify(toPayload(form));
const crumbs = [{ label: "Anketlerim", to: "/surveys" }, { label: "Anket Oluşturma" }];

/** S-03 · Anket oluşturma / düzenleme (S-04 soru modalı dahil) */
export default function SurveyBuilderPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  // Kaydetmeden sonra edit sayfasına geçerken yanıt state ile taşınır; tekrar yüklemeye gerek kalmaz.
  const preloaded = id && location.state?.detail?.id === id ? location.state.detail : null;
  const initial = preloaded ? toForm(preloaded) : id ? null : emptySurvey();
  const [form, setForm] = useState(initial);
  const [saved, setSaved] = useState(initial ? snapshot(initial) : null);
  const load = useAsync(() => getSurvey(id), [id], { enabled: !!id && !preloaded });
  useEffect(() => {
    if (!load.data) return;
    const f = toForm(load.data);
    setForm(f);
    setSaved(snapshot(f));
  }, [load.data]);

  // Katılımcı sayfasından yayın hatasıyla (SURVEY_INCOMPLETE) dönülmüş olabilir.
  const [saveError, setSaveError] = useState(location.state?.publishError ?? null);
  const [saving, setSaving] = useState(false);
  const [modal, setModal] = useState(null); // { index|null, question }
  const [preview, setPreview] = useState(false);
  const [templateModal, setTemplateModal] = useState(false);

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

  if (load.error) return <Shell><PageHead title="Anket" crumbs={crumbs} /><ErrorState error={load.error} onRetry={load.reload} backTo={{ to: "/surveys", label: "Anketlerime dön" }} /></Shell>;
  if (!form) return <Shell><Spinner size="lg" label="Anket yükleniyor..." /></Shell>;

  const mode = editMode(form);
  const structural = mode === "edit";
  const canEdit = mode !== "readOnly";
  const canPublish = !form.id || form.allowedActions.includes("PUBLISH");
  const set = patch => setForm(f => ({ ...f, ...patch }));
  const setQuestions = fn => setForm(f => ({ ...f, questions: fn(f.questions) }));

  const save = async ({ quiet = false } = {}) => {
    if (!form.title.trim()) {
      setSaveError({ status: 400, code: "VALIDATION_ERROR", errors: [{ field: "title", code: "NotBlank", message: "Anket başlığı boş bırakılamaz" }] });
      return null;
    }
    setSaving(true);
    setSaveError(null);
    try {
      const res = await upsertSurvey(toPayload(form));
      const f = toForm(res);
      setForm(f);
      setSaved(snapshot(f));
      if (!quiet) toast.success("Anket kaydedildi.");
      if (!form.id) navigate(`/surveys/${res.id}/edit`, { replace: true, state: { detail: res } });
      return res;
    } catch (err) {
      setSaveError(err);
      window.scrollTo({ top: 0, behavior: "smooth" });
      return null;
    } finally {
      setSaving(false);
    }
  };

  const goToParticipants = async () => {
    const res = dirty || !form.id ? await save({ quiet: true }) : form;
    if (res) navigate(`/surveys/${res.id}/participants`);
  };

  const openQuestion = index => setModal({ index, question: index === null ? newQuestion(null) : form.questions[index] });
  const applyQuestion = q => {
    setQuestions(list => (modal.index === null ? [...list, q] : list.map((x, i) => (i === modal.index ? q : x))));
    setModal(null);
  };
  // Şablon sunucudaki kayıttan alınır; kaydedilmemiş değişiklik varsa önce anket kaydedilir.
  const saveTemplate = async ({ name, description }) => {
    const res = dirty || !form.id ? await save({ quiet: true }) : form;
    if (!res) { setTemplateModal(false); return; }
    const template = await saveAsTemplate({ surveyId: res.id, name, description });
    setTemplateModal(false);
    toast.success(`"${template.name}" şablonu kaydedildi.`);
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
    <PageHead title={form.id ? "Anketi Düzenle" : "Anket Oluşturma"} crumbs={crumbs}>
      <Button icon="eye" onClick={() => setPreview(true)}>Önizle</Button>
      {canEdit && <Button icon="check" loading={saving} disabled={form.id && !dirty} onClick={() => save()}>Anketi Kaydet</Button>}
      {canPublish && <Button variant="primary" icon="send" disabled={saving} onClick={goToParticipants}>Gönder</Button>}
    </PageHead>

    <ErrorAlert error={saveError} onItemClick={focusError} />
    {mode === "textOnly" && <Alert tone="info" title="Anket yayında">Yalnızca başlık, açıklama, soru ve seçenek metinleri değiştirilebilir.</Alert>}
    {mode === "readOnly" && <Alert tone="warning" title="Bu anket artık düzenlenemez">Süresi dolmuş, kapanmış veya arşivlenmiş anketler yalnızca görüntülenebilir.</Alert>}

    <div className="builder-grid">
      <div>
        <section className="panel section-panel">
          <div className="inline-between"><span /><StatusBadge value={form.status} /></div>
          <Field label="Anket başlığı" id="title" error={fieldErrors.title}>
            <TextInput maxLength={200} value={form.title} disabled={!canEdit} onChange={e => set({ title: e.target.value })} placeholder="Örn. Eğitim Değerlendirme Anketi" />
          </Field>
          <Field label="Açıklama" id="description" error={fieldErrors.description}>
            <TextArea maxLength={2000} value={form.description} disabled={!canEdit} onChange={e => set({ description: e.target.value })} placeholder="Katılımcılara gösterilecek kısa açıklama" />
          </Field>
        </section>

        <section className="panel section-panel" id="field-questions">
          <div className="section-heading"><Badge tone="gray">Sorular</Badge><b>{form.questions.length} soru</b></div>
          {fieldErrors.questions && <p className="field-error">{fieldErrors.questions}</p>}
          <QuestionList questions={form.questions} setQuestions={setQuestions} structural={structural} canEdit={canEdit}
            rowMessages={rowMessages} onOpen={openQuestion} />
        </section>
        {structural && <ComingSoon><Button icon="plus" disabled>Yeni Bölüm Ekle</Button></ComingSoon>}
      </div>

      <aside className="panel settings-panel">
        <h2>Anket Ayarları</h2>
        <Field label="Bitiş tarihi ve saati" id="endsAt" error={fieldErrors.endsAt} hint="İstanbul saati">
          <DateTimeInput value={form.endsAt} disabled={!structural} onChange={v => set({ endsAt: v })} />
        </Field>
        <div className="inline-between" id="field-responseMode">
          <div><b>Anonim yanıt</b><small>Yanıtlar kişiyle ilişkilendirilmez</small></div>
          <Toggle label="Anonim yanıt" checked={form.responseMode === "ANONYMOUS"} disabled={!structural}
            onChange={on => set({ responseMode: on ? "ANONYMOUS" : "IDENTIFIED" })} />
        </div>
        {fieldErrors.responseMode && <p className="field-error">{fieldErrors.responseMode}</p>}
        <hr />
        <div className="inline-between"><span>Katılımcılar</span><b>{form.participantCount} kişi</b></div>
        {canPublish && <Button disabled={saving} onClick={goToParticipants}>Katılımcıları Yönet</Button>}
        <hr />
        <div className="inline-between"><span>Soru</span><b>{form.questions.length}</b></div>
        <div className="inline-between"><span>Zorunlu soru</span><b>{requiredCount}</b></div>
        <Button variant="ghost" icon="template" disabled={saving} onClick={() => setTemplateModal(true)}>Şablon olarak kaydet</Button>
      </aside>
    </div>

    {modal && <QuestionModal initial={modal.question} isNew={modal.index === null} textOnly={!structural}
      errors={modalErrors} onClose={() => setModal(null)} onSave={canEdit ? applyQuestion : () => setModal(null)} />}
    {templateModal && <SaveAsTemplateModal defaultName={form.title} defaultDescription={form.description}
      onSave={saveTemplate} onClose={() => setTemplateModal(false)} />}
    {preview && <SurveyPreviewModal form={form} onClose={() => setPreview(false)} />}
  </Shell>;
}
