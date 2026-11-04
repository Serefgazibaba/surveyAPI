import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import Button from "../../components/Button.jsx";
import ComingSoon from "../../components/ComingSoon.jsx";
import SearchInput from "../../components/SearchInput.jsx";
import DataTable from "../../components/DataTable.jsx";
import Pager from "../../components/Pager.jsx";
import Alert from "../../components/Alert.jsx";
import ErrorAlert from "../../components/ErrorAlert.jsx";
import ErrorState from "../../components/ErrorState.jsx";
import ConfirmDialog from "../../components/ConfirmDialog.jsx";
import Spinner from "../../components/Spinner.jsx";
import Icon from "../../components/Icon.jsx";
import useAsync from "../../hooks/useAsync.js";
import { getSurvey, publishSurvey } from "../../api/surveys.js";
import { listInvitableUsers } from "../../api/users.js";
import { indexFromPath } from "../../utils/errorMessages.js";

const MAX_PARTICIPANTS = 1000;
const columns = [
  { key: "fullName", header: "Ad Soyad", className: "title-cell" },
  { key: "email", header: "E-posta" },
];

/** Seçili kullanıcı çipi (sayfalar arası seçim burada görünür). */
function SelectedChips({ users, invalidIds, onRemove, onClear }) {
  if (!users.length) return <p className="text-muted" style={{ margin: 0 }}>Henüz katılımcı seçilmedi. Aşağıdaki listeden seçim yapın.</p>;
  return <>
    <div className="inline-between"><b>{users.length} katılımcı seçildi</b><button type="button" className="text-danger" onClick={onClear}>Tümünü temizle</button></div>
    <div className="chips">
      {users.map(u => <span key={u.id} className={`chip-item${invalidIds.has(u.id) ? " invalid" : ""}`} title={u.email}>
        {u.fullName}
        <button type="button" aria-label={`${u.fullName} seçimini kaldır`} onClick={() => onRemove(u.id)}><Icon name="x" /></button>
      </span>)}
    </div>
  </>;
}

