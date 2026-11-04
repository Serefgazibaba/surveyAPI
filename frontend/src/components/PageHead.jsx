import Breadcrumb from "./Breadcrumb.jsx";

/** Sayfa başlığı + sağda aksiyon butonları. `crumbs` verilirse üstte breadcrumb çizilir. */
export default function PageHead({ title, crumbs, children }) {
  return <>
    {crumbs && <Breadcrumb items={crumbs} />}
    <header className="page-head"><h1>{title}</h1><div className="head-actions">{children}</div></header>
  </>;
}
