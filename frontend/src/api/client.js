import { ApiError, toApiError } from "./problem.js";
import { tokenStore } from "../auth/tokenStore.js";

const BASE_URL = (import.meta.env.VITE_API_BASE_URL || "/api/v1").replace(/\/$/, "");

let sessionExpiredHandler = () => {};
/** Admin oturumu yenilenemediğinde çağrılır (AuthContext login'e yönlendirir). */
export function onAdminSessionExpired(fn) {
  sessionExpiredHandler = fn;
}

function buildUrl(path, query) {
  const params = new URLSearchParams();
  Object.entries(query || {}).forEach(([key, value]) => {
    const values = Array.isArray(value) ? value : [value];
    values.forEach(v => { if (v !== undefined && v !== null && v !== "") params.append(key, v); });
  });
  const qs = params.toString();
  return `${BASE_URL}${path}${qs ? `?${qs}` : ""}`;
}

async function send(method, path, { body, query, token }) {
  const headers = { Accept: "application/json, application/problem+json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;
  try {
    return await fetch(buildUrl(path, query), { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  } catch {
    throw new ApiError({ network: true });
  }
}

async function parse(response) {
  if (!response.ok) throw await toApiError(response);
  if (response.status === 204) return null;
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

// Refresh token tek kullanımlık (rotation): aynı anda birden fazla refresh isteği oturumu iptal ettirir.
// Bu yüzden tüm 401'ler tek bir paylaşılan refresh promise'ini bekler.
let refreshing = null;

export function refreshAdminSession() {
  if (!refreshing) {
    const refreshToken = tokenStore.getAdmin()?.refreshToken;
    refreshing = (async () => {
      if (!refreshToken) throw new ApiError({ status: 401, code: "SESSION_EXPIRED" });
      const res = await send("POST", "/admin/auth/refresh", { body: { refreshToken } });
      const data = await parse(res);
      tokenStore.setAdmin(data);
      return data;
    })().finally(() => { refreshing = null; });
  }
  return refreshing;
}

/**
 * Tek API giriş noktası.
 * auth: "admin" (varsayılan) | "user" (katılımcı) | false (public)
 * Hata durumunda her zaman ApiError fırlatır.
 */
export async function request(method, path, { body, query, auth = "admin" } = {}) {
  const tokenFor = () => (auth === "admin" ? tokenStore.getAdmin()?.accessToken : auth === "user" ? tokenStore.getParticipant()?.accessToken : null);
  let response = await send(method, path, { body, query, token: tokenFor() });

  if (response.status === 401 && auth === "admin" && tokenStore.getAdmin()?.refreshToken) {
    try {
      await refreshAdminSession();
    } catch (e) {
      tokenStore.clearAdmin();
      sessionExpiredHandler();
      throw e instanceof ApiError && e.code !== "NETWORK_ERROR" ? new ApiError({ status: 401, code: "SESSION_EXPIRED" }) : e;
    }
    response = await send(method, path, { body, query, token: tokenFor() });
  }
  return parse(response);
}

export const api = {
  get: (path, opts) => request("GET", path, opts),
  post: (path, body, opts) => request("POST", path, { ...opts, body }),
  put: (path, body, opts) => request("PUT", path, { ...opts, body }),
  del: (path, opts) => request("DELETE", path, opts),
};
