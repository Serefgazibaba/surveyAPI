import { Fragment } from "react";
import { Link } from "react-router-dom";
import Icon from "./Icon.jsx";

/** items: [{ label, to? }] — son öğe link değildir. */
export default function Breadcrumb({ items }) {
  return <nav className="breadcrumb" aria-label="Konum">
    {items.map((item, i) => <Fragment key={i}>
      {i > 0 && <Icon name="right" />}
      {item.to && i < items.length - 1 ? <Link to={item.to}>{item.label}</Link> : <span>{item.label}</span>}
    </Fragment>)}
  </nav>;
}
