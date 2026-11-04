import Icon from "./Icon.jsx";
import Button from "./Button.jsx";
import { describeError } from "../utils/errorMessages.js";

/**
 * Satır içi hata kutusu. Backend hatasını (ApiError) okunur başlık + açıklama + madde listesine çevirir.
 * onItemClick(item): madde tıklanınca ilgili alana gitmek için.
 * onRetry: "Tekrar dene" butonu. extra: ek içerik (örn. geri sayım).
 */
export default function ErrorAlert({ error, onRetry, onItemClick, extra, hideItems = false }) {
  const info = describeError(error);
  if (!info) return null;
  return <div className={`alert ${info.tone}`} role="alert">
    <Icon name="alert" />
    <div className="alert-body">
      <b>{info.title}</b>
      {info.hint && <p>{info.hint}</p>}
      {!hideItems && info.items.length > 0 && <ul>
        {info.items.map((item, i) => <li key={`${item.field}-${i}`}>
          {item.label && <>{onItemClick ? <button type="button" onClick={() => onItemClick(item)}>{item.label}</button> : <b style={{ display: "inline" }}>{item.label}</b>}: </>}
          {item.message}
        </li>)}
      </ul>}
      {extra}
      {onRetry && <div className="alert-actions"><Button size="sm" icon="refresh" onClick={onRetry}>Tekrar dene</Button></div>}
    </div>
  </div>;
}
