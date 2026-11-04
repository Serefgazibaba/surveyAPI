import { useEffect, useRef, useState } from "react";
import TextInput from "./TextInput.jsx";

/**
 * Debounce'lu arama kutusu. Kullanıcı yazmayı bıraktıktan `delay` ms sonra onSearch(value) çağrılır.
 * `value` dışarıdan değişirse (örn. URL) kutu senkronlanır.
 */
export default function SearchInput({ value = "", onSearch, delay = 300, placeholder = "Ara...", maxLength = 100 }) {
  const [text, setText] = useState(value);
  const first = useRef(true);
  useEffect(() => { setText(value); }, [value]);
  useEffect(() => {
    if (first.current) { first.current = false; return; }
    if (text === value) return;
    const t = setTimeout(() => onSearch?.(text.trim()), delay);
    return () => clearTimeout(t);
  }, [text]); // eslint-disable-line react-hooks/exhaustive-deps
  return <TextInput className="search" icon="search" placeholder={placeholder} maxLength={maxLength}
    value={text} onChange={e => setText(e.target.value)} aria-label={placeholder} />;
}
