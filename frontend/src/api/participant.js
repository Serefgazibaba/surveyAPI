import { api } from "./client.js";

const opts = { auth: "user" };

/** POST /user/auth/login { invitationToken } → UserLoginResponse */
export const participantLogin = invitationToken => api.post("/user/auth/login", { invitationToken }, { auth: false });

/** GET /user/surveys/{id} → UserSurveyResponse (draft dahil) */
export const getParticipantSurvey = id => api.get(`/user/surveys/${id}`, opts);

/** PUT /user/surveys/{id}/draft { currentQuestionId, answers } → { savedAt, answeredCount, totalQuestionCount } */
export const saveDraft = (id, draft) => api.put(`/user/surveys/${id}/draft`, draft, opts);

/** POST /user/surveys/{id}/submit { answers } → { submittedAt, message } */
export const submitAnswers = (id, answers) => api.post(`/user/surveys/${id}/submit`, { answers }, opts);
