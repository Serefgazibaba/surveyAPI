/** Alan bazlı kırmızı hata mesajı. Mesaj yoksa hiçbir şey çizmez. */
export default function FieldError({ message }) {
  if (!message) return null;
  return <small className="field-error" role="alert">{message}</small>;
}
