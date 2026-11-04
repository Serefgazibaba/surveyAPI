import Badge from "./Badge.jsx";
import { statusMeta } from "../utils/statusLabels.js";

/**
 * Backend enum değerini Türkçe etiket + renkle gösterir.
 * kind: "survey" | "email" | "invitation"
 */
export default function StatusBadge({ kind = "survey", value }) {
  const meta = statusMeta(kind, value);
  return <Badge tone={meta.tone}>{meta.label}</Badge>;
}
