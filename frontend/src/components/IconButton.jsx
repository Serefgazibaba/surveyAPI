import { Link } from "react-router-dom";
import Icon from "./Icon.jsx";

/** Sadece ikonlu küçük buton (tablo/soru satırı aksiyonları). `label` hem tooltip hem erişilebilir ad. */
export default function IconButton({ icon, label, to, onClick, disabled, danger = false }) {
  const cls = `icon-action${danger ? " text-danger" : ""}`;
  if (to && !disabled) return <Link className={cls} to={to} title={label} aria-label={label}><Icon name={icon} /></Link>;
  return <button type="button" className={cls} title={label} aria-label={label} onClick={onClick} disabled={disabled}><Icon name={icon} /></button>;
}
