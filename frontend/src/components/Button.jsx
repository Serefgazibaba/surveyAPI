import { Link } from "react-router-dom";
import Icon from "./Icon.jsx";
import Spinner from "./Spinner.jsx";

/**
 * Tek buton bileşeni.
 * variant: "default" | "primary" | "ghost" | "danger"
 * size: "md" | "sm"
 * icon: Icon adı (solda), loading: yükleniyor göstergesi + disabled
 * to: verilirse react-router Link olarak çizilir.
 */
export default function Button({ children, variant = "default", size = "md", icon, loading = false, disabled, to, type = "button", className = "", ...props }) {
  const cls = ["btn", variant !== "default" && variant, size === "sm" && "sm", className].filter(Boolean).join(" ");
  const content = <>{loading ? <Spinner /> : icon && <Icon name={icon} />}{children}</>;
  if (to && !disabled) return <Link className={cls} to={to} {...props}>{content}</Link>;
  return <button type={type} className={cls} disabled={disabled || loading} aria-busy={loading || undefined} {...props}>{content}</button>;
}
