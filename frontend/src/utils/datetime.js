// Backend tüm zamanları UTC ISO-8601 olarak verir/bekler; ekranda Europe/Istanbul gösterilir.
export const TIME_ZONE = "Europe/Istanbul";

const partsFormatter = new Intl.DateTimeFormat("en-CA", {
  timeZone: TIME_ZONE, year: "numeric", month: "2-digit", day: "2-digit",
  hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23",
});

function zonedParts(date) {
  const p = Object.fromEntries(partsFormatter.formatToParts(date).map(x => [x.type, x.value]));
  return { year: +p.year, month: +p.month, day: +p.day, hour: +p.hour, minute: +p.minute, second: +p.second };
}

/** UTC ISO → { date: "YYYY-MM-DD", time: "HH:mm" } (İstanbul saati). */
export function toZonedInput(iso) {
  if (!iso) return { date: "", time: "" };
  const p = zonedParts(new Date(iso));
  const pad = n => String(n).padStart(2, "0");
  return { date: `${p.year}-${pad(p.month)}-${pad(p.day)}`, time: `${pad(p.hour)}:${pad(p.minute)}` };
}

/** İstanbul tarih + saat girişi → UTC ISO. Eksik girişte null. */
export function fromZonedInput(date, time) {
  if (!date) return null;
  const [y, m, d] = date.split("-").map(Number);
  const [h, mi] = (time || "23:59").split(":").map(Number);
  const guess = Date.UTC(y, m - 1, d, h, mi);
  const p = zonedParts(new Date(guess));
  const offset = Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute, p.second) - guess;
  return new Date(guess - offset).toISOString();
}

const dateFmt = new Intl.DateTimeFormat("tr-TR", { timeZone: TIME_ZONE, day: "2-digit", month: "2-digit", year: "numeric" });
const dateTimeFmt = new Intl.DateTimeFormat("tr-TR", { timeZone: TIME_ZONE, day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
const timeFmt = new Intl.DateTimeFormat("tr-TR", { timeZone: TIME_ZONE, hour: "2-digit", minute: "2-digit" });

/** Saniye → "3 dk 12 sn"; değer yoksa "—". */
export function formatDuration(seconds) {
  if (seconds === null || seconds === undefined) return "—";
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return m ? `${m} dk ${s} sn` : `${s} sn`;
}

export const formatDate = iso => (iso ? dateFmt.format(new Date(iso)) : "—");
export const formatDateTime = iso => (iso ? dateTimeFmt.format(new Date(iso)) : "—");
export const formatTime = iso => (iso ? timeFmt.format(new Date(iso)) : "");
