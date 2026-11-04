/** Etiketli onay kutusu. onChange(boolean) */
export default function Checkbox({ label, checked, onChange, ...props }) {
  const box = <input type="checkbox" checked={!!checked} onChange={e => onChange?.(e.target.checked)} {...props} />;
  if (!label) return box;
  return <label className="check">{box}{label}</label>;
}
