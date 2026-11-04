import { useEffect, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import PublicLayout from "../../components/PublicLayout.jsx";
import Spinner from "../../components/Spinner.jsx";
import ErrorState from "../../components/ErrorState.jsx";
import { participantLogin } from "../../api/participant.js";
import { tokenStore } from "../../auth/tokenStore.js";
import { isParticipantAccessError } from "../../utils/errorMessages.js";
import useAsync from "../../hooks/useAsync.js";

/**
 * /s/:token · E-postadaki davet bağlantısı.
 * Token'ı katılımcı JWT'sine çevirir, URL'den siler (replace) ve forma yönlendirir.
 */
export default function InviteLandingPage() {
  const { token } = useParams();
  const navigate = useNavigate();
  // Aynı promise paylaşılır: StrictMode'daki çift effect çalışması ikinci bir login isteği atmaz.
  const pending = useRef(null);
  const login = useAsync(() => (pending.current ??= participantLogin(token)), [token]);

  useEffect(() => {
    if (login.data) {
      const session = tokenStore.setParticipant(login.data);
      navigate(`/f/${session.surveyId}`, { replace: true });
    } else if (login.error && isParticipantAccessError(login.error)) {
      tokenStore.clearParticipant();
      const { status, code, surveyTitle } = login.error;
      navigate("/access-error", { replace: true, state: { status, code, surveyTitle } });
    }
  }, [login.data, login.error, navigate]);

  return <PublicLayout>
    {login.error && !isParticipantAccessError(login.error)
      ? <ErrorState error={login.error} onRetry={() => { pending.current = null; login.reload(); }} />
      : <Spinner size="lg" label="Anketiniz açılıyor..." />}
  </PublicLayout>;
}
