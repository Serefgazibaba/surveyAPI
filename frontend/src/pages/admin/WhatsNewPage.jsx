import { Link } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import Badge from "../../components/Badge.jsx";

// Uygulamaya eklenen özellikler (en yeni önce). Yeni sürümde başa satır eklenir.
const RELEASES = [
  {
    date: "05.11.2026", title: "Raporlar", to: "/reports",
    items: ["Anket bazında katılım oranı, memnuniyet ortalaması ve soru dağılımları", "Serbest metin ve “Diğer” cevaplarında arama", "Kayıtlı anketlerde katılımcı bazlı yanıtlar", "CSV indirme ve PDF olarak yazdırma"],
  },
  {
    date: "04.11.2026", title: "Şablonlar", to: "/templates",
    items: ["Anketi şablon olarak kaydetme", "Sıfırdan şablon oluşturma ve düzenleme", "Şablondan tek tıkla yeni taslak anket"],
  },
  {
    date: "04.11.2026", title: "Katılımcı formu",
    items: ["Davet linkiyle giriş", "Ara kayıt ve kaldığı yerden devam", "Gönderim ve tekrar yanıt engeli; anonim modda kimlik bağı yok"],
  },
  {
    date: "04.11.2026", title: "Davet mailleri", to: "/mails",
    items: ["Yayında seçilen katılımcılara kişiye özel davet maili", "Gönderim durumu takibi ve hatalı mailleri yeniden gönderme"],
  },
  {
    date: "03.11.2026", title: "Anket oluşturma ve yayınlama", to: "/surveys",
    items: ["Likert, tekli seçim, çoklu seçim ve serbest metin soruları", "Taslak kaydetme, sürükle-bırak ile sıralama", "Katılımcı seçerek yayınlama; anonim veya kayıtlı yanıt modu"],
  },
];

/** Yenilikler: eklenen özelliklerin kısa listesi. */
export default function WhatsNewPage() {
  return <Shell>
    <PageHead title="Yenilikler" />
    <div className="release-list">
      {RELEASES.map(r => <section key={r.title} className="panel release-card">
        <div className="inline-between">
          <h2>{r.to ? <Link to={r.to}>{r.title}</Link> : r.title}</h2>
          <Badge tone="gray">{r.date}</Badge>
        </div>
        <ul>{r.items.map(i => <li key={i}>{i}</li>)}</ul>
      </section>)}
    </div>
  </Shell>;
}
