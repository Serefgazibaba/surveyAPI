import Checkbox from "./Checkbox.jsx";
import EmptyState from "./EmptyState.jsx";
import ErrorState from "./ErrorState.jsx";
import Skeleton from "./Skeleton.jsx";

/**
 * Genel tablo.
 * columns: [{ key, header, render?(row), className?, sortKey? }]
 * sort: { value: "-updatedAt", onChange(nextSort) }  → sortKey'li başlıklar tıklanabilir olur
 * selection: { selected: Set<id>, onChange(nextSet), isSelectable?(row) }
 * loading / error (ApiError) / onRetry / empty: { title, text, action }
 */
export default function DataTable({ columns, rows = [], rowKey = r => r.id, loading, error, onRetry, empty, sort, selection, rowClassName }) {
  if (error) return <ErrorState error={error} onRetry={onRetry} />;

  const selectable = selection ? rows.filter(r => selection.isSelectable?.(r) ?? true) : [];
  const allSelected = selectable.length > 0 && selectable.every(r => selection.selected.has(rowKey(r)));
  const toggleAll = () => {
    const next = new Set(selection.selected);
    selectable.forEach(r => (allSelected ? next.delete(rowKey(r)) : next.add(rowKey(r))));
    selection.onChange(next);
  };
  const toggleOne = row => {
    const next = new Set(selection.selected);
    const id = rowKey(row);
    next.has(id) ? next.delete(id) : next.add(id);
    selection.onChange(next);
  };

  const header = col => {
    if (!col.sortKey || !sort) return col.header;
    const active = sort.value.replace(/^-/, "") === col.sortKey;
    const desc = sort.value.startsWith("-");
    const next = active && !desc ? `-${col.sortKey}` : col.sortKey;
    return <button type="button" className="sort-btn" onClick={() => sort.onChange(next)}>
      {col.header}<span className="arrow">{active ? (desc ? "▼" : "▲") : "↕"}</span>
    </button>;
  };

  const colCount = columns.length + (selection ? 1 : 0);
  return <div className="panel table-scroll">
    <table>
      <thead><tr>
        {selection && <th className="cell-check"><Checkbox aria-label="Tümünü seç" checked={allSelected} disabled={!selectable.length} onChange={toggleAll} /></th>}
        {columns.map(c => <th key={c.key} className={c.className}>{header(c)}</th>)}
      </tr></thead>
      <tbody>
        {loading && Array.from({ length: 4 }).map((_, i) => <tr key={`sk-${i}`}>{Array.from({ length: colCount }).map((__, j) => <td key={j}><Skeleton /></td>)}</tr>)}
        {!loading && rows.length === 0 && <tr><td colSpan={colCount}><EmptyState {...empty} /></td></tr>}
        {!loading && rows.map(row => {
          const id = rowKey(row);
          const isSel = selection?.selected.has(id);
          const canSel = selection?.isSelectable?.(row) ?? true;
          return <tr key={id} className={[isSel && "selected", rowClassName?.(row)].filter(Boolean).join(" ") || undefined}>
            {selection && <td className="cell-check"><Checkbox aria-label="Satırı seç" checked={isSel} disabled={!canSel} onChange={() => toggleOne(row)} /></td>}
            {columns.map(c => <td key={c.key} className={c.className}>{c.render ? c.render(row) : row[c.key] ?? "—"}</td>)}
          </tr>;
        })}
      </tbody>
    </table>
  </div>;
}
