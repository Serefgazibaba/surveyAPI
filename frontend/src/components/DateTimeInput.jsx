import { useState } from "react";
import { toZonedInput, fromZonedInput } from "../utils/datetime.js";

/**
 * Tarih + saat girişi. Değer UTC ISO string'dir; kullanıcı İstanbul saatiyle girer.
 * Saat otomatik doldurulmaz: tarih ve saat ikisi de girilene kadar onChange(null) çağrılır,
 * yarım giriş (yalnızca tarih veya saat) bileşenin içinde korunur.
 * onChange(isoOrNull)
 */
export default function DateTimeInput({ value, onChange, disabled, invalid = false }) {
  const [local, setLocal] = useState(() => toZonedInput(value));
  const [lastValue, setLastValue] = useState(value);
  // Değer dışarıdan değiştiyse (ör. kayıttan dönen yanıt) yerel girişi ona göre güncelle.
  if (value !== lastValue) {
    setLastValue(value);
    if (value) setLocal(toZonedInput(value));
  }

  const update = patch => {
    const next = { ...local, ...patch };
    setLocal(next);
    const iso = next.date && next.time ? fromZonedInput(next.date, next.time) : null;
    setLastValue(iso);
    onChange?.(iso);
  };

  const cls = invalid ? "has-error" : undefined;
  return <div className="two-inputs">
    <input type="date" aria-label="Tarih" className={cls} value={local.date} disabled={disabled} onChange={e => update({ date: e.target.value })} />
    <input type="time" aria-label="Saat" className={cls} value={local.time} disabled={disabled} onChange={e => update({ time: e.target.value })} />
  </div>;
}
