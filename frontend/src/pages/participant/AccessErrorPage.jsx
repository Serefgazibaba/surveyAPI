import { useLocation } from "react-router-dom";
import PublicLayout from "../../components/PublicLayout.jsx";
import StateCard from "../../components/StateCard.jsx";
import { describeError } from "../../utils/errorMessages.js";

/** S-10 · Erişim hatası (geçersiz davet, süresi dolmuş, kapalı veya zaten yanıtlanmış anket). */
export default function AccessErrorPage() {
  const { state } = useLocation();
  const error = state?.code ? state : { status: 401, code: "NO_PARTICIPANT_SESSION" };
  const info = describeError(error);
  const tone = error.code === "ALREADY_RESPONDED" ? "info" : "warning";
  return <PublicLayout>
    {error.surveyTitle && <h1>{error.surveyTitle}</h1>}
    <StateCard tone={tone} title={info.title}>{info.hint}</StateCard>
  </PublicLayout>;
}
