import { Navigate, Route, Routes, useParams } from "react-router-dom";
import ErrorBoundary from "./components/ErrorBoundary.jsx";
import { ToastProvider } from "./components/Toast.jsx";
import { AuthProvider } from "./auth/AuthContext.jsx";
import RequireAdmin from "./auth/RequireAdmin.jsx";
import LoginPage from "./pages/admin/LoginPage.jsx";
import SurveyListPage from "./pages/admin/SurveyListPage.jsx";
import SurveyBuilderPage from "./pages/admin/SurveyBuilderPage.jsx";
import ParticipantsPage from "./pages/admin/ParticipantsPage.jsx";
import PublishResultPage from "./pages/admin/PublishResultPage.jsx";
import MailsPage from "./pages/admin/MailsPage.jsx";
import TemplateListPage from "./pages/admin/TemplateListPage.jsx";
import TemplateBuilderPage from "./pages/admin/TemplateBuilderPage.jsx";
import InviteLandingPage from "./pages/participant/InviteLandingPage.jsx";
import SurveyFormPage from "./pages/participant/SurveyFormPage.jsx";
import ThanksPage from "./pages/participant/ThanksPage.jsx";
import AccessErrorPage from "./pages/participant/AccessErrorPage.jsx";

/** Yeni ↔ düzenle geçişinde builder state'i sıfırlansın diye anket id'si key olarak kullanılır. */
function BuilderRoute() {
  const { id } = useParams();
  return <SurveyBuilderPage key={id || "new"} />;
}

function TemplateBuilderRoute() {
  const { id } = useParams();
  return <TemplateBuilderPage key={id || "new"} />;
}

export default function App() {
  return <div className="survey-tool">
    <ErrorBoundary>
      <ToastProvider>
        <AuthProvider>
          <Routes>
            {/* Admin */}
            <Route path="/login" element={<LoginPage />} />
            <Route element={<RequireAdmin />}>
              <Route path="/surveys" element={<SurveyListPage />} />
              <Route path="/surveys/new" element={<BuilderRoute />} />
              <Route path="/surveys/:id/edit" element={<BuilderRoute />} />
              <Route path="/surveys/:id/participants" element={<ParticipantsPage />} />
              <Route path="/surveys/:id/published" element={<PublishResultPage />} />
              <Route path="/mails" element={<MailsPage />} />
              <Route path="/templates" element={<TemplateListPage />} />
              <Route path="/templates/new" element={<TemplateBuilderRoute />} />
              <Route path="/templates/:id/edit" element={<TemplateBuilderRoute />} />
            </Route>

            {/* Katılımcı (davet linki: {FRONTEND_BASE_URL}/s/{token}) */}
            <Route path="/s/:token" element={<InviteLandingPage />} />
            <Route path="/f/:surveyId" element={<SurveyFormPage />} />
            <Route path="/f/:surveyId/thanks" element={<ThanksPage />} />
            <Route path="/access-error" element={<AccessErrorPage />} />

            <Route path="*" element={<Navigate to="/surveys" replace />} />
          </Routes>
        </AuthProvider>
      </ToastProvider>
    </ErrorBoundary>
  </div>;
}
