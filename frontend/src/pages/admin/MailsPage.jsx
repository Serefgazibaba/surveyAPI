import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import Tabs from "../../components/Tabs.jsx";
import Button from "../../components/Button.jsx";
import Select from "../../components/Select.jsx";
import SearchInput from "../../components/SearchInput.jsx";
import DataTable from "../../components/DataTable.jsx";
import Pager from "../../components/Pager.jsx";
import Badge from "../../components/Badge.jsx";
import StatusBadge from "../../components/StatusBadge.jsx";
import ErrorAlert from "../../components/ErrorAlert.jsx";
import ConfirmDialog from "../../components/ConfirmDialog.jsx";
import { useToast } from "../../components/Toast.jsx";
import useAsync from "../../hooks/useAsync.js";
import { listMails, resendMails } from "../../api/mails.js";
import { listSurveys } from "../../api/surveys.js";
import { statusOptions } from "../../utils/statusLabels.js";
import { formatDateTime } from "../../utils/datetime.js";
import { indexFromPath } from "../../utils/errorMessages.js";

/** Gönderim durumu: davet e-postaları, başarısız olanları yeniden gönderme. */
export default function MailsPage() {
  const [params, setParams] = useSearchParams();
  const toast = useToast();
  const tab = params.get("emailStatus") || "ALL";
  const surveyId = params.get("surveyId") || "";
  const q = params.get("q") || "";
  const page = Number(params.get("page")) || 1;
  const size = Number(params.get("size")) || 20;

  const mails = useAsync(
    () => listMails({ emailStatus: tab === "ALL" ? undefined : tab, surveyId: surveyId || undefined, q, page, size }),
    [tab, surveyId, q, page, size],
  );
  const surveys = useAsync(() => listSurveys({ size: 100, sort: "-updatedAt" }), []);

  const update = patch => {
    const next = new URLSearchParams(params);
    Object.entries(patch).forEach(([k, v]) => (v && v !== "ALL" ? next.set(k, v) : next.delete(k)));
    if (!("page" in patch)) next.delete("page");
    setParams(next, { replace: true });
    setSelected(new Set());
  };

  const [selected, setSelected] = useState(() => new Set());
  const [confirming, setConfirming] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);
  const [sentIds, setSentIds] = useState([]);
  // MAILS_NOT_RESENDABLE: errors[].field = "emailIds[i]" → satır bazında mesaj
  const rowErrors = useMemo(() => {
    const map = {};
    (error?.errors || []).forEach(e => {
      const emailId = sentIds[indexFromPath(e.field, "emailIds")];
      if (emailId) map[emailId] = e.message;
    });
    return map;
  }, [error, sentIds]);

  const resend = async () => {
    const ids = [...selected];
    setSending(true);
    setError(null);
    setSentIds(ids);
    try {
      const res = await resendMails(ids);
      toast.success(`${res.requeuedCount} e-posta yeniden kuyruğa alındı. Eski davet linkleri artık geçersiz.`);
      setSelected(new Set());
      mails.reload();
    } catch (err) {
      setError(err);
    } finally {
      setSending(false);
      setConfirming(false);
    }
  };

  const summary = mails.data?.summary || {};
  const total = Object.values(summary).reduce((a, b) => a + b, 0);
  const tabs = [{ value: "ALL", label: "Tümü", count: total }, ...statusOptions("email").map(o => ({ ...o, count: summary[o.value] ?? 0 }))];
  const surveyOptions = (surveys.data?.items || []).map(s => ({ value: s.id, label: s.title }));

  const columns = [
    { key: "recipient", header: "Alıcı", className: "title-cell", render: m => <>{m.recipientName}<small>{m.recipientEmail}</small></> },
    { key: "surveyTitle", header: "Anket" },
    { key: "emailStatus", header: "E-posta", render: m => <>
      <StatusBadge kind="email" value={m.emailStatus} />{m.stuck && <> <Badge tone="amber">Takıldı</Badge></>}
    </> },
    { key: "invitationStatus", header: "Davet", render: m => <StatusBadge kind="invitation" value={m.invitationStatus} /> },
    { key: "attemptCount", header: "Deneme", className: "nowrap" },
    { key: "lastAttemptAt", header: "Son Deneme", className: "nowrap", render: m => formatDateTime(m.lastAttemptAt) },
    { key: "lastError", header: "Hata", render: m => {
      const msg = rowErrors[m.emailId] || m.lastError;
      return msg ? <small className={rowErrors[m.emailId] ? "field-error" : "text-muted"}>{msg}</small> : "—";
    } },
  ];

  return <Shell>
    <PageHead title="Gönderim Durumu">
      <Button icon="refresh" onClick={mails.reload} loading={mails.loading && !!mails.data}>Yenile</Button>
      <Button variant="primary" icon="send" disabled={!selected.size} onClick={() => { setError(null); setConfirming(true); }}>
        Seçilenleri yeniden gönder{selected.size > 0 && ` (${selected.size})`}
      </Button>
    </PageHead>
    <Tabs items={tabs} value={tab} onChange={v => update({ emailStatus: v })} />
    <div className="toolbar">
      <SearchInput value={q} placeholder="Alıcı adı veya e-posta ara..." onSearch={v => update({ q: v })} />
      <Select placeholder="Tüm anketler" options={surveyOptions} value={surveyId} onChange={v => update({ surveyId: v })} aria-label="Anket filtresi" />
    </div>
    <ErrorAlert error={error} />
    <DataTable columns={columns} rows={mails.data?.items} rowKey={m => m.emailId}
      loading={mails.loading && !mails.data} error={mails.error} onRetry={mails.reload}
      selection={{ selected, onChange: next => setSelected(new Set([...next].slice(0, 100))), isSelectable: m => m.resendable }}
      rowClassName={m => (rowErrors[m.emailId] ? "row-error" : undefined)}
      empty={{ title: "Kayıt yok", text: "Bu filtrelere uyan e-posta bulunamadı." }} />
    {!mails.error && <Pager meta={mails.data?.page} onChange={p => update(p)} />}
    {confirming && <ConfirmDialog title="E-postaları yeniden gönder"
      message={`${selected.size} davet e-postası yeni bir bağlantıyla yeniden gönderilecek. Önceki bağlantılar geçersiz olur.`}
      confirmLabel="Yeniden gönder" busy={sending} onConfirm={resend} onCancel={() => setConfirming(false)} />}
  </Shell>;
}
