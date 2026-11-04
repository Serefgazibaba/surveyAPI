import Icon from "./Icon.jsx";

const ICONS = { error: "alert", warning: "alert", info: "info", success: "check" };

/** Satır içi bilgi/uyarı kutusu. tone: "info" | "success" | "warning" | "error" */
export default function Alert({ tone = "info", title, children, actions }) {
  return <div className={`alert ${tone}`} role={tone === "error" ? "alert" : "status"}>
    <Icon name={ICONS[tone]} />
    <div className="alert-body">
      {title && <b>{title}</b>}
      {children}
      {actions && <div className="alert-actions">{actions}</div>}
    </div>
  </div>;
}
