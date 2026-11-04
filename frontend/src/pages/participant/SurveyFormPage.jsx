import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import PublicLayout from "../../components/PublicLayout.jsx";
import Button from "../../components/Button.jsx";
import Icon from "../../components/Icon.jsx";
import Alert from "../../components/Alert.jsx";
import ErrorAlert from "../../components/ErrorAlert.jsx";
import ErrorState from "../../components/ErrorState.jsx";
import FieldError from "../../components/FieldError.jsx";
import Spinner from "../../components/Spinner.jsx";
import QuestionInput from "../../components/QuestionInput.jsx";
import useAsync from "../../hooks/useAsync.js";
import { getParticipantSurvey, saveDraft, submitAnswers } from "../../api/participant.js";
import { tokenStore } from "../../auth/tokenStore.js";
import { collectAnswers, fromAnswers, validateAnswer } from "../../utils/answers.js";
import { formatDateTime, formatTime } from "../../utils/datetime.js";
import { indexFromPath, isParticipantAccessError } from "../../utils/errorMessages.js";

const AUTOSAVE_MS = 2000;

/** Kaydetme durumu göstergesi. */
function SaveIndicator({ state }) {
  if (state.status === "saving") return <span>☁ Kaydediliyor...</span>;
  if (state.status === "saved") return <span>☁ Otomatik kaydedildi {formatTime(state.at)}</span>;
  if (state.status === "error") return <span className="field-error">Taslak kaydedilemedi, cevaplarınız bu sayfada duruyor.</span>;
  return <span />;
}

