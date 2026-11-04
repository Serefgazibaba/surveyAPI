// Token saklama. Admin: "Beni hatırla" → localStorage, aksi halde sessionStorage.
// Katılımcı: her zaman sessionStorage (README önerisi; davet linki tekrar açılarak yenilenir).

const ADMIN_KEY = "surveyTool.admin";
const PARTICIPANT_KEY = "surveyTool.participant";

function safe(fn, fallback = null) {
  try { return fn(); } catch { return fallback; }
}

function read(storage, key) {
  return safe(() => JSON.parse(storage.getItem(key) || "null"));
}

export const tokenStore = {
  getAdmin() {
    return read(localStorage, ADMIN_KEY) || read(sessionStorage, ADMIN_KEY);
  },
  setAdmin(tokenResponse, remember) {
    const keep = remember ?? this.getAdmin()?.remember ?? false;
    const session = {
      accessToken: tokenResponse.accessToken,
      refreshToken: tokenResponse.refreshToken,
      user: tokenResponse.user ?? this.getAdmin()?.user,
      remember: keep,
    };
    this.clearAdmin();
    safe(() => (keep ? localStorage : sessionStorage).setItem(ADMIN_KEY, JSON.stringify(session)));
    return session;
  },
  clearAdmin() {
    safe(() => localStorage.removeItem(ADMIN_KEY));
    safe(() => sessionStorage.removeItem(ADMIN_KEY));
  },

  getParticipant() {
    return read(sessionStorage, PARTICIPANT_KEY);
  },
  setParticipant(loginResponse) {
    const session = {
      accessToken: loginResponse.accessToken,
      surveyId: loginResponse.surveyId,
      surveyTitle: loginResponse.surveyTitle,
      expiresAt: loginResponse.expiresAt,
    };
    safe(() => sessionStorage.setItem(PARTICIPANT_KEY, JSON.stringify(session)));
    return session;
  },
  clearParticipant() {
    safe(() => sessionStorage.removeItem(PARTICIPANT_KEY));
  },
};
