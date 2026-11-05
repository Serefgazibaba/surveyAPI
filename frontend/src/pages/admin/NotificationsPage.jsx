import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import Button from "../../components/Button.jsx";
import Icon from "../../components/Icon.jsx";
import Spinner from "../../components/Spinner.jsx";
import ErrorState from "../../components/ErrorState.jsx";
import { fetchNotifications, ENDING_SOON_DAYS } from "../../hooks/useNotifications.js";
import { formatDateTime } from "../../utils/datetime.js";

/** Bildirimler: gönderilemeyen davet mailleri ve yakında kapanacak anketler (mevcut verilerden). */
export default function NotificationsPage() {
  const [state, setState] = useState({ items: null, error: null });
  const load = useCallback((refresh = false) => {
    setState(s => ({ ...s, error: null }));
    fetchNotifications({ refresh }).then(items => setState({ items, error: null }), error => setState({ items: null, error }));
  }, []);
  useEffect(() => { load(true); }, [load]);

  const { items, error } = state;
  return <Shell>
    <PageHead title="Bildirimler">
      <Button icon="refresh" onClick={() => load(true)}>Yenile</Button>
    </PageHead>
    {error && <ErrorState error={error} onRetry={() => load(true)} />}
    {!error && !items && <Spinner size="lg" label="Bildirimler yükleniyor..." />}
    {items?.length === 0 && <section className="panel placeholder-card">
      <div className="placeholder-icon"><Icon name="bell" /></div>
      <h2>Yeni bildirim yok</h2>
      <p>Gönderilemeyen davet mailleri ve {ENDING_SOON_DAYS} gün içinde kapanacak anketler burada gösterilir.</p>
    </section>}
    {items?.length > 0 && <section className="panel notification-list">
      {items.map(n => <Link key={n.key} to={n.to} className={`notification-item ${n.tone}`}>
        <span className="notification-icon"><Icon name={n.icon} /></span>
        <span className="notification-copy">
          <b>{n.title}</b>
          <small>{n.endsAt ? `Bitiş: ${formatDateTime(n.endsAt)}` : n.text}</small>
        </span>
        <Icon name="right" />
      </Link>)}
    </section>}
  </Shell>;
}
