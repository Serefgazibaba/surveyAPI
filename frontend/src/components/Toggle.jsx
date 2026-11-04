/** Aç/kapa anahtarı. onChange(boolean) */
export default function Toggle({ checked, onChange, label, disabled }) {
  return <button type="button" role="switch" aria-checked={!!checked} aria-label={label}
    className={`toggle${checked ? " on" : ""}`} disabled={disabled} onClick={() => onChange?.(!checked)} />;
}
