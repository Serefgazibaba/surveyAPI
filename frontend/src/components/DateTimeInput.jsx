import { toZonedInput, fromZonedInput } from "../utils/datetime.js";

/**
 * Tarih + saat girişi. Değer UTC ISO string'dir; kullanıcı İstanbul saatiyle girer.
 * onChange(isoOrNull)
 */
export default function DateTimeInput({ value, onChange, disabled }) {
  const { date, time } = toZonedInput(value);
  const emit = (d, t) => onChange?.(fromZonedInput(d, t || time || "18:00"));
  return <div className="two-inputs">
    <input type="date" aria-label="Tarih" value={date} disabled={disabled} onChange={e => emit(e.target.value, time)} />
    <input type="time" aria-label="Saat" value={time} disabled={disabled || !date} onChange={e => emit(date, e.target.value)} />
  </div>;
}
