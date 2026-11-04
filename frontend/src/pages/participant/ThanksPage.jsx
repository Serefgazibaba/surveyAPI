import { useLocation } from "react-router-dom";
import PublicLayout from "../../components/PublicLayout.jsx";
import StateCard from "../../components/StateCard.jsx";

/** S-09 · Teşekkür */
export default function ThanksPage() {
  const { state } = useLocation();
  return <PublicLayout>
    {state?.title && <h1>{state.title}</h1>}
    <StateCard tone="success" title="Cevaplarınız başarıyla gönderildi.">
      {state?.message || "Zaman ayırdığınız için teşekkür ederiz."} Bu sayfayı kapatabilirsiniz.
    </StateCard>
  </PublicLayout>;
}
