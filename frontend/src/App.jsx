import React, { useState } from "react";

/**
 * Survey Tool — React UI prototype
 * Runtime target: Node.js 24.14.0 / npm 11.9.0
 * This file contains presentation only; connect API/authentication separately.
 */

const initialSurveys = [
  { name: "Eğitim Değerlendirme Anketi", sub: "Performance Optimized Back-end Development", status: "Yayında", end: "12.12.2026", invited: 40, responses: 28 },
  { name: "Çalışan Memnuniyeti 2026 Q4", sub: "People & Culture", status: "Yayında", end: "30.11.2026", invited: 320, responses: 187 },
  { name: "Müşteri İhtiyaç Analizi", sub: "Satış & Pazarlama", status: "Taslak", end: "15.01.2027", invited: 0, responses: 0 },
  { name: "Eğiticinin Eğitimi Geri Bildirim", sub: "Akademi", status: "Süresi Doldu", end: "24.03.2026", invited: 18, responses: 16 },
  { name: "Yan Haklar Tercih Anketi", sub: "People & Culture", status: "Kapandı", end: "10.10.2025", invited: 290, responses: 244 },
  { name: "GenAI in Development Ön Anket", sub: "Akademi", status: "Taslak", end: "10.11.2026", invited: 0, responses: 0 },
];

const screens = [
  ["login", "S-01 · Giriş"], ["list", "S-02 · Anketlerim"],
  ["builder", "S-03 · Anket Oluşturma"], ["question", "S-04 · Yeni Soru"],
  ["participants", "S-05 · Katılımcılar"], ["sent", "S-06 · Gönderim Onayı"],
  ["report", "S-07 · Anket Raporu"], ["templates", "Şablonlar"],
  ["form", "S-08 · Katılımcı Formu"], ["thanks", "S-09 · Teşekkür"],
  ["expired", "S-10 · Erişim Hatası"],
];

const initialSections = [
  { name: "Eğitim İçeriği", questions: [
    ["Eğitim içeriğinin yeterli olduğunu düşünüyorum.", "Likert", true],
    ["Eğitimde kullanılan materyaller anlaşılırdı.", "Likert", true],
    ["Eğitimin en faydalı bulduğunuz bölümü hangisiydi?", "Tekli Seçim", false],
  ]},
  { name: "Eğitmen ve Organizasyon", questions: [
    ["Eğitmen konuya hakimdi.", "Likert", true],
    ["Hangi konularda ek eğitim almak istersiniz?", "Çoklu Seçim", false],
    ["Eğitimle ilgili eklemek istedikleriniz.", "Serbest Metin", false],
  ]},
];

function Icon({ name = "dot" }) {
  const paths = {
    home: <><path d="m3 10 9-7 9 7v11h-6v-6H9v6H3z" /></>,
    list: <><rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 9h6M9 13h6M9 17h4"/></>,
    plus: <><path d="M12 5v14M5 12h14"/></>,
    chart: <><path d="M3 3v18h18M7 15l4-4 3 3 5-6"/></>,
    users: <><circle cx="9" cy="8" r="4"/><path d="M2 21c0-4 3-6 7-6s7 2 7 6M16 4a4 4 0 0 1 0 8M18 16c2.5.6 4 2.6 4 5"/></>,
    template: <><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></>,
    search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
    calendar: <><rect x="3" y="4" width="18" height="17" rx="2"/><path d="M8 2v4M16 2v4M3 10h18"/></>,
    send: <><path d="m22 2-7 20-4-9-9-4zM22 2 11 13"/></>,
    edit: <><path d="M12 20h9M16.5 3.5a2 2 0 0 1 3 3L7 19l-4 1 1-4z"/></>,
    eye: <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></>,
    lock: <><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></>,
    check: <><path d="m20 6-11 11-5-5"/></>,
    left: <><path d="m15 18-6-6 6-6"/></>,
    right: <><path d="m9 18 6-6-6-6"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    trash: <><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/></>,
    copy: <><rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1"/></>,
  };
  return <svg className="i" viewBox="0 0 24 24" aria-hidden="true">{paths[name] || <circle cx="12" cy="12" r="8"/>}</svg>;
}

