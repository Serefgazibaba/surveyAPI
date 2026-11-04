/**
 * Backend'de henüz karşılığı olmayan özellikleri pasif + "Yakında" rozetiyle gösterir.
 * İçine normal bileşen (Button, sekme, alan) konur; tıklanamaz hale gelir.
 */
export default function ComingSoon({ children, block = false, note = "Bu özellik yakında eklenecek" }) {
  return <span className={`coming-soon${block ? " block" : ""}`} title={note} aria-disabled="true">{children}</span>;
}
