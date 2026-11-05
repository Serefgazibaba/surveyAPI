import Shell from "./Shell.jsx";
import PageHead from "./PageHead.jsx";
import Icon from "./Icon.jsx";

/** Henüz içeriği olmayan menü sayfaları: başlık + ortalanmış ikon, başlık ve açıklama. */
export default function PlaceholderPage({ title, icon, text }) {
  return <Shell>
    <PageHead title={title} />
    <section className="panel placeholder-card">
      <div className="placeholder-icon"><Icon name={icon} /></div>
      <h2>{title}</h2>
      <p>{text}</p>
    </section>
  </Shell>;
}
