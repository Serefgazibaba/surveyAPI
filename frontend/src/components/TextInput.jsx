import Icon from "./Icon.jsx";

/** Çerçeveli metin girişi. `icon` solda ikon, `suffix` sağda ek içerik (örn. buton). */
export default function TextInput({ icon, suffix, className = "", disabled, ...props }) {
  return <div className={`input${disabled ? " disabled" : ""} ${className}`.trim()}>
    {icon && <Icon name={icon} />}
    <input disabled={disabled} {...props} />
    {suffix}
  </div>;
}
