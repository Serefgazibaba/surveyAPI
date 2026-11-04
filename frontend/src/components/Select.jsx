/** Açılır liste. options: [{ value, label }]; `placeholder` boş seçenek olarak eklenir. */
export default function Select({ options = [], placeholder, value, onChange, ...props }) {
  return <select className="select" value={value ?? ""} onChange={e => onChange?.(e.target.value)} {...props}>
    {placeholder !== undefined && <option value="">{placeholder}</option>}
    {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
  </select>;
}
