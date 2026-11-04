import { api } from "./client.js";

/** GET /admin/surveys?q&status[]&sort&page&size → { items, page } */
export const listSurveys = ({ q, status, sort, page, size } = {}) =>
  api.get("/admin/surveys", { query: { q, status, sort, page, size } });

/** GET /admin/surveys/{id} → SurveyDetailResponse */
export const getSurvey = id => api.get(`/admin/surveys/${id}`);

/** POST /admin/upsertSurvey (id yoksa oluşturur) → SurveyDetailResponse */
export const upsertSurvey = payload => api.post("/admin/upsertSurvey", payload);

/** DELETE /admin/surveys/{id} → 204 (yalnızca DRAFT) */
export const deleteSurvey = id => api.del(`/admin/surveys/${id}`);

/** POST /admin/publishSurvey { surveyId, userIds } → PublishSurveyResponse */
export const publishSurvey = (surveyId, userIds) => api.post("/admin/publishSurvey", { surveyId, userIds });
