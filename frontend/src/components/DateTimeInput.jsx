import { useState } from "react";
import DateInput from "./DateInput.jsx";
import TimeInput from "./TimeInput.jsx";
import { toZonedInput, fromZonedInput, todayYmd } from "../utils/datetime.js";

/**
 * Tarih + saat girişi. Değer UTC ISO string'dir; kullanıcı İstanbul saatiyle girer.
 * Tarih GG.AA.YYYY (DateInput), saat 24 saat biçiminde (TimeInput). Tarayıcının yerel tarih/saat seçicileri işletim
 * sistemi diline göre biçim (mm/dd/yyyy, AM/PM) gösterdiği için kullanılmaz.
 * Saat otomatik doldurulmaz: tarih, saat ve dakika girilene kadar onChange(null) çağrılır;
 * yarım giriş bileşenin içinde korunur.
 * onChange(isoOrNull)
 */
export default function DateTimeInput({ value, onChange, disabled, invalid = false }) {
  const split = iso => {
    const { date, time } = toZonedInput(iso);
    const [hour = "", minute = ""] = time ? time.split(":") : [];
    return { date, hour, minute };
  };
  const [local, setLocal] = useState(() => split(value));
  const [lastValue, setLastValue] = useState(value);
  // Değer dışarıdan değiştiyse (ör. kayıttan dönen yanıt) yerel girişi ona göre güncelle.
  if (value !== lastValue) {
    setLastValue(value);
    if (value) setLocal(split(value));
  }

  const update = patch => {
    const next = { ...local, ...patch };
    setLocal(next);
    const iso = next.date && next.hour && next.minute ? fromZonedInput(next.date, `${next.hour}:${next.minute}`) : null;
    setLastValue(iso);
    onChange?.(iso);
  };

  return <div className="two-inputs">
    <DateInput value={local.date} min={todayYmd()} invalid={invalid} disabled={disabled} onChange={date => update({ date })} />
    <TimeInput hour={local.hour} minute={local.minute} invalid={invalid} disabled={disabled} onChange={update} />
  </div>;
}
