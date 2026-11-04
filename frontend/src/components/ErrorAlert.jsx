import Alert from "./Alert.jsx";
import Button from "./Button.jsx";
import { describeError } from "../utils/errorMessages.js";

/**
 * Backend hatasını (ApiError) okunur başlık + açıklama + madde listesi olarak gösterir.
 * onItemClick(item): madde tıklanınca ilgili alana gitmek için.
 * onRetry: "Tekrar dene" butonu. extra: ek içerik (örn. geri sayım). hideItems: maddeleri gizle.
 */
export default function ErrorAlert({ error, onRetry, onItemClick, extra, hideItems = false }) {
  const info = describeError(error);
  if (!info) return null;
  return <Alert tone={info.tone} title={info.title}
    actions={onRetry && <Button size="sm" icon="refresh" onClick={onRetry}>Tekrar dene</Button>}>
    {info.hint && <p>{info.hint}</p>}
    {!hideItems && info.items.length > 0 && <ul>
      {info.items.map((item, i) => <li key={`${item.field}-${i}`}>
        {item.label && <>{onItemClick
          ? <button type="button" onClick={() => onItemClick(item)}>{item.label}</button>
          : <b style={{ display: "inline" }}>{item.label}</b>}: </>}
        {item.message}
      </li>)}
    </ul>}
    {extra}
  </Alert>;
}
