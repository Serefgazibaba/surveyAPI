import Button from "./Button.jsx";
import Icon from "./Icon.jsx";

const SIZES = [20, 50];

/**
 * Backend PageMeta ({ page, size, totalItems, totalPages }) ile sayfalama.
 * onChange({ page, size })
 */
export default function Pager({ meta, onChange, sizes = SIZES }) {
  if (!meta) return null;
  const { page, size, totalItems, totalPages } = meta;
  const last = Math.max(totalPages, 1);
  return <div className="pager">
    {totalItems} toplam satır <span className="grow" />
    Sayfa başına satır
    <select value={size} aria-label="Sayfa başına satır" onChange={e => onChange({ page: 1, size: Number(e.target.value) })}>
      {sizes.map(s => <option key={s} value={s}>{s}</option>)}
    </select>
    <span>Sayfa {page} / {last}</span>
    <Button size="sm" aria-label="Önceki sayfa" disabled={page <= 1} onClick={() => onChange({ page: page - 1, size })}><Icon name="left" /></Button>
    <Button size="sm" aria-label="Sonraki sayfa" disabled={page >= last} onClick={() => onChange({ page: page + 1, size })}><Icon name="right" /></Button>
  </div>;
}
