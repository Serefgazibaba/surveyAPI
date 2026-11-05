import { useEffect, useState } from "react";
import { listMails } from "../api/mails.js";
import { listSurveys } from "../api/surveys.js";
import { tokenStore } from "../auth/tokenStore.js";

// Bildirimler mevcut verilerden türetilir (ayrı bir bildirim servisi yok):
//  - gönderilemeyen davet mailleri (FAILED)
//  - en geç ENDING_SOON_DAYS gün içinde kapanacak yayındaki anketler
// Sol menü her sayfada yeniden çizildiği için sonuç kısa süre önbellekte tutulur.

export const ENDING_SOON_DAYS = 3;
const TTL_MS = 60_000;

let cache = null; // { at, owner, items } — owner: önbelleğin ait olduğu Admin (başka hesapla girişte kullanılmaz)
let inflight = null;

async function load() {
  const [mails, surveys] = await Promise.all([
    listMails({ emailStatus: "FAILED", size: 1 }),
    listSurveys({ status: ["PUBLISHED"], sort: "endsAt", size: 50 }),
  ]);
  const items = [];
  const failed = mails?.summary?.FAILED ?? 0;
  if (failed > 0) {
    items.push({ key: "failed-mails", tone: "error", icon: "mail", title: `${failed} davet maili gönderilemedi`, text: "Gönderim Durumu ekranından hatayı görüp yeniden gönderebilirsiniz.", to: "/mails?emailStatus=FAILED" });
  }
  const limit = Date.now() + ENDING_SOON_DAYS * 864e5;
  (surveys?.items || [])
    .filter(s => s.endsAt && new Date(s.endsAt).getTime() <= limit)
    .forEach(s => items.push({ key: `ending-${s.id}`, tone: "warning", icon: "clock", title: `"${s.title}" yakında kapanıyor`, endsAt: s.endsAt, to: `/reports/${s.id}` }));
  return items;
}

const currentOwner = () => tokenStore.getAdmin()?.user?.id ?? tokenStore.getAdmin()?.user?.email ?? null;

/** Bildirimleri yükler. refresh: önbelleği atlayıp yeniden yükler. */
export function fetchNotifications({ refresh = false } = {}) {
  const owner = currentOwner();
  if (!refresh && cache && cache.owner === owner && Date.now() - cache.at < TTL_MS) return Promise.resolve(cache.items);
  if (!inflight) {
    inflight = load()
      .then(items => { cache = { at: Date.now(), owner, items }; return items; })
      .finally(() => { inflight = null; });
  }
  return inflight;
}

/** Sol menü rozeti için bildirim sayısı; hata olursa rozet gösterilmez. */
export function useNotificationCount() {
  const [count, setCount] = useState(cache && cache.owner === currentOwner() ? cache.items.length : 0);
  useEffect(() => {
    let alive = true;
    fetchNotifications().then(items => alive && setCount(items.length), () => {});
    return () => { alive = false; };
  }, []);
  return count;
}
