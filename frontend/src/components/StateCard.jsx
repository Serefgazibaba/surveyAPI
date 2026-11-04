import Icon from "./Icon.jsx";

const ICONS = { success: "check", warning: "alert", error: "alert", info: "info" };

/** Ortalanmış durum kartı (başarı, uyarı, hata, bilgi). actions: alt butonlar. */
export default function StateCard({ tone = "success", title, children, actions }) {
  return <section className="panel state-card">
    <div className={`state-icon${tone === "success" ? "" : ` ${tone}`}`}><Icon name={ICONS[tone]} /></div>
    <h2>{title}</h2>
    {children && <p>{children}</p>}
    {actions && <div className="state-actions">{actions}</div>}
  </section>;
}
