import { NavLink, useLocation } from "react-router-dom";
import Icon from "./Icon.jsx";
import ComingSoon from "./ComingSoon.jsx";
import { useAuth } from "../auth/AuthContext.jsx";

// [yol | null (yakında), ikon, etiket]
const SECTIONS = [
  ["Benim Alanım", [[null, "home", "Dashboard"], ["/surveys", "list", "Anketlerim"], ["/surveys/new", "plus", "Anket Oluştur"], [null, "template", "Şablonlar"]]],
  ["Analiz", [[null, "chart", "Raporlar"], ["/mails", "mail", "Gönderim Durumu"], [null, "users", "Katılımcı Listeleri"]]],
];

function initials(name = "") {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0]?.toLocaleUpperCase("tr")).join("") || "?";
}

function NavItem({ to, icon, label }) {
  const { pathname } = useLocation();
  if (!to) return <ComingSoon block><button type="button" className="nav-item" disabled><Icon name={icon} />{label}</button></ComingSoon>;
  // "Anketlerim" anket alt sayfalarında da aktif kalır, "Anket Oluştur" hariç.
  const active = to === "/surveys" ? pathname.startsWith("/surveys") && pathname !== "/surveys/new" : pathname === to;
  return <NavLink to={to} className={`nav-item${active ? " active" : ""}`}><Icon name={icon} />{label}</NavLink>;
}

/** Admin sayfa iskeleti: sol menü + içerik. */
export default function Shell({ children }) {
  const { user, logout } = useAuth();
  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><div className="logo">S</div><div><b>Survey Tool</b><small>DefineX</small></div></div>
      {SECTIONS.map(([title, items]) => <div key={title}>
        <div className="nav-label">{title}</div>
        {items.map(([to, icon, label]) => <NavItem key={label} to={to} icon={icon} label={label} />)}
      </div>)}
      <div className="sidebar-bottom">
        <div className="profile">
          <div className="avatar">{initials(user?.fullName)}</div>
          <div><b>{user?.fullName}</b><small>{user?.email}</small></div>
          <button type="button" className="logout" title="Çıkış yap" aria-label="Çıkış yap" onClick={logout}><Icon name="logout" /></button>
        </div>
      </div>
    </aside>
    <main className="main-content">{children}</main>
  </div>;
}
