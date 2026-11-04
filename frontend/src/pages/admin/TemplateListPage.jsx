import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import Tabs from "../../components/Tabs.jsx";
import Button from "../../components/Button.jsx";
import IconButton from "../../components/IconButton.jsx";
import Icon from "../../components/Icon.jsx";
import SearchInput from "../../components/SearchInput.jsx";
import Pager from "../../components/Pager.jsx";
import Spinner from "../../components/Spinner.jsx";
import EmptyState from "../../components/EmptyState.jsx";
import ErrorState from "../../components/ErrorState.jsx";
import ConfirmDialog from "../../components/ConfirmDialog.jsx";
import { useToast } from "../../components/Toast.jsx";
import useAsync from "../../hooks/useAsync.js";
import { listTemplates, deleteTemplate, createSurveyFromTemplate } from "../../api/templates.js";
import { formatDate } from "../../utils/datetime.js";

function TemplateCard({ t, using, disabled, onUse, onDelete }) {
  return <section className="panel template-card">
    <div className="inline-between">
      <div className="template-icon"><Icon name="template" /></div>
      <div className="actions">
        <IconButton icon="edit" label="Düzenle" to={`/templates/${t.id}/edit`} />
        <IconButton icon="trash" label="Sil" danger onClick={onDelete} />
      </div>
    </div>
    <h2>{t.name}</h2>
    <span className="text-muted">{t.questionCount} soru · {formatDate(t.updatedAt)}</span>
    {t.description && <p className="template-description">{t.description}</p>}
    <Button className="template-use" icon="plus" loading={using} disabled={disabled} onClick={onUse}>Bu şablonu kullan</Button>
  </section>;
}

/** Şablonlar: kendi şablonları; şablondan anket oluşturma, düzenleme, silme (FR-30, FR-31). */
export default function TemplateListPage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const toast = useToast();
  const q = params.get("q") || "";
  const page = Number(params.get("page")) || 1;
  const size = Number(params.get("size")) || 24;

  const { data, error, loading, reload } = useAsync(() => listTemplates({ q, page, size }), [q, page, size]);

  const update = patch => {
    const next = new URLSearchParams(params);
    Object.entries(patch).forEach(([k, v]) => { next.delete(k); if (v !== undefined && v !== null && v !== "") next.set(k, v); });
    if (!("page" in patch)) next.delete("page");
    setParams(next, { replace: true });
  };

  const [usingId, setUsingId] = useState(null);
  const use = async t => {
    setUsingId(t.id);
    try {
      const survey = await createSurveyFromTemplate(t.id);
      toast.success(`"${t.name}" şablonundan yeni taslak anket oluşturuldu.`);
      navigate(`/surveys/${survey.id}/edit`, { state: { detail: survey } });
    } catch (err) {
      toast.error(err);
      setUsingId(null);
    }
  };

  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);
  const confirmDelete = async () => {
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteTemplate(toDelete.id);
      toast.success(`"${toDelete.name}" şablonu silindi.`);
      setToDelete(null);
      reload();
    } catch (err) {
      setDeleteError(err);
    } finally {
      setDeleting(false);
    }
  };

  const items = data?.items;
  let body;
  if (error) body = <ErrorState error={error} onRetry={reload} />;
  else if (!items) body = <Spinner size="lg" label="Şablonlar yükleniyor..." />;
  else if (items.length === 0) body = <section className="panel">{q
    ? <EmptyState title="Sonuç bulunamadı" text="Farklı bir şablon adıyla tekrar arayın." />
    : <EmptyState title="Henüz şablonunuz yok" text="Bir anketi düzenlerken “Şablon olarak kaydet” ile veya sıfırdan şablon oluşturabilirsiniz."
      action={<Button variant="primary" icon="plus" to="/templates/new">Yeni Şablon</Button>} />}</section>;
  else body = <div className="template-grid" aria-busy={loading || undefined}>
    {items.map(t => <TemplateCard key={t.id} t={t} using={usingId === t.id} disabled={!!usingId && usingId !== t.id}
      onUse={() => use(t)} onDelete={() => { setDeleteError(null); setToDelete(t); }} />)}
  </div>;

  return <Shell>
    <Tabs variant="seg" value="templates" onChange={v => v === "mine" && navigate("/surveys")}
      items={[{ value: "mine", label: "Anketlerim" }, { value: "templates", label: "Şablonlar" }]} />
    <PageHead title="Şablonlar">
      <Button variant="primary" icon="plus" to="/templates/new">Yeni Şablon</Button>
    </PageHead>
    <div className="toolbar">
      <SearchInput value={q} placeholder="Şablon ara..." onSearch={v => update({ q: v })} />
    </div>
    {body}
    {!error && items?.length > 0 && <Pager meta={data?.page} onChange={p => update(p)} sizes={[12, 24, 48]} />}
    {toDelete && <ConfirmDialog
      title="Şablonu sil"
      message={`"${toDelete.name}" şablonu kalıcı olarak silinecek. Bu şablondan daha önce oluşturulmuş anketler etkilenmez.`}
      confirmLabel="Sil" danger busy={deleting} error={deleteError}
      onConfirm={confirmDelete} onCancel={() => setToDelete(null)}
    />}
  </Shell>;
}