/** S-08 · Katılımcı formu (tek soru/sayfa, taslaktan devam, otomatik kayıt) */
export default function SurveyFormPage() {
  const { surveyId } = useParams();
  const navigate = useNavigate();
  const session = tokenStore.getParticipant();
  const hasSession = session?.surveyId === surveyId;

  const toAccessError = useCallback(err => {
    tokenStore.clearParticipant();
    navigate("/access-error", { replace: true, state: { status: err.status, code: err.code, surveyTitle: err.surveyTitle } });
  }, [navigate]);

  const survey = useAsync(() => getParticipantSurvey(surveyId), [surveyId], { enabled: hasSession });
  const [values, setValues] = useState({});
  const [index, setIndex] = useState(0);
  const [errors, setErrors] = useState({}); // questionId → mesaj
  const [saveState, setSaveState] = useState({ status: "idle" });
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const ready = useRef(false);
  const timer = useRef(null);

  const data = survey.data;
  const questions = useMemo(() => data?.questions || [], [data]);

  // İlk yükleme: taslak varsa cevaplar ve kalınan soru geri yüklenir.
  useEffect(() => {
    if (!data) return;
    setValues(fromAnswers(data.draft?.answers));
    const i = questions.findIndex(q => q.id === data.draft?.currentQuestionId);
    setIndex(i >= 0 ? i : 0);
    if (data.draft?.savedAt) setSaveState({ status: "saved", at: data.draft.savedAt });
    ready.current = false;
  }, [data]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (survey.error && isParticipantAccessError(survey.error)) toAccessError(survey.error);
  }, [survey.error, toAccessError]);

  const persist = useCallback(async (idx, vals) => {
    clearTimeout(timer.current);
    timer.current = null;
    setSaveState({ status: "saving" });
    try {
      const res = await saveDraft(surveyId, { currentQuestionId: questions[idx]?.id, answers: collectAnswers(questions, vals) });
      setSaveState({ status: "saved", at: res.savedAt });
    } catch (err) {
      if (isParticipantAccessError(err)) return toAccessError(err);
      setSaveState({ status: "error" });
    }
  }, [surveyId, questions, toAccessError]);

  // Cevap değiştikçe 2 sn sonra taslağa kaydet (ilk geri yükleme hariç).
  useEffect(() => {
    if (!data) return;
    if (!ready.current) { ready.current = true; return; }
    clearTimeout(timer.current);
    timer.current = setTimeout(() => persist(index, values), AUTOSAVE_MS);
    return () => clearTimeout(timer.current);
  }, [values]); // eslint-disable-line react-hooks/exhaustive-deps

  // Sayfa kapanırken/gizlenirken bekleyen otomatik kaydı hemen gönder.
  const latest = useRef({});
  latest.current = { index, values };
  useEffect(() => {
    const flush = () => {
      if (!timer.current) return;
      clearTimeout(timer.current);
      timer.current = null;
      const { index: idx, values: vals } = latest.current;
      saveDraft(surveyId, { currentQuestionId: questions[idx]?.id, answers: collectAnswers(questions, vals) }, { keepalive: true }).catch(() => {});
    };
    const onVisibility = () => { if (document.visibilityState === "hidden") flush(); };
    window.addEventListener("pagehide", flush);
    document.addEventListener("visibilitychange", onVisibility);
    return () => { window.removeEventListener("pagehide", flush); document.removeEventListener("visibilitychange", onVisibility); };
  }, [surveyId, questions]);

  if (!hasSession) return <PublicLayout><ErrorState error={{ status: 401, code: "NO_PARTICIPANT_SESSION" }} /></PublicLayout>;
  if (survey.error) {
    return <PublicLayout>{!isParticipantAccessError(survey.error) && <ErrorState error={survey.error} onRetry={survey.reload} />}</PublicLayout>;
  }
  if (!data) return <PublicLayout><Spinner size="lg" label="Anket yükleniyor..." /></PublicLayout>;

  const question = questions[index];
  const total = questions.length;
  const isLast = index === total - 1;
  const pct = total ? Math.round(((index + 1) / total) * 100) : 0;

  const setAnswer = v => {
    setValues(prev => ({ ...prev, [question.id]: v }));
    setErrors(prev => ({ ...prev, [question.id]: undefined }));
  };

  const go = next => {
    if (next > index) {
      const msg = validateAnswer(question, values[question.id]);
      if (msg) return setErrors(prev => ({ ...prev, [question.id]: msg }));
    }
    setIndex(next);
    persist(next, values);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const submit = async () => {
    const localErrors = {};
    questions.forEach(q => { const m = validateAnswer(q, values[q.id]); if (m) localErrors[q.id] = m; });
    const firstInvalid = questions.findIndex(q => localErrors[q.id]);
    if (firstInvalid >= 0) {
      setErrors(localErrors);
      setIndex(firstInvalid);
      return;
    }
    clearTimeout(timer.current);
    setSubmitting(true);
    setSubmitError(null);
    const answers = collectAnswers(questions, values);
    try {
      const res = await submitAnswers(surveyId, answers);
      tokenStore.clearParticipant();
      navigate(`/f/${surveyId}/thanks`, { replace: true, state: { message: res.message, title: data.title } });
    } catch (err) {
      if (isParticipantAccessError(err)) return toAccessError(err);
      // REQUIRED → field = soru id; INVALID_ANSWERS → field = answers[i].… (gönderilen dizide)
      const serverErrors = {};
      (err.errors || []).forEach(e => {
        const i = indexFromPath(e.field, "answers");
        const qid = i !== null ? answers[i]?.questionId : e.field;
        if (qid && !serverErrors[qid]) serverErrors[qid] = e.message;
      });
      setErrors(serverErrors);
      const first = questions.findIndex(q => serverErrors[q.id]);
      if (first >= 0) setIndex(first);
      setSubmitError(err);
    } finally {
      setSubmitting(false);
    }
  };

  return <PublicLayout>
    <h1>{data.title}</h1>
    <p className="lead">
      {data.description}
      {data.anonymousNotice && <> <b>{data.anonymousNotice}</b></>}
      {data.endsAt && <><br /><small>Son yanıt tarihi: {formatDateTime(data.endsAt)}</small></>}
    </p>
    {total === 0 ? <Alert tone="info" title="Bu ankette soru bulunmuyor." /> : <>
      <div className="progress-label">{index + 1} / {total} <div className="progress"><span style={{ width: `${pct}%` }} /></div> %{pct}</div>
      <ErrorAlert error={submitError} hideItems />
      <section className="panel form-card">
        <h2><span className="question-number">{question.number ?? index + 1}</span>{question.text}{question.required && <em>*</em>}</h2>
        <QuestionInput key={question.id} question={question} value={values[question.id]} onChange={setAnswer} disabled={submitting} />
        <div className="mt"><FieldError message={errors[question.id]} /></div>
        <div className="form-footer">
          <SaveIndicator state={saveState} />
          <div className="grow-actions">
            {index > 0 && <Button icon="left" disabled={submitting} onClick={() => go(index - 1)}>Geri</Button>}
            {isLast
              ? <Button variant="primary" icon="send" loading={submitting} onClick={submit}>Gönder</Button>
              : <Button variant="primary" onClick={() => go(index + 1)}>İleri <Icon name="right" /></Button>}
          </div>
        </div>
      </section>
    </>}
  </PublicLayout>;
}
