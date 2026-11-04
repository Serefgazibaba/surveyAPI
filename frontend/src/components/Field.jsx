import FieldError from "./FieldError.jsx";

/**
 * Form alanı sarmalayıcısı: etiket + kontrol + yardım metni + alan hatası.
 * `error` backend'den gelen (mapFieldErrors ile eşlenmiş) mesajdır.
 * `id` verilirse hata listesinden bu alana odaklanılabilir.
 */
export default function Field({ label, hint, error, children, id, as = "label" }) {
  const Tag = as;
  return <Tag className={`field${error ? " has-error" : ""}`} id={id ? `field-${id}` : undefined}>
    {label && <span>{label}</span>}
    {children}
    {hint && !error && <small className="hint">{hint}</small>}
    <FieldError message={error} />
  </Tag>;
}
