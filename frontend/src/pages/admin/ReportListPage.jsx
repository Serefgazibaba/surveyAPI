import { Link, useSearchParams } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import IconButton from "../../components/IconButton.jsx";
import Icon from "../../components/Icon.jsx";
import SearchInput from "../../components/SearchInput.jsx";
import FilterMenu from "../../components/FilterMenu.jsx";
import DataTable from "../../components/DataTable.jsx";
import Pager from "../../components/Pager.jsx";
import StatusBadge from "../../components/StatusBadge.jsx";
import useAsync from "../../hooks/useAsync.js";
import { listReports } from "../../api/reports.js";
import { statusOptions } from "../../utils/statusLabels.js";
import { formatDate, formatDateTime } from "../../utils/datetime.js";

const REPORTABLE = ["PUBLISHED", "EXPIRED", "CLOSED", "ARCHIVED"];

/** Raporlar: taslak olmayan anketlerin katılım özetleri. */
export default function ReportListPage() {
  const [params, setParams] = useSearchParams();
  const q = params.get("q") || "";
  const status = params.getAll("status");
  const page = Number(params.get("page")) || 1;
  const size = Number(params.get("size")) || 20;
  const { data, error, loading, reload } = useAsync(() => listReports({ q, status, page, size }), [q, status.join(","), page, size]);

  const update = patch => {
    const next = new URLSearchParams(params);
    Object.entries(patch).forEach(([k, v]) => {
      next.delete(k);
      (Array.isArray(v) ? v : [v]).forEach(x => { if (x !== undefined && x !== null && x !== "") next.append(k, x); });
    });
    if (!("page" in patch)) next.delete("page");
    setParams(next, { replace: true });
  };

  const columns = [
    { key: "title", header: "Anket", className: "title-cell", render: r => <><Link to={`/reports/${r.surveyId}`}>{r.title}</Link><small>{r.responseMode === "ANONYMOUS" ? "Anonim" : "Kayıtlı"}</small></> },
    { key: "status", header: "Durum", render: r => <StatusBadge value={r.status} /> },
    { key: "endsAt", header: "Bitiş Tarihi", render: r => <span className="date"><Icon name="calendar" />{formatDate(r.endsAt)}</span> },
    { key: "responses", header: "Davet / Yanıt", render: r => `${r.invitationCount} / ${r.responseCount}` },
    {
      key: "rate", header: "Katılım", render: r => <div className="response">
        <div className="progress" aria-hidden="true"><span style={{ width: `${Math.min(r.participationRate, 100)}%` }} /></div>
        %{r.participationRate}
      </div>,
    },
    { key: "last", header: "Son Yanıt", render: r => (r.lastResponseAt ? formatDateTime(r.lastResponseAt) : "—") },
    { key: "actions", header: "İşlemler", className: "right", render: r => <div className="actions"><IconButton icon="chart" label="Raporu aç" to={`/reports/${r.surveyId}`} /></div> },
  ];

  const filtered = q || status.length;
  return <Shell>
    <PageHead title="Raporlar" />
    <div className="toolbar">
      <SearchInput value={q} placeholder="Anket ara..." onSearch={v => update({ q: v })} />
      <FilterMenu label="Durum" options={statusOptions("survey").filter(o => REPORTABLE.includes(o.value))} value={status} onChange={v => update({ status: v })} />
    </div>
    <DataTable
      columns={columns}
      rows={data?.items}
      rowKey={r => r.surveyId}
      loading={loading && !data}
      error={error}
      onRetry={reload}
      empty={filtered
        ? { title: "Sonuç bulunamadı", text: "Arama veya filtreleri değiştirip tekrar deneyin." }
        : { title: "Henüz rapor yok", text: "Bir anketi yayınladığınızda raporu burada görünür." }}
    />
    {!error && <Pager meta={data?.page} onChange={p => update(p)} />}
  </Shell>;
}
