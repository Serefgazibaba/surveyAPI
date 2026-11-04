import { useEffect, useRef, useState } from "react";
import Button from "./Button.jsx";
import Checkbox from "./Checkbox.jsx";

/**
 * Çoklu seçim filtresi (açılır menü). options: [{ value, label }], value: string[]
 * onChange(nextValues)
 */
export default function FilterMenu({ label, options, value = [], onChange }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  useEffect(() => {
    if (!open) return;
    const close = e => { if (!ref.current?.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, [open]);
  const toggle = v => onChange(value.includes(v) ? value.filter(x => x !== v) : [...value, v]);
  return <div className="popover-wrap" ref={ref}>
    <Button icon="filter" onClick={() => setOpen(o => !o)} aria-expanded={open}>
      {label}{value.length > 0 && ` (${value.length})`}
    </Button>
    {open && <div className="popover">
      {options.map(o => <Checkbox key={o.value} label={o.label} checked={value.includes(o.value)} onChange={() => toggle(o.value)} />)}
      {value.length > 0 && <Button size="sm" variant="ghost" onClick={() => onChange([])}>Temizle</Button>}
    </div>}
  </div>;
}
