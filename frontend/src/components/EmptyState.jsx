/** Liste boş olduğunda gösterilen mesaj. */
export default function EmptyState({ title = "Kayıt bulunamadı", text, action }) {
  return <div className="empty"><b>{title}</b>{text && <span>{text}</span>}{action && <div className="mt">{action}</div>}</div>;
}
