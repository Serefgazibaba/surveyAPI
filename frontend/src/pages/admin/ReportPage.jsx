import { useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import Button from "../../components/Button.jsx";
import IconButton from "../../components/IconButton.jsx";
import Icon from "../../components/Icon.jsx";
import Tabs from "../../components/Tabs.jsx";
import Modal from "../../components/Modal.jsx";
import SearchInput from "../../components/SearchInput.jsx";
import DataTable from "../../components/DataTable.jsx";
import StatusBadge from "../../components/StatusBadge.jsx";
import EmptyState from "../../components/EmptyState.jsx";
import ErrorState from "../../components/ErrorState.jsx";
import Spinner from "../../components/Spinner.jsx";
import { useToast } from "../../components/Toast.jsx";
import useAsync from "../../hooks/useAsync.js";
import { getReport, downloadReportCsv } from "../../api/reports.js";
import { questionTypeLabel } from "../../utils/statusLabels.js";
import { formatDate, formatDateTime, formatDuration } from "../../utils/datetime.js";

const crumbs = [{ label: "Raporlar", to: "/reports" }, { label: "Anket Raporu" }];
const PREVIEW = 5;
const MODAL_PAGE = 20;

const matches = (text, q) => !q || (text || "").toLocaleLowerCase("tr").includes(q.toLocaleLowerCase("tr"));

function Bar({ label, count, percentage, max }) {
  return <div className="bar-row">
    <span>{label}</span>
    <div className="bar-track"><span style={{ width: `${max ? (count / max) * 100 : 0}%` }} /></div>
    <small>{count} · %{percentage}</small>
  </div>;
}

/** Metin cevapları (serbest metin / "Diğer"): arama + sayfalama ön yüzde. */
function TextAnswersModal({ title, answers, onClose }) {
  const [q, setQ] = useState("");
  const [page, setPage] = useState(1);
  const filtered = answers.filter(a => matches(a.text, q));
  const last = Math.max(1, Math.ceil(filtered.length / MODAL_PAGE));
  const current = Math.min(page, last);
  const items = filtered.slice((current - 1) * MODAL_PAGE, current * MODAL_PAGE);
  return <Modal wide title={title} subtitle={`${answers.length} cevap`} onClose={onClose}
    footer={<Button variant="primary" onClick={onClose}>Kapat</Button>}>
    <SearchInput value={q} placeholder="Cevaplarda ara..." onSearch={v => { setQ(v); setPage(1); }} />
    {items.length === 0 && <EmptyState title="Sonuç bulunamadı" text="Farklı bir kelimeyle arayın." />}
    {items.map((a, i) => <div className="free-answer" key={`${current}-${i}`}>
      <p className="answer-text">{a.text}</p><small className="text-muted">{formatDateTime(a.submittedAt)}</small>
    </div>)}
    {filtered.length > MODAL_PAGE && <div className="pager">
      {filtered.length} cevap <span className="grow" /> Sayfa {current} / {last}
      <Button size="sm" aria-label="Önceki sayfa" disabled={current <= 1} onClick={() => setPage(current - 1)}><Icon name="left" /></Button>
      <Button size="sm" aria-label="Sonraki sayfa" disabled={current >= last} onClick={() => setPage(current + 1)}><Icon name="right" /></Button>
    </div>}
  </Modal>;
}

function QuestionCard({ q, onShowTexts }) {
  const heading = <div className="report-heading">
    <div><h2><span className="question-number">{q.number}</span>{q.text || "Soru metni girilmedi"}</h2>
      <small>{questionTypeLabel(q.type)} · {q.answerCount} yanıt</small></div>
    {q.type === "LIKERT" && <div className="average">{q.average ?? "—"}<small>Ortalama / {q.scale}</small></div>}
  </div>;

  if (q.type === "LIKERT") {
    const max = Math.max(0, ...q.distribution.map(b => b.count));
    return <section className="panel report-card">{heading}
      {q.distribution.map(b => <Bar key={b.value} label={b.label ? `${b.value} · ${b.label}` : String(b.value)} count={b.count} percentage={b.percentage} max={max} />)}
    </section>;
  }
  if (q.options) {
    const other = q.otherCount ?? 0;
    const max = Math.max(other, ...q.options.map(o => o.count));
    const otherPct = q.answerCount ? Math.round((other / q.answerCount) * 1000) / 10 : 0;
    return <section className="panel report-card">{heading}
      {q.type === "MULTIPLE_CHOICE" && <p className="small text-muted report-note">Birden çok seçenek seçilebildiği için yüzdelerin toplamı %100'ü aşabilir.</p>}
      {q.options.map(o => <Bar key={o.optionId} label={o.label || "—"} count={o.count} percentage={o.percentage} max={max} />)}
      {other > 0 && <>
        <Bar label="Diğer" count={other} percentage={otherPct} max={max} />
        <Button size="sm" variant="ghost" className="mt" onClick={() => onShowTexts(`${q.number}. ${q.text || ""} · Diğer cevapları`, q.otherAnswers)}>Diğer cevaplarını gör ({other})</Button>
      </>}
    </section>;
  }
  const answers = q.answers || [];
  return <section className="panel report-card">{heading}
    {answers.length === 0 && <p className="text-muted">Bu soruya henüz cevap yazılmadı.</p>}
    {answers.slice(0, PREVIEW).map((a, i) => <div className="free-answer" key={i}><p className="answer-text">{a.text}</p></div>)}
    {answers.length > PREVIEW && <Button size="sm" variant="ghost" className="mt no-print" onClick={() => onShowTexts(`${q.number}. ${q.text || ""}`, answers)}>Tüm cevaplar ({answers.length})</Button>}
  </section>;
}

function ParticipantModal({ response, questions, onClose }) {
  const byId = Object.fromEntries(response.answers.map(a => [a.questionId, a.value]));
  return <Modal wide title={response.participant?.fullName || "Katılımcı"}
    subtitle={`${response.participant?.email || ""} · ${formatDateTime(response.submittedAt)}`} onClose={onClose}
    footer={<Button variant="primary" onClick={onClose}>Kapat</Button>}>
    {questions.map(q => <div key={q.questionId} className="participant-answer">
      <b><span className="question-number">{q.number}</span>{q.text || "Soru metni girilmedi"}</b>
      <p className={byId[q.questionId] ? "answer-text" : "answer-text text-muted"}>{byId[q.questionId] || "Yanıtlanmadı"}</p>
    </div>)}
  </Modal>;
}

function ParticipantsTab({ report }) {
  const [q, setQ] = useState("");
  const [open, setOpen] = useState(null);
  const rows = report.responses.filter(r => matches(r.participant?.fullName, q) || matches(r.participant?.email, q));
  const columns = [
    { key: "name", header: "Katılımcı", className: "title-cell", render: r => <>{r.participant?.fullName || "—"}<small>{r.participant?.email}</small></> },
    { key: "submittedAt", header: "Gönderim", render: r => formatDateTime(r.submittedAt) },
    { key: "duration", header: "Süre", render: r => formatDuration(r.durationSeconds) },
    { key: "answered", header: "Yanıtlanan", render: r => `${r.answers.length} / ${report.questions.length}` },
    { key: "actions", header: "İşlemler", className: "right", render: r => <div className="actions"><IconButton icon="eye" label="Cevapları gör" onClick={() => setOpen(r)} /></div> },
  ];
  return <>
    <div className="toolbar"><SearchInput value={q} placeholder="Ad veya e-posta ara..." onSearch={setQ} /></div>
    <DataTable columns={columns} rows={rows} rowKey={r => r.responseId}
      empty={q ? { title: "Sonuç bulunamadı", text: "Farklı bir ad veya e-posta deneyin." } : { title: "Henüz yanıt gelmedi" }} />
    {open && <ParticipantModal response={open} questions={report.questions} onClose={() => setOpen(null)} />}
  </>;
}

/** S-07 · Anket raporu: özet göstergeler, soru dağılımları, metin cevapları, katılımcı yanıtları (kayıtlı mod). */
export default function ReportPage() {
  const { surveyId } = useParams();
  const toast = useToast();
  const { data: report, error, reload } = useAsync(() => getReport(surveyId), [surveyId]);
  const [tab, setTab] = useState("summary");
  const [texts, setTexts] = useState(null); // { title, answers }
  const [downloading, setDownloading] = useState(false);

  const waiting = useMemo(() => (report ? Math.max(0, report.summary.deliveredInvitationCount - report.summary.responseCount) : 0), [report]);

  if (error) return <Shell><PageHead title="Anket Raporu" crumbs={crumbs} /><ErrorState error={error} onRetry={reload} backTo={{ to: "/reports", label: "Raporlara dön" }} /></Shell>;
  if (!report) return <Shell><Spinner size="lg" label="Rapor hazırlanıyor..." /></Shell>;

  const s = report.summary;
  const hasResponses = s.responseCount > 0;
  const identified = report.responseMode === "IDENTIFIED";

  const download = async () => {
    setDownloading(true);
    try {
      await downloadReportCsv(report.surveyId);
    } catch (err) {
      toast.error(err);
    } finally {
      setDownloading(false);
    }
  };

  return <Shell>
    <PageHead title={report.title} crumbs={crumbs}>
      <Button icon="refresh" className="no-print" onClick={reload}>Yenile</Button>
      <Button icon="download" className="no-print" loading={downloading} disabled={!hasResponses} onClick={download}
        title={hasResponses ? "Ham yanıtları CSV olarak indir" : "İndirilecek yanıt yok"}>CSV İndir</Button>
      <Button variant="primary" icon="download" className="no-print" onClick={() => window.print()}>PDF İndir</Button>
    </PageHead>
    <div className="report-meta">
      <StatusBadge value={report.status} />
      <span className="date"><Icon name="calendar" />Bitiş: {formatDate(report.endsAt)}</span>
      <span>{identified ? "Kayıtlı yanıt" : "Anonim yanıt"}</span>
      <span className="text-muted">Oluşturulma: {formatDateTime(report.generatedAt)}</span>
    </div>

    <div className="kpis">
      <div className="panel kpi hero-kpi"><span>Memnuniyet Ortalaması</span><b>{s.averageSatisfactionScore ?? "—"}</b><small>5 üzerinden</small></div>
      <div className="panel kpi"><span>Katılım Oranı</span><b>%{s.participationRate}</b><small>{s.deliveredInvitationCount} gönderilen davete göre</small></div>
      <div className="panel kpi"><span>Davet / Yanıt</span><b>{s.invitationCount} / {s.responseCount}</b><small>{waiting ? `${waiting} kişi bekleniyor` : "Bekleyen yok"}</small></div>
      <div className="panel kpi"><span>Ort. Tamamlama</span><b>{formatDuration(s.averageCompletionSeconds)}</b><small>ara kayıt yapan yanıtlara göre</small></div>
    </div>

    {identified && hasResponses && <div className="no-print">
      <Tabs value={tab} onChange={setTab} items={[{ value: "summary", label: "Özet" }, { value: "participants", label: "Katılımcı yanıtları", count: report.responses.length }]} />
    </div>}

    {!hasResponses && <section className="panel"><EmptyState title="Henüz yanıt gelmedi" text="Katılımcılar anketi gönderdikçe sonuçlar burada görünür." /></section>}
    {hasResponses && (tab === "participants" && identified
      ? <ParticipantsTab report={report} />
      : report.questions.map(q => <QuestionCard key={q.questionId} q={q} onShowTexts={(title, answers) => setTexts({ title, answers })} />))}

    {texts && <TextAnswersModal title={texts.title} answers={texts.answers} onClose={() => setTexts(null)} />}
  </Shell>;
}
