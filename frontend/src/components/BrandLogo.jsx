import { useId } from "react";

const DARK = "#1D1D2B";
const BLUE = "#1E4FD8";

/** X işareti (32×32): birbirini kesen iki bant; sol yarısı koyu, sağ yarısı mavi. */
function Mark({ className, size }) {
  const id = useId().replace(/:/g, "");
  const bands = <>
    <polygon points="0,0 8,0 32,32 24,32" />
    <polygon points="0,32 8,32 32,0 24,0" />
  </>;
  return <svg className={className} width={size} height={size} viewBox="0 0 32 32" aria-hidden="true" focusable="false">
    <defs>
      <clipPath id={`${id}-l`}><rect x="0" y="0" width="16" height="32" /></clipPath>
      <clipPath id={`${id}-r`}><rect x="16" y="0" width="16" height="32" /></clipPath>
    </defs>
    <g clipPath={`url(#${id}-l)`} fill={DARK}>{bands}</g>
    <g clipPath={`url(#${id}-r)`} fill={BLUE}>{bands}</g>
  </svg>;
}

/**
 * AnketX logosu. variant: "full" (Anket + X, birleşik yazım) | "mark" (yalnızca X işareti).
 * size: "full"de yazı boyutu, "mark"ta işaretin kenarı (px). X, büyük harf yüksekliğinde ve yazının hemen
 * ardından durur; yazı tarayıcıda çizildiği için genişlikten bağımsız olarak hep bitişiktir.
 */
export default function BrandLogo({ variant = "full", size = 28, className = "" }) {
  if (variant === "mark") {
    return <span className={`brand-logo ${className}`.trim()} role="img" aria-label="AnketX"><Mark size={size} /></span>;
  }
  return <span className={`brand-logo ${className}`.trim()} role="img" aria-label="AnketX" style={{ fontSize: size }}>
    <span className="brand-logo-text" aria-hidden="true">Anket</span>
    <Mark className="brand-logo-mark" />
  </span>;
}