function Button({ children, primary = false, ghost = false, ...props }) {
  return <button className={`btn${primary ? " primary" : ""}${ghost ? " ghost" : ""}`} {...props}>{children}</button>;
}
function Badge({ children, tone = "blue" }) {
  return <span className={`badge ${tone}`}>{children}</span>;
}
function Field({ label, children }) {
  return <label className="field"><span>{label}</span>{children}</label>;
}
function Shell({ active, navigate, children }) {
  const nav = [
    ["dashboard", "home", "Dashboard"], ["list", "list", "Anketlerim"],
    ["builder", "plus", "Anket Oluştur"], ["templates", "template", "Şablonlar"],
    ["report", "chart", "Raporlar"], ["people", "users", "Katılımcı Listeleri"],
  ];
  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><div className="logo">S</div><div><b>Survey Tool</b><small>DefineX</small></div></div>
      <div className="nav-label">Benim Alanım</div>
      {nav.slice(0, 4).map(([key, icon, label]) => <button key={key} className={`nav-item ${active === key ? "active" : ""}`} onClick={() => navigate(key)}><Icon name={icon}/>{label}</button>)}
      <div className="nav-label">Analiz</div>
      {nav.slice(4).map(([key, icon, label]) => <button key={key} className={`nav-item ${active === key ? "active" : ""}`} onClick={() => navigate(key)}><Icon name={icon}/>{label}</button>)}
      <div className="nav-label">Sistem</div><button className="nav-item"><span>＋</span>Destek</button>
      <div className="sidebar-bottom"><button className="nav-item">✦ Yenilikler</button><button className="nav-item">♧ Bildirimler <span className="count">3</span></button><div className="profile"><div className="avatar">EA</div><div><b>Ediz Agarer</b><small>Admin</small></div></div></div>
    </aside>
    <main className="main-content">{children}</main>
  </div>;
}
function PageHead({ title, children }) {
  return <header className="page-head"><h1>{title}</h1><div className="head-actions">{children}</div></header>;
}
function Login({ navigate }) {
  return <div className="login-screen">
    <section className="login-art"><div className="brand light"><div className="logo">S</div><div><b>Survey Tool</b><small>DefineX</small></div></div>
      <div><h2>Anketlerinizi dakikalar içinde hazırlayın, güvenle dağıtın.</h2><p>Kurum içi ve kurum dışı anketler için kişiye özel linkler, anlık katılım takibi ve tek tıkla raporlama.</p></div><small>© 2026 DefineX</small>
    </section>
    <section className="login-form"><form onSubmit={e => { e.preventDefault(); navigate("list"); }}>
      <div><h1>Giriş Yap</h1><p>Admin hesabınızla devam edin.</p></div>
      <Button onClick={() => navigate("list")}><Icon name="lock"/>Kurumsal hesapla giriş (SSO)</Button><div className="or">veya</div>
      <Field label="E-posta"><div className="input"><input defaultValue="admin@teamdefinex.com"/></div></Field>
      <Field label="Şifre"><div className="input"><input type="password" defaultValue="password"/></div></Field>
      <div className="inline-between muted"><label><input type="checkbox" defaultChecked/> Beni hatırla</label><a>Şifremi unuttum</a></div>
      <Button primary type="submit">Giriş Yap</Button>
    </form></section>
  </div>;
}
function SurveyList({ surveys, navigate }) {
  const [filter, setFilter] = useState("");
  const rows = surveys.filter(s => s.name.toLocaleLowerCase("tr").includes(filter.toLocaleLowerCase("tr")));
  return <Shell active="list" navigate={navigate}>
    <div className="seg"><button className="selected">Anketlerim</button><button onClick={() => navigate("templates")}>Şablonlar</button></div>
    <PageHead title="Anketlerim"><Button onClick={() => navigate("templates")}><Icon name="template"/>Şablondan Oluştur</Button></PageHead>
    <div className="toolbar"><div className="input search"><Icon name="search"/><input placeholder="Anket ara..." value={filter} onChange={e => setFilter(e.target.value)}/></div><Button>Durum</Button><span className="grow"/><Button primary onClick={() => navigate("builder")}><Icon name="plus"/>Yeni Anket</Button></div>
    <div className="panel table-scroll"><table><thead><tr><th>Anket</th><th>Durum</th><th>Bitiş Tarihi</th><th>Yanıt / Katılımcı</th><th className="right">İşlemler</th></tr></thead>
      <tbody>{rows.map((s, i) => { const pct = s.invited ? Math.round(s.responses / s.invited * 100) : 0; return <tr key={i}>
        <td className="title-cell">{s.name}<small>{s.sub}</small></td><td><Badge tone={s.status === "Yayında" ? "blue" : s.status === "Taslak" ? "gray" : s.status === "Kapandı" ? "green" : "amber"}>{s.status}</Badge></td>
        <td><span className="date"><Icon name="calendar"/>{s.end}</span></td><td>{s.invited ? <div className="response"><div className="progress"><span style={{width: `${pct}%`}}/></div>{s.responses}/{s.invited} <small>%{pct}</small></div> : "—"}</td>
        <td><div className="actions"><button title="Sonuçlar" onClick={() => navigate("report")}><Icon name="chart"/></button><button title="Gönder" onClick={() => navigate("participants")}><Icon name="send"/></button><button title="Düzenle" onClick={() => navigate("builder")}><Icon name="edit"/></button></div></td>
      </tr>; })}</tbody></table></div>
    <div className="pager">{rows.length} toplam satır <span className="grow"/>Sayfa başına satır <select><option>20</option><option>50</option></select> Sayfa 1 / 1</div>
  </Shell>;
}
function Builder({ navigate, sections, setSections }) {
  const [title, setTitle] = useState("Eğitim Değerlendirme Anketi");
  const [anon, setAnon] = useState(true);
  const [showQuestion, setShowQuestion] = useState(false);
  return <Shell active="builder" navigate={navigate}>
    <div className="breadcrumb">Anketlerim <Icon name="right"/> Anket Oluşturma</div>
    <PageHead title="Anket Oluşturma"><Button onClick={() => navigate("form")}><Icon name="eye"/>Önizle</Button><Button><Icon name="check"/>Anketi Kaydet</Button><Button primary onClick={() => navigate("participants")}><Icon name="send"/>Gönder</Button></PageHead>
    <div className="builder-grid"><div>
      <section className="panel section-panel">
        <Field label="Anket başlığı"><div className="input"><input value={title} onChange={e => setTitle(e.target.value)}/></div></Field>
        <Field label="Açıklama"><textarea defaultValue="Katıldığınız eğitimi değerlendirmeniz, sonraki eğitimlerin planlanmasında bize yol gösterecektir. Anket yaklaşık 3 dakika sürer."/></Field>
      </section>
      {sections.map((section, si) => <section className="panel section-panel" key={si}>
        <div className="section-heading"><Badge tone="gray">Bölüm {si + 1}</Badge><b>{section.name}</b><button className="icon-action"><Icon name="trash"/></button></div>
        {section.questions.map((q, qi) => <div className="question-row" key={qi}><span className="grip">⠿</span><span className="question-number">{si * 3 + qi + 1}</span><div className="question-copy"><b>{q[0]}{q[2] && <em>*</em>}</b></div><span className={`chip ${q[1] === "Likert" ? "likert" : ""}`}>{q[1]}</span><button className="icon-action"><Icon name="edit"/></button><button className="icon-action"><Icon name="copy"/></button><button className="icon-action"><Icon name="trash"/></button></div>)}
        <Button onClick={() => setShowQuestion(true)}><Icon name="plus"/>Yeni Soru</Button>
      </section>)}
      <Button onClick={() => setSections([...sections, {name: "Yeni Bölüm", questions: []}])}>＋ Yeni Bölüm Ekle</Button>
    </div><aside className="panel settings-panel"><h2>Anket Ayarları</h2><Field label="Bitiş tarihi ve saati"><div className="two-inputs"><input type="date" defaultValue="2026-12-12"/><input type="time" defaultValue="18:00"/></div></Field>
      <div className="inline-between"><div><b>Anonim yanıt</b><small>Yanıtlar kişiyle ilişkilendirilmez</small></div><button className={`toggle ${anon ? "on" : ""}`} onClick={() => setAnon(!anon)} aria-label="Anonim yanıt"/></div>
      <hr/><div className="inline-between"><span>Katılımcılar</span><b>4 kişi</b></div><Button onClick={() => navigate("participants")}>Katılımcıları Yönet</Button><hr/>
      <div className="inline-between"><span>Bölüm</span><b>{sections.length}</b></div><div className="inline-between"><span>Soru</span><b>{sections.reduce((n, s) => n + s.questions.length, 0)}</b></div><div className="inline-between"><span>Zorunlu soru</span><b>4</b></div>
      <Button ghost>Şablon olarak kaydet</Button>
    </aside></div>
    {showQuestion && <div className="modal-backdrop" onClick={() => setShowQuestion(false)}><div className="modal" onClick={e => e.stopPropagation()}><div className="modal-title"><div><h2>Yeni Bir Anket Sorusu Oluşturma</h2><p>Bölüm: {sections[0]?.name}</p></div><button onClick={() => setShowQuestion(false)}>×</button></div><div className="modal-content"><Field label="Soru"><textarea placeholder="Soru metnini yazın..."/></Field><Field label="Cevap Tipi"><select><option>Cevap Tipi Seçiniz...</option><option>Likert</option><option>Tekli Seçim</option><option>Çoklu Seçim</option><option>Serbest Metin</option></select></Field><label><input type="checkbox"/> Zorunlu soru</label></div><div className="modal-footer"><Button onClick={() => setShowQuestion(false)}>Vazgeç</Button><Button primary onClick={() => setShowQuestion(false)}>Oluştur</Button></div></div></div>}
  </Shell>;
}
function Participants({ navigate }) {
  const [emails, setEmails] = useState(["ayse.yilmaz@teamdefinex.com", "mehmet.kaya@teamdefinex.com", "joe@mail.com", "jack@mail.com"]);
  const [email, setEmail] = useState("");
  return <Shell active="builder" navigate={navigate}><div className="breadcrumb">Anketlerim <Icon name="right"/> Katılımcılar</div><PageHead title="Katılımcılar"><Button onClick={() => navigate("builder")}>Geri</Button><Button primary onClick={() => navigate("sent")}><Icon name="send"/>Tamam ve Gönder</Button></PageHead>
    <section className="panel participant-panel"><p>Her katılımcıya kişiye özel, tek kullanımlık link gönderilir.</p><Field label="E-posta"><div className="input"><input placeholder="user@mail.com" value={email} onChange={e => setEmail(e.target.value)}/><Button primary onClick={() => {if(email.includes("@")) {setEmails([...emails, email]);setEmail("");}}}>Ekle</Button></div></Field>
      <div className="upload-box">⇧ <div><b>CSV dosyası yükleyin</b> veya sürükleyip bırakın<small>Tek sütun: email · Örnek dosyayı indir</small></div></div>
      <div className="inline-between"><b>{emails.length} katılımcı</b><button className="text-danger" onClick={() => setEmails([])}>Tümünü temizle</button></div>
      <div className="email-list">{emails.map((m, i) => <div key={`${m}-${i}`}><span className="avatar mini">{m.slice(0,2).toUpperCase()}</span>{m}<button className="text-danger" onClick={() => setEmails(emails.filter((_, j) => j !== i))}>Sil</button></div>)}</div>
    </section></Shell>;
}
function Report({ navigate }) {
  const data = [[2,3,5,11,7],[1,2,6,12,7],[0,1,4,9,14]];
  const questions = ["Eğitim içeriğinin yeterli olduğunu düşünüyorum.", "Eğitimde kullanılan materyaller anlaşılırdı.", "Eğitmen konuya hakimdi."];
  const labels = ["Kesinlikle Katılmıyorum", "Katılmıyorum", "Normal", "Katılıyorum", "Kesinlikle Katılıyorum"];
  return <Shell active="report" navigate={navigate}><div className="breadcrumb">Anketlerim <Icon name="right"/> Rapor</div><PageHead title="Eğitim Anketi Raporu"><Button>↓ CSV İndir</Button><Button primary>↓ Export PDF</Button></PageHead>
    <div className="kpis"><div className="panel kpi hero-kpi"><span>Memnuniyet Ortalaması</span><b>4.2</b><small>5 üzerinden</small></div><div className="panel kpi"><span>Katılım Oranı</span><b>%70</b><small>▲ %8 geçen döneme göre</small></div><div className="panel kpi"><span>Davet / Yanıt</span><b>40 / 28</b><small>12 kişi bekleniyor</small></div><div className="panel kpi"><span>Ort. Tamamlama</span><b>3 dk 12 sn</b><small>medyan 2 dk 48 sn</small></div></div>
    {data.map((values, i) => <section className="panel report-card" key={i}><div className="report-heading"><div><h2>{questions[i]}</h2><small>Likert · {values.reduce((a,b)=>a+b,0)} yanıt</small></div><div className="average">4.2<small>Ortalama / 5</small></div></div>{values.map((v, j) => <div className="bar-row" key={j}><span>{labels[j]}</span><div className="bar-track"><span style={{width: `${v / Math.max(...values) * 100}%`, opacity: .4 + j * .13}}/></div><small>{v} · %{Math.round(v / values.reduce((a,b)=>a+b,0) * 100)}</small></div>)}</section>)}
    <section className="panel report-card"><h2>Eğitimle ilgili eklemek istedikleriniz.</h2><div className="free-answer">Hazelcast kısmı için bir gün daha ayrılabilir, uygulamalar çok faydalıydı.</div><div className="free-answer">Örnek proje kodlarının eğitim sonrası paylaşılması harika olur.</div><div className="free-answer">Salon biraz soğuktu, onun dışında her şey çok iyiydi.</div></section>
  </Shell>;
}
function Templates({ navigate }) {
  const templates = [["Eğitim Değerlendirme","8 soru · 2 bölüm","Akademi"],["Çalışan Memnuniyeti","24 soru · 5 bölüm","People & Culture"],["Müşteri Memnuniyeti (NPS)","6 soru · 1 bölüm","Satış"],["Onboarding Geri Bildirimi","12 soru · 3 bölüm","People & Culture"]];
  return <Shell active="templates" navigate={navigate}><div className="seg"><button onClick={() => navigate("list")}>Anketlerim</button><button className="selected">Şablonlar</button></div><PageHead title="Şablonlar"><Button primary onClick={() => navigate("builder")}><Icon name="plus"/>Yeni Şablon</Button></PageHead><div className="template-grid">{templates.map(t => <section className="panel template-card" key={t[0]}><div className="template-icon"><Icon name="template"/></div><h2>{t[0]}</h2><span>{t[1]}</span><Badge tone="gray">{t[2]}</Badge><Button onClick={() => navigate("builder")}>Bu şablonu kullan</Button></section>)}</div></Shell>;
}
function PublicForm({ navigate }) {
  const [choice, setChoice] = useState(-1);
  return <div className="public-screen"><header className="public-top"><div className="logo">S</div><b>Survey Tool</b><span><Icon name="lock"/>Güvenli bağlantı</span></header><div className="public-wrap"><h1>Eğitim Değerlendirme Anketi</h1><p className="lead">Katıldığınız eğitimi değerlendirmeniz, sonraki eğitimlerin planlanmasında bize yol gösterecektir. <b>Yanıtlarınız anonimdir.</b></p><div className="progress-label">1 / 6 <div className="progress"><span style={{width:"17%"}}/></div> %17</div><section className="panel form-card"><Badge tone="gray">Eğitim İçeriği</Badge><h2><span className="question-number">1</span>Eğitim içeriğinin yeterli olduğunu düşünüyorum.<em>*</em></h2><div className="likert-options">{["Kesinlikle Katılmıyorum","Katılmıyorum","Normal","Katılıyorum","Kesinlikle Katılıyorum"].map((v,i)=><button className={choice === i ? "chosen" : ""} onClick={() => setChoice(i)} key={v}><span className="radio-dot"/>{v}</button>)}</div><div className="form-footer"><span>☁ Otomatik kaydedildi</span><Button primary onClick={() => navigate("thanks")}>İleri <Icon name="right"/></Button></div></section></div></div>;
}
function PublicState({ expired = false, navigate }) {
  return <div className="public-screen"><header className="public-top"><div className="logo">S</div><b>Survey Tool</b><span><Icon name="lock"/>Güvenli bağlantı</span></header><div className="public-wrap"><section className="panel state-card"><div className={`state-icon ${expired ? "warning" : ""}`}><Icon name="check"/></div><h2>{expired ? "Bu anketin süresi dolmuştur" : "Cevaplarınız başarıyla gönderildi."}</h2><p>{expired ? "Anket 12.12.2026 18:00 itibarıyla yanıtlara kapanmıştır. Bir sorun olduğunu düşünüyorsanız anket sahibiyle iletişime geçebilirsiniz." : "Zaman ayırdığınız için teşekkür ederiz. Geri bildiriminiz sonraki eğitimlerin planlanmasında kullanılacaktır. Bu sayfayı kapatabilirsiniz."}</p><Button onClick={() => navigate("list")}>Anketlerime dön</Button></section></div></div>;
}
function SimplePage({ title, navigate }) {
  return <Shell active="dashboard" navigate={navigate}><PageHead title={title}/><section className="panel state-card"><div className="state-icon"><Icon name="check"/></div><h2>{title}</h2><p>Bu alanın içerikleri uygulamanın sonraki geliştirme aşamasında eklenecektir.</p></section></Shell>;
}

