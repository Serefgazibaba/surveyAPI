/** Renkli etiket. tone: "blue" | "gray" | "green" | "amber" | "red" */
export default function Badge({ children, tone = "blue" }) {
  return <span className={`badge ${tone}`}>{children}</span>;
}
