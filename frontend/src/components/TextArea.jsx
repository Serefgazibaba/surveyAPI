/** Çok satırlı metin girişi. `maxLength` verilirse karakter sayacı gösterilir (showCount). */
export default function TextArea({ value = "", maxLength, showCount = false, ...props }) {
  return <>
    <textarea className="textarea" value={value} maxLength={maxLength} {...props} />
    {showCount && maxLength && <div className="char-count">{value.length} / {maxLength}</div>}
  </>;
}
