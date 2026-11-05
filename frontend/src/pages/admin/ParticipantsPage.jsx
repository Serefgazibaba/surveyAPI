import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import Button from "../../components/Button.jsx";
import ComingSoon from "../../components/ComingSoon.jsx";
import TextInput from "../../components/TextInput.jsx";
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

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const normalizeEmail = e => e.trim().toLocaleLowerCase("en");

/**
 * Seçilen katılımcılar (sayfalar arası seçim burada görünür). people: [{ key, fullName, email, external }]
 * invalidKeys: sunucunun işaretlediği kişiler.
 */
function SelectedChips({ people, invalidKeys, onRemove, onClear }) {
  if (!people.length) return <p className="text-muted" style={{ margin: 0 }}>Henüz katılımcı seçilmedi. Listeden seçin veya e-posta ile ekleyin.</p>;
  return <>
    <div className="inline-between"><b>{people.length} katılımcı seçildi</b><button type="button" className="text-danger" onClick={onClear}>Tümünü temizle</button></div>
    <div className="chips">
      {people.map(p => <span key={p.key} className={`chip-item${p.external ? " external" : ""}${invalidKeys.has(p.key) ? " invalid" : ""}`} title={p.email}>
        {p.fullName}{p.external && <span className="chip-tag">Dış · {p.email}</span>}
        <button type="button" aria-label={`${p.fullName} seçimini kaldır`} onClick={() => onRemove(p)}><Icon name="x" /></button>
      </span>)}
    </div>
  </>;
}

/** Uygulamada kayıtlı olmayan kişiyi ad + e-posta ile ekler (FR-40). */
function AddByEmail({ takenEmails, onAdd }) {
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [error, setError] = useState(null);
  const submit = e => {
    e.preventDefault();
    const name = fullName.trim();
    const mail = normalizeEmail(email);
    if (!name) return setError("Ad Soyad girilmelidir.");
    if (!EMAIL_RE.test(mail)) return setError("Geçerli bir e-posta adresi girin.");
    if (takenEmails.has(mail)) return setError("Bu e-posta adresi zaten listede.");
    onAdd({ fullName: name, email: mail });
    setFullName("");
    setEmail("");
    setError(null);
  };
  return <form className="add-by-email" onSubmit={submit} noValidate>
    <div className="add-by-email-row">
      <TextInput aria-label="Ad Soyad" placeholder="Ad Soyad" maxLength={200} value={fullName} onChange={e => { setFullName(e.target.value); setError(null); }} />
      <TextInput aria-label="E-posta" type="email" placeholder="ornek@firma.com" maxLength={254} value={email} onChange={e => { setEmail(e.target.value); setError(null); }} />
      <Button type="submit" icon="plus">Ekle</Button>
    </div>
    {error
      ? <p className="field-error" role="alert">{error}</p>
      : <small className="text-muted">Uygulamada kaydı olmayan kişilere de davet gönderebilirsiniz. Adres kayıtlı bir kullanıcıya aitse otomatik eşleştirilir.</small>}
  </form>;
}

