import Icon from "./Icon.jsx";

/** Katılımcı (public) sayfa iskeleti: üst bar + ortalanmış içerik. `banner` üstte ince uyarı şeridi. */
export default function PublicLayout({ children, banner }) {
  return <div className="public-screen">
    {banner && <div className="preview-banner">{banner}</div>}
    <header className="public-top"><div className="logo">S</div><b>Survey Tool</b><span><Icon name="lock" />Güvenli bağlantı</span></header>
    <div className="public-wrap">{children}</div>
  </div>;
}
