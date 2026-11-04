import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext.jsx";

/** Admin oturumu yoksa giriş sayfasına yönlendirir; dönüşte kaldığı sayfaya gelir. */
export default function RequireAdmin() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  return <Outlet />;
}