export default function SurveyTool() {
  const [screen, setScreen] = useState("login");
  const [surveys] = useState(initialSurveys);
  const [sections, setSections] = useState(initialSections);
  const navigate = (next) => { setScreen(next); window.scrollTo({top: 0, behavior: "smooth"}); };
  const screenContent = {
    login: <Login navigate={navigate}/>,
    list: <SurveyList surveys={surveys} navigate={navigate}/>,
    builder: <Builder navigate={navigate} sections={sections} setSections={setSections}/>,
    question: <Builder navigate={navigate} sections={sections} setSections={setSections}/>,
    participants: <Participants navigate={navigate}/>,
    sent: <PublicState navigate={navigate}/>,
    report: <Report navigate={navigate}/>,
    templates: <Templates navigate={navigate}/>,
    form: <PublicForm navigate={navigate}/>,
    thanks: <PublicState navigate={navigate}/>,
    expired: <PublicState expired navigate={navigate}/>,
    dashboard: <SimplePage title="Dashboard" navigate={navigate}/>,
    people: <SimplePage title="Katılımcı Listeleri" navigate={navigate}/>,
  };
  return <div className="survey-tool">
    <style>{styles}</style>
    {screenContent[screen] || screenContent.login}
    <div className="screen-switcher"><span>Prototip ekranı</span><button onClick={() => navigate(screens[(screens.findIndex(s => s[0] === screen) + screens.length - 1) % screens.length][0])}>‹</button><select value={screen} onChange={e => navigate(e.target.value)}>{screens.map(([key, label]) => <option value={key} key={key}>{label}</option>)}</select><button onClick={() => navigate(screens[(screens.findIndex(s => s[0] === screen) + 1) % screens.length][0])}>›</button></div>
  </div>;
}