/** S-05 · Katılımcı seçimi ve yayınlama */
export default function ParticipantsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const survey = useAsync(() => getSurvey(id), [id]);
  const [query, setQuery] = useState({ q: "", page: 1, size: 20 });
  const users = useAsync(() => listInvitableUsers(query), [query.q, query.page, query.size]);

  const [selected, setSelected] = useState(() => new Map()); // id → { id, fullName, email }
  const [confirming, setConfirming] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [error, setError] = useState(null);

  // INVALID_PARTICIPANTS: errors[].field = "userIds[i]" → gönderilen dizideki kullanıcı
  const [sentIds, setSentIds] = useState([]);
  const invalidIds = useMemo(() => new Set((error?.code === "INVALID_PARTICIPANTS" ? error.errors : [])
    .map(e => sentIds[indexFromPath(e.field, "userIds")]).filter(Boolean)), [error, sentIds]);

  const selectedSet = useMemo(() => new Set(selected.keys()), [selected]);
  const onSelectionChange = nextSet => {
    const next = new Map(selected);
    (users.data?.items || []).forEach(u => (nextSet.has(u.id) ? next.set(u.id, u) : next.delete(u.id)));
    setSelected(next);
  };
  const remove = userId => setSelected(prev => { const next = new Map(prev); next.delete(userId); return next; });

  const publish = async () => {
    const ids = [...selected.keys()];
    setPublishing(true);
    setError(null);
    setSentIds(ids);
    try {
      const result = await publishSurvey(id, ids);
      navigate(`/surveys/${id}/published`, { replace: true, state: { result, title: survey.data?.title } });
    } catch (err) {
      setError(err);
      setConfirming(false);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } finally {
      setPublishing(false);
    }
  };

  // Eksik anket hatası builder'da ilgili alanlar işaretlenerek gösterilir.
  const fixInBuilder = () => navigate(`/surveys/${id}/edit`, { state: { publishError: { ...error, errors: error.errors } } });

  const crumbs = [{ label: "Anketlerim", to: "/surveys" }, { label: survey.data?.title || "Anket", to: `/surveys/${id}/edit` }, { label: "Katılımcılar" }];
  if (survey.error) return <Shell><PageHead title="Katılımcılar" crumbs={crumbs} /><ErrorState error={survey.error} onRetry={survey.reload} backTo={{ to: "/surveys", label: "Anketlerime dön" }} /></Shell>;
  if (!survey.data) return <Shell><Spinner size="lg" label="Anket yükleniyor..." /></Shell>;

  const canPublish = survey.data.allowedActions?.includes("PUBLISH");
  const count = selected.size;

  return <Shell>
    <PageHead title="Katılımcılar" crumbs={crumbs}>
      <Button to={`/surveys/${id}/edit`}>Geri</Button>
      {canPublish && <Button variant="primary" icon="send" disabled={!count || count > MAX_PARTICIPANTS} onClick={() => setConfirming(true)}>Tamam ve Gönder</Button>}
    </PageHead>

    {!canPublish && <Alert tone="info" title="Bu anket zaten yayınlanmış"
      actions={<Button size="sm" icon="mail" to={`/mails?surveyId=${id}`}>Gönderim durumunu görüntüle</Button>}>
      Yayındaki ankete yeni katılımcı eklenemez.
    </Alert>}
    <ErrorAlert error={error} onItemClick={error?.code === "SURVEY_INCOMPLETE" ? fixInBuilder : undefined} />
    {error?.code === "SURVEY_INCOMPLETE" && <div className="mt" style={{ marginTop: -6, marginBottom: 16 }}><Button icon="edit" onClick={fixInBuilder}>Anketi düzenle</Button></div>}

    {canPublish && <div className="stack">
      <section className="panel participant-panel">
        <p>Her katılımcıya kişiye özel, tek kullanımlık link e-posta ile gönderilir.</p>
        <SelectedChips users={[...selected.values()]} invalidIds={invalidIds} onRemove={remove} onClear={() => setSelected(new Map())} />
        {count > MAX_PARTICIPANTS && <p className="field-error">En fazla {MAX_PARTICIPANTS} katılımcı seçilebilir.</p>}
        <ComingSoon block><div className="upload-box" style={{ width: "100%" }}>⇧ <div><b>CSV dosyası yükleyin</b> veya e-posta ile ekleyin<small>Kayıtlı olmayan kişileri davet etme yakında</small></div></div></ComingSoon>
      </section>

      <div className="toolbar" style={{ marginBottom: 0 }}>
        <SearchInput value={query.q} placeholder="Ad veya e-posta ile ara..." onSearch={q => setQuery(s => ({ ...s, q, page: 1 }))} />
      </div>
      <div>
        <DataTable columns={columns} rows={users.data?.items} loading={users.loading && !users.data} error={users.error} onRetry={users.reload}
          selection={{ selected: selectedSet, onChange: onSelectionChange }}
          rowClassName={u => (invalidIds.has(u.id) ? "row-error" : undefined)}
          empty={{ title: "Kullanıcı bulunamadı", text: query.q ? "Aramayı değiştirip tekrar deneyin." : "Davet edilebilecek aktif kullanıcı yok." }} />
        {!users.error && <Pager meta={users.data?.page} onChange={p => setQuery(s => ({ ...s, ...p }))} />}
      </div>
    </div>}

    {confirming && <ConfirmDialog title="Anketi yayınla"
      message={`"${survey.data.title}" ${count} kişiye gönderilecek. Yayından sonra soru ekleme/silme, tip, sıralama, bitiş tarihi ve yanıt modu değiştirilemez.`}
      confirmLabel="Yayınla ve Gönder" busy={publishing} onConfirm={publish} onCancel={() => setConfirming(false)} />}
  </Shell>;
}
