import StateCard from "./StateCard.jsx";
import Button from "./Button.jsx";
import { describeError } from "../utils/errorMessages.js";

/**
 * Veri yüklenemediğinde tam panel hata. 404/403 için geri dönüş, diğerleri için "Tekrar dene".
 * backTo: { to, label } geri dönülecek sayfa.
 */
export default function ErrorState({ error, onRetry, backTo }) {
  const info = describeError(error);
  if (!info) return null;
  const retryable = onRetry && ![403, 404].includes(error.status);
  return <StateCard tone={info.tone} title={info.title} actions={<>
    {backTo && <Button to={backTo.to}>{backTo.label}</Button>}
    {retryable && <Button variant="primary" icon="refresh" onClick={onRetry}>Tekrar dene</Button>}
  </>}>{info.hint}</StateCard>;
}
