/** Dönen yükleme göstergesi. `size="lg"` sayfa ortası için; `label` verilirse blok olarak metinle çizilir. */
export default function Spinner({ size, label }) {
  const spinner = <span className={`spinner${size === "lg" ? " lg" : ""}`} role="status" aria-label={label || "Yükleniyor"} />;
  if (!label) return spinner;
  return <div className="loading-block">{spinner}<span>{label}</span></div>;
}
