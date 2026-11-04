import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import Tabs from "../../components/Tabs.jsx";
import Button from "../../components/Button.jsx";
import IconButton from "../../components/IconButton.jsx";
import Icon from "../../components/Icon.jsx";
import ComingSoon from "../../components/ComingSoon.jsx";
import SearchInput from "../../components/SearchInput.jsx";
import FilterMenu from "../../components/FilterMenu.jsx";
import DataTable from "../../components/DataTable.jsx";
import Pager from "../../components/Pager.jsx";
import StatusBadge from "../../components/StatusBadge.jsx";
import ConfirmDialog from "../../components/ConfirmDialog.jsx";
import { useToast } from "../../components/Toast.jsx";
import useAsync from "../../hooks/useAsync.js";
import { listSurveys, deleteSurvey } from "../../api/surveys.js";
import { statusOptions } from "../../utils/statusLabels.js";
import { formatDate } from "../../utils/datetime.js";

const DEFAULT_SORT = "-updatedAt";

/** S-02 · Anketlerim */
export default function SurveyListPage() {
  const [params, setParams] = useSearchParams();
  const toast = useToast();
  const q = params.get("q") || "";
  const status = params.getAll("status");
  const sort = params.get("sort") || DEFAULT_SORT;
  const page = Number(params.get("page")) || 1;
  const size = Number(params.get("size")) || 20;

  const { data, error, loading, reload } = useAsync(
    () => listSurveys({ q, status, sort, page, size }),
    [q, status.join(","), sort, page, size],
  );

  const update = patch => {
    const next = new URLSearchParams(params);
    Object.entries(patch).forEach(([k, v]) => {
      next.delete(k);
      (Array.isArray(v) ? v : [v]).forEach(x => { if (x !== undefined && x !== null && x !== "") next.append(k, x); });
    });
    if (!("page" in patch)) next.delete("page");
    setParams(next, { replace: true });
  };

  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);
  const confirmDelete = async () => {
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteSurvey(toDelete.id);
      toast.success(`"${toDelete.title}" silindi.`);
      setToDelete(null);
      reload();
    } catch (err) {
      setDeleteError(err);
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    { key: "title", header: "Anket", sortKey: "title", className: "title-cell", render: s => <>{s.title}<small>{s.questionCount} soru</small></> },
    { key: "status", header: "Durum", render: s => <StatusBadge value={s.status} /> },
    { key: "endsAt", header: "Bitiş Tarihi", sortKey: "endsAt", render: s => <span className="date"><Icon name="calendar" />{formatDate(s.endsAt)}</span> },
    { key: "participantCount", header: "Katılımcı", render: s => (s.participantCount ? `${s.participantCount} kişi` : "—") },
    { key: "updatedAt", header: "Güncellenme", sortKey: "updatedAt", render: s => formatDate(s.updatedAt) },
    {
      key: "actions", header: "İşlemler", className: "right", render: s => {
        const can = a => s.allowedActions?.includes(a);
        return <div className="actions">
          <IconButton icon="chart" label="Sonuçlar (yakında)" disabled />
          {can("PUBLISH") && <IconButton icon="send" label="Gönder" to={`/surveys/${s.id}/participants`} />}
          {can("EDIT")
            ? <IconButton icon="edit" label="Düzenle" to={`/surveys/${s.id}/edit`} />
            : <IconButton icon="eye" label="Görüntüle" to={`/surveys/${s.id}/edit`} />}
          {can("DELETE") && <IconButton icon="trash" label="Sil" danger onClick={() => { setDeleteError(null); setToDelete(s); }} />}
        </div>;
      },
    },
  ];

  const filtered = q || status.length;
  return <Shell>
    <Tabs variant="seg" value="mine" items={[{ value: "mine", label: "Anketlerim" }, { value: "templates", label: "Şablonlar", disabled: true }]} />
    <PageHead title="Anketlerim">
      <ComingSoon><Button icon="template" disabled>Şablondan Oluştur</Button></ComingSoon>
    </PageHead>
    <div className="toolbar">
      <SearchInput value={q} placeholder="Anket ara..." onSearch={v => update({ q: v })} />
      <FilterMenu label="Durum" options={statusOptions("survey")} value={status} onChange={v => update({ status: v })} />
      <span className="grow" />
      <Button variant="primary" icon="plus" to="/surveys/new">Yeni Anket</Button>
    </div>
    <DataTable
      columns={columns}
      rows={data?.items}
      loading={loading && !data}
      error={error}
      onRetry={reload}
      sort={{ value: sort, onChange: v => update({ sort: v }) }}
      empty={filtered
        ? { title: "Sonuç bulunamadı", text: "Arama veya filtreleri değiştirip tekrar deneyin." }
        : { title: "Henüz anketiniz yok", text: "İlk anketinizi oluşturarak başlayın.", action: <Button variant="primary" icon="plus" to="/surveys/new">Yeni Anket</Button> }}
    />
    {!error && <Pager meta={data?.page} onChange={p => update(p)} />}
    {toDelete && <ConfirmDialog
      title="Anketi sil"
      message={`"${toDelete.title}" kalıcı olarak silinecek. Bu işlem geri alınamaz.`}
      confirmLabel="Sil" danger busy={deleting} error={deleteError}
      onConfirm={confirmDelete} onCancel={() => setToDelete(null)}
    />}
  </Shell>;
}