/** S-05 · Katılımcı seçimi ve yayınlama */
export default function ParticipantsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const survey = useAsync(() => getSurvey(id), [id]);
  const [query, setQuery] = useState({ q: "", page: 1, size: 20 });
  const users = useAsync(() => listInvitableUsers(query), [query.q, query.page, query.size]);

  const [selected, setSelected] = useState(() => new Map()); // kayıtlı: id → { id, fullName, email }
  const [external, setExternal] = useState(() => new Map()); // dış: email → { fullName, email }
  const [confirming, setConfirming] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [error, setError] = useState(null);

  // Hata alanları gönderilen dizilerdeki konuma göre gelir: userIds[i] (422) veya participants[i].email/fullName (400).
  const [sent, setSent] = useState({ userIds: [], emails: [] });
  const invalidKeys = useMemo(() => {
    const keys = new Set();
    (error?.errors || []).forEach(e => {
      if (e.field?.startsWith("userIds[")) {
        const k = sent.userIds[indexFromPath(e.field, "userIds")];
        if (k) keys.add(k);
      }
      if (e.field?.startsWith("participants[")) {
        const k = sent.emails[indexFromPath(e.field, "participants")];
        if (k) keys.add(`ext:${k}`);
      }
    });
    return keys;
  }, [error, sent]);

  const selectedSet = useMemo(() => new Set(selected.keys()), [selected]);
  const onSelectionChange = nextSet => {
    const next = new Map(selected);
    (users.data?.items || []).forEach(u => (nextSet.has(u.id) ? next.set(u.id, u) : next.delete(u.id)));
    setSelected(next);
  };
  const remove = person => (person.external
    ? setExternal(prev => { const next = new Map(prev); next.delete(person.email); return next; })
    : setSelected(prev => { const next = new Map(prev); next.delete(person.key); return next; }));
  const people = [
    ...[...selected.values()].map(u => ({ key: u.id, fullName: u.fullName, email: u.email, external: false })),
    ...[...external.values()].map(p => ({ key: `ext:${p.email}`, fullName: p.fullName, email: p.email, external: true })),
  ];
  const takenEmails = new Set(people.map(p => normalizeEmail(p.email)));

  const publish = async () => {
    const userIds = [...selected.keys()];
    const participants = [...external.values()];
    setPublishing(true);
    setError(null);
    setSent({ userIds, emails: participants.map(p => p.email) });
    try {
      const result = await publishSurvey(id, { userIds, participants });
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
  const count = selected.size + external.size;

  return <Shell>
    <PageHead title="Katılımcılar" crumbs={crumbs}>
      <Button to={`/surveys/${id}/edit`}>Geri</Button>
      {canPublish && <Button variant="primary" icon="send" disabled={!count || count > MAX_PARTICIPANTS} onClick={() => setConfirming(true)}>Gönder</Button>}
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
        <SelectedChips people={people} invalidKeys={invalidKeys} onRemove={remove} onClear={() => { setSelected(new Map()); setExternal(new Map()); }} />
        {count > MAX_PARTICIPANTS && <p className="field-error">En fazla {MAX_PARTICIPANTS} katılımcı seçilebilir.</p>}
        <div className="participant-section">
          <b>E-posta ile ekle</b>
          <AddByEmail takenEmails={takenEmails} onAdd={p => setExternal(prev => new Map(prev).set(p.email, p))} />
        </div>
        <ComingSoon block><div className="upload-box" style={{ width: "100%" }}>⇧ <div><b>CSV dosyası yükleyin</b><small>Toplu katılımcı içe aktarma yakında</small></div></div></ComingSoon>
      </section>

      <h2 className="section-title">Kayıtlı kullanıcılar</h2>
      <div className="toolbar" style={{ marginBottom: 0 }}>
        <SearchInput value={query.q} placeholder="Ad veya e-posta ile ara..." onSearch={q => setQuery(s => ({ ...s, q, page: 1 }))} />
      </div>
      <div>
        <DataTable columns={columns} rows={users.data?.items} loading={users.loading && !users.data} error={users.error} onRetry={users.reload}
          selection={{ selected: selectedSet, onChange: onSelectionChange }}
          rowClassName={u => (invalidKeys.has(u.id) ? "row-error" : undefined)}
          empty={{ title: "Kullanıcı bulunamadı", text: query.q ? "Aramayı değiştirip tekrar deneyin." : "Davet edilebilecek aktif kullanıcı yok. E-posta ile ekleyebilirsiniz." }} />
        {!users.error && <Pager meta={users.data?.page} onChange={p => setQuery(s => ({ ...s, ...p }))} />}
      </div>
    </div>}

    {confirming && <ConfirmDialog title="Anketi yayınla"
      message={`"${survey.data.title}" ${count} kişiye gönderilecek. Yayından sonra soru ekleme/silme, tip, sıralama, bitiş tarihi ve yanıt modu değiştirilemez.`}
      confirmLabel="Yayınla ve Gönder" busy={publishing} onConfirm={publish} onCancel={() => setConfirming(false)} />}
  </Shell>;
}
