import ComingSoon from "./ComingSoon.jsx";

/**
 * Sekmeler. variant: "line" (alt çizgili) | "seg" (prototipteki segment görünümü)
 * items: [{ value, label, count?, disabled? }]  — disabled sekmeler "Yakında" olarak gösterilir.
 */
export default function Tabs({ items, value, onChange, variant = "line" }) {
  return <div className={variant === "seg" ? "seg" : "tabs"} role="tablist">
    {items.map(item => {
      const selected = item.value === value;
      const btn = <button key={item.value} type="button" role="tab" aria-selected={selected} disabled={item.disabled}
        className={selected ? (variant === "seg" ? "selected" : "active") : undefined}
        onClick={() => onChange?.(item.value)}>
        {item.label}{item.count !== undefined && <span className="count-pill">{item.count}</span>}
      </button>;
      return item.disabled ? <ComingSoon key={item.value}>{btn}</ComingSoon> : btn;
    })}
  </div>;
}
