import { NavLink, useLocation } from "react-router-dom";
import Icon from "./Icon.jsx";
import { useAuth } from "../auth/AuthContext.jsx";
import { useNotificationCount } from "../hooks/useNotifications.js";

// [yol, ikon, etiket]
const SECTIONS = [
  ["Benim Alanım", [["/dashboard", "home", "Dashboard"], ["/surveys", "list", "Anketlerim"], ["/surveys/new", "plus", "Anket Oluştur"], ["/templates", "template", "Şablonlar"]]],
  ["Analiz", [["/reports", "chart", "Raporlar"], ["/mails", "mail", "Gönderim Durumu"], ["/participant-lists", "users", "Katılımcı Listeleri"]]],
];

// Alt sayfalarında da aktif kalan menü öğeleri ("Anketlerim", "Anket Oluştur" hariç).
const PREFIX_ACTIVE = ["/templates", "/reports"];

function initials(name = "") {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0]?.toLocaleUpperCase("tr")).join("") || "?";
}

function NavItem({ to, icon, label, badge }) {
  const { pathname } = useLocation();
  const active = to === "/surveys" ? pathname.startsWith("/surveys") && pathname !== "/surveys/new"
    : PREFIX_ACTIVE.includes(to) ? pathname.startsWith(to) : pathname === to;
  return <NavLink to={to} className={`nav-item${active ? " active" : ""}`}>
    <Icon name={icon} />{label}
    {badge > 0 && <span className="nav-badge" aria-label={`${badge} yeni`}>{badge > 99 ? "99+" : badge}</span>}
  </NavLink>;
}

/** Admin sayfa iskeleti: sol menü (ekran yüksekliğinde, sabit) + içerik. */
export default function Shell({ children }) {
  const { user, logout } = useAuth();
  const notifications = useNotificationCount();
  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><div className="logo">S</div><div><b>Survey Tool</b><small>DefineX</small></div></div>
      <nav className="sidebar-nav">
        {SECTIONS.map(([title, items]) => <div key={title}>
          <div className="nav-label">{title}</div>
          {items.map(([to, icon, label]) => <NavItem key={label} to={to} icon={icon} label={label} />)}
        </div>)}
      </nav>
      <div className="sidebar-bottom">
        <NavItem to="/whats-new" icon="sparkle" label="Yenilikler" />
        <NavItem to="/notifications" icon="bell" label="Bildirimler" badge={notifications} />
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