const styles = `
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
:root{--primary:#1E4FD8;--primary-hover:#1A44BC;--primary-soft:#E7EDFC;--primary-text:#1E40AF;--bg:#F7F8FA;--surface:#fff;--sidebar:#FAFBFC;--border:#E4E7EC;--border-strong:#D0D5DD;--text:#101828;--muted:#667085;--subtle:#98A2B3;--danger:#D92D20;--success:#079455;--warn:#B54708;--radius:10px;--shadow:0 1px 2px rgba(16,24,40,.05)}
.survey-tool{min-height:100vh;background:var(--bg);color:var(--text);font:14px Inter,system-ui,sans-serif;-webkit-font-smoothing:antialiased}.survey-tool *{box-sizing:border-box}.survey-tool button,.survey-tool input,.survey-tool select,.survey-tool textarea{font:inherit;color:inherit}.survey-tool button{cursor:pointer}.i{width:16px;height:16px;stroke:currentColor;fill:none;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round;flex:none}
.app-shell{display:flex;min-height:100vh}.sidebar{width:250px;flex:none;background:var(--sidebar);border-right:1px solid var(--border);display:flex;flex-direction:column;padding:14px 8px;min-height:100vh}.brand{display:flex;align-items:center;gap:10px;padding:4px 6px 22px}.brand b{display:block;font-size:14px;font-weight:600}.brand small{display:block;color:var(--subtle);font-size:10px;margin-top:3px}.logo{width:32px;height:32px;border-radius:8px;background:var(--primary);color:#fff;display:grid;place-items:center;font-weight:700}.nav-label{color:var(--subtle);font-size:12px;padding:14px 8px 6px}.nav-item{width:100%;display:flex;align-items:center;gap:10px;padding:10px 9px;border:0;background:transparent;border-radius:8px;text-align:left;font-weight:500}.nav-item:hover{background:#F2F4F7}.nav-item.active{background:var(--primary-soft);color:var(--primary)}.sidebar-bottom{margin-top:auto}.count{margin-left:auto;background:#C4320A;color:white;border-radius:99px;padding:2px 7px;font-size:10px}.profile{display:flex;align-items:center;gap:10px;background:#F2F4F7;border-radius:10px;padding:9px;margin-top:8px}.profile b,.profile small{display:block}.profile b{font-size:13px;font-weight:500}.profile small{font-size:10px;color:var(--subtle);margin-top:3px}.avatar{width:30px;height:30px;border-radius:50%;background:#344054;color:#fff;display:grid;place-items:center;font-size:11px;font-weight:600}.main-content{flex:1;min-width:0;padding:24px 26px 90px}
h1,h2,p{margin-top:0}h1{font-size:24px;line-height:1.25;font-weight:700;letter-spacing:-.01em;margin-bottom:0}h2{font-size:16px;font-weight:600;margin-bottom:0}.page-head{display:flex;align-items:center;justify-content:space-between;gap:16px;margin:22px 0 26px;flex-wrap:wrap}.head-actions{display:flex;gap:10px;flex-wrap:wrap}.btn{display:inline-flex;align-items:center;justify-content:center;gap:8px;border:1px solid var(--border);background:#fff;padding:8px 14px;border-radius:8px;font-weight:500;white-space:nowrap;box-shadow:var(--shadow);min-height:36px}.btn:hover{background:#F9FAFB}.btn.primary{background:var(--primary);border-color:var(--primary);color:#fff;font-weight:600}.btn.primary:hover{background:var(--primary-hover)}.btn.ghost{border-color:transparent;background:transparent;box-shadow:none;color:var(--primary)}.panel{background:var(--surface);border:1px solid var(--border);border-radius:12px}.seg{display:inline-flex;background:#F0F2F5;border-radius:10px;padding:4px;gap:2px}.seg button{border:0;background:transparent;padding:6px 12px;border-radius:7px;color:var(--muted)}.seg button.selected{background:#fff;color:var(--text);box-shadow:var(--shadow)}.toolbar{display:flex;gap:12px;align-items:center;margin-bottom:24px;flex-wrap:wrap}.grow{flex:1}.input{display:flex;align-items:center;gap:8px;border:1px solid var(--border);background:#fff;border-radius:8px;padding:0 12px;height:38px;box-shadow:var(--shadow)}.input input{border:0;outline:0;flex:1;min-width:0;width:100%;height:100%;background:transparent}.input .i{color:var(--subtle)}.search{width:380px;max-width:100%}.field{display:flex;flex-direction:column;gap:6px;font-size:13px;font-weight:500}.field>span{font-size:13px}.field textarea,.modal-content select{border:1px solid var(--border);border-radius:8px;padding:10px 12px;min-height:84px;resize:vertical;outline:0;width:100%;background:#fff}.field textarea:focus{border-color:var(--primary);box-shadow:0 0 0 3px #1e4fd81f}.badge{display:inline-flex;align-items:center;font-size:12px;font-weight:600;padding:4px 10px;border-radius:999px;white-space:nowrap}.badge.blue{background:var(--primary-soft);color:var(--primary-text)}.badge.gray{background:#F2F4F7;color:#475467}.badge.green{background:#ECFDF3;color:var(--success)}.badge.amber{background:#FFFAEB;color:var(--warn)}.table-scroll{overflow-x:auto}.survey-tool table{width:100%;border-collapse:collapse;min-width:760px}.survey-tool th{text-align:left;font-weight:500;padding:14px 16px;border-bottom:1px solid var(--border);white-space:nowrap}.survey-tool td{padding:17px 16px;border-bottom:1px solid var(--border);vertical-align:middle}.survey-tool tbody tr:last-child td{border-bottom:0}.survey-tool tbody tr:hover{background:#FCFCFD}.title-cell{font-weight:500}.title-cell small{display:block;color:var(--muted);font-size:12px;margin-top:4px;font-weight:400}.date{display:inline-flex;align-items:center;gap:8px;color:#475467}.date .i{color:var(--subtle)}.response{display:flex;align-items:center;gap:9px;color:#475467;white-space:nowrap}.response small{color:var(--subtle)}.progress{height:6px;border-radius:999px;background:#EAECF0;overflow:hidden;width:120px}.progress span{display:block;height:100%;background:var(--primary);border-radius:999px}.actions{display:flex;justify-content:flex-end;gap:5px}.actions button,.icon-action{border:0;background:transparent;color:var(--muted);width:30px;height:30px;border-radius:7px;display:inline-grid;place-items:center}.actions button:hover,.icon-action:hover{background:#F2F4F7;color:var(--text)}.right{text-align:right}.pager{display:flex;align-items:center;gap:12px;padding:20px 8px;color:var(--muted);flex-wrap:wrap}.pager select{border:1px solid var(--border);border-radius:7px;padding:6px;background:#fff}.breadcrumb{display:flex;align-items:center;gap:8px;color:var(--muted);font-size:13px}.builder-grid{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:20px;align-items:start}.section-panel{padding:18px;margin-bottom:16px;display:flex;flex-direction:column;gap:14px}.section-heading{display:flex;align-items:center;gap:10px}.section-heading>b{font-size:14px}.section-heading .icon-action{margin-left:auto}.question-row{display:flex;align-items:center;gap:11px;padding:12px 8px;border:1px solid var(--border);border-radius:10px;background:#fff}.grip{color:var(--subtle);font-size:20px}.question-number{width:26px;height:26px;border-radius:7px;background:#F2F4F7;display:grid;place-items:center;font-size:12px;font-weight:600;color:#475467;flex:none}.question-copy{flex:1;min-width:0}.question-copy b{font-weight:500;line-height:1.4}.question-copy em,.form-card em{color:var(--danger);font-style:normal}.chip{font-size:11px;font-weight:600;padding:3px 8px;border-radius:6px;background:#F4EBFF;color:#6941C6;white-space:nowrap}.chip.likert{background:#E7EDFC;color:#1E40AF}.settings-panel{padding:18px;display:flex;flex-direction:column;gap:16px;position:sticky;top:20px}.settings-panel small{display:block;color:var(--muted);font-size:12px;margin-top:4px}.settings-panel hr{border:0;border-top:1px solid var(--border);width:100%}.two-inputs{display:grid;grid-template-columns:1fr 1fr;gap:8px}.two-inputs input{min-width:0;width:100%;height:38px;border:1px solid var(--border);border-radius:8px;padding:0 8px}.inline-between{display:flex;align-items:center;justify-content:space-between;gap:12px}.toggle{position:relative;width:36px;height:20px;border:0;border-radius:99px;background:#D0D5DD;flex:none}.toggle:after{content:"";position:absolute;top:2px;left:2px;width:16px;height:16px;border-radius:50%;background:#fff;box-shadow:0 1px 2px #0003;transition:.15s}.toggle.on{background:var(--primary)}.toggle.on:after{left:18px}.modal-backdrop{position:fixed;inset:0;background:#10182873;display:grid;place-items:center;padding:16px;z-index:50}.modal{width:520px;max-width:100%;background:#fff;border-radius:14px;box-shadow:0 20px 40px #1018282e;overflow:hidden}.modal-title,.modal-footer{display:flex;align-items:center;justify-content:space-between;padding:20px 22px}.modal-title p{font-size:13px;color:var(--muted);margin:5px 0 0}.modal-title button{border:0;background:transparent;font-size:24px}.modal-content{padding:4px 22px 20px;display:flex;flex-direction:column;gap:16px}.modal-footer{justify-content:flex-end;gap:10px;padding-top:0}.participant-panel{max-width:720px;padding:22px;display:flex;flex-direction:column;gap:18px}.participant-panel>p{color:var(--muted);margin:0}.upload-box{border:1px dashed var(--border-strong);border-radius:10px;padding:18px;display:flex;gap:14px;align-items:center;color:var(--muted)}.upload-box b,.upload-box small{display:block}.upload-box b{color:var(--primary)}.upload-box small{font-size:12px;margin-top:5px}.email-list{border:1px solid var(--border);border-radius:10px;overflow:hidden}.email-list>div{display:flex;align-items:center;gap:10px;padding:9px 12px;border-bottom:1px solid var(--border)}.email-list>div:last-child{border-bottom:0}.email-list button{margin-left:auto}.avatar.mini{width:26px;height:26px;font-size:10px}.text-danger{border:0;background:transparent;color:var(--danger)}.kpis{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:16px;margin-bottom:20px}.kpi{padding:18px;display:flex;flex-direction:column;gap:10px}.kpi span{color:var(--muted);font-size:13px}.kpi b{font-size:28px;letter-spacing:-.02em}.kpi small{color:var(--success)}.hero-kpi{background:var(--primary);border-color:var(--primary);color:#fff}.hero-kpi span,.hero-kpi small{color:#C7D5FA}.report-card{padding:20px;margin-bottom:16px}.report-heading{display:flex;justify-content:space-between;gap:16px;align-items:flex-start;margin-bottom:18px}.report-heading small{display:block;color:var(--muted);margin-top:6px}.average{text-align:right;font-size:22px;color:var(--primary);font-weight:700}.average small{font-size:12px;color:var(--muted);font-weight:400}.bar-row{display:grid;grid-template-columns:170px 1fr 80px;gap:12px;align-items:center;font-size:13px;margin-top:10px}.bar-track{height:22px;background:#F2F4F7;border-radius:6px;overflow:hidden}.bar-track span{display:block;height:100%;background:var(--primary);border-radius:6px}.bar-row>small{text-align:right;color:var(--muted)}.free-answer{padding:12px 14px;border-radius:10px;background:#F9FAFB;border:1px solid var(--border);margin-top:10px;color:#344054;line-height:1.5}.template-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:16px}.template-card{padding:18px;display:flex;flex-direction:column;gap:12px}.template-card>span:not(.badge){color:var(--muted)}.template-card .badge{align-self:flex-start}.template-card .btn{margin-top:6px}.template-icon{width:36px;height:36px;border-radius:8px;background:var(--primary-soft);color:var(--primary);display:grid;place-items:center}.login-screen{min-height:100vh;display:grid;grid-template-columns:1fr 1fr;background:#fff}.login-art{background:var(--primary);color:#fff;padding:48px;display:flex;flex-direction:column;justify-content:space-between;position:relative;overflow:hidden}.login-art:after{content:"";position:absolute;right:-120px;bottom:-120px;width:420px;height:420px;border-radius:50%;border:60px solid #ffffff12}.login-art h2{font-size:30px;line-height:1.25;max-width:420px}.login-art p{color:#C7D5FA;max-width:400px;line-height:1.6}.brand.light .logo{background:#fff;color:var(--primary)}.brand.light small{color:#C7D5FA}.login-form{display:grid;place-items:center;padding:24px}.login-form form{width:360px;max-width:100%;display:flex;flex-direction:column;gap:16px}.login-form p{color:var(--muted);margin:7px 0 0}.or{display:flex;align-items:center;gap:10px;color:var(--subtle);font-size:12px}.or:before,.or:after{content:"";flex:1;border-top:1px solid var(--border)}.muted{color:var(--muted)}.inline-between a{color:var(--primary)}.public-screen{min-height:100vh;background:var(--bg)}.public-top{background:#fff;border-bottom:1px solid var(--border);padding:14px 24px;display:flex;align-items:center;gap:10px}.public-top>span{margin-left:auto;color:var(--muted);font-size:13px;display:flex;align-items:center;gap:6px}.public-wrap{max-width:680px;margin:0 auto;padding:36px 16px 100px}.public-wrap h1{font-size:22px}.lead{color:var(--muted);margin:7px 0 22px;line-height:1.5}.progress-label{display:flex;align-items:center;gap:12px;margin-bottom:18px;color:var(--muted);font-size:13px}.progress-label .progress{flex:1;width:auto;height:8px}.form-card{padding:26px}.form-card h2{display:flex;align-items:center;gap:12px;line-height:1.45;margin-top:16px}.likert-options{display:grid;grid-template-columns:repeat(5,1fr);gap:8px;margin-top:22px}.likert-options button{display:flex;flex-direction:column;align-items:center;gap:9px;padding:14px 6px;border:1px solid var(--border);border-radius:10px;background:#fff;font-size:12px;line-height:1.35}.likert-options button.chosen{border-color:var(--primary);background:var(--primary-soft)}.radio-dot{width:18px;height:18px;border-radius:50%;border:2px solid var(--border-strong)}.chosen .radio-dot{border-color:var(--primary);background:var(--primary);box-shadow:inset 0 0 0 4px var(--primary-soft)}.form-footer{display:flex;align-items:center;justify-content:space-between;margin-top:26px}.form-footer>span{font-size:12px;color:var(--muted)}.state-card{padding:44px 28px;text-align:center;margin-top:28px}.state-card h2{font-size:20px;margin:20px 0 8px}.state-card p{color:var(--muted);max-width:420px;line-height:1.55;margin:0 auto 20px}.state-icon{width:56px;height:56px;border-radius:50%;background:#ECFDF3;color:var(--success);display:grid;place-items:center;margin:0 auto;box-shadow:0 0 0 8px #F6FEF9}.state-icon.warning{background:#FFFAEB;color:var(--warn)}.screen-switcher{position:fixed;left:50%;bottom:16px;transform:translateX(-50%);background:#101828;color:#fff;border-radius:12px;padding:6px 6px 6px 14px;display:flex;align-items:center;gap:10px;box-shadow:0 10px 30px #1018284d;z-index:60;font-size:13px;max-width:calc(100vw - 32px)}.screen-switcher select,.screen-switcher button{background:#1D2939;border:1px solid #344054;color:#fff;border-radius:8px;padding:7px 9px}.screen-switcher button{width:32px;height:32px;padding:0;font-size:20px}
@media(max-width:1100px){.builder-grid{grid-template-columns:1fr}.settings-panel{position:static}.kpis{grid-template-columns:repeat(2,1fr)}}@media(max-width:820px){.sidebar{display:none}.main-content{padding:16px 16px 90px}.login-screen{grid-template-columns:1fr}.login-art{display:none}.search{width:100%}.bar-row{grid-template-columns:110px 1fr 54px}.likert-options{grid-template-columns:1fr}.likert-options button{flex-direction:row;text-align:left;padding:12px 14px;font-size:14px}.screen-switcher>span{display:none}}@media(max-width:560px){.kpis{grid-template-columns:1fr 1fr}.bar-row{grid-template-columns:1fr 1fr 48px}.question-row{flex-wrap:wrap}.question-copy{flex-basis:65%}.chip{margin-left:37px}.report-heading{flex-direction:column}.average{text-align:left}.screen-switcher select{max-width:190px}}
`;
