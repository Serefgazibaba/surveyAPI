/**
 * Backend RFC 7807 (application/problem+json) hatasının istemci karşılığı.
 * Tüm API hataları bu sınıfa dönüştürülür; ekranlar yalnızca bunu görür.
 */
export class ApiError extends Error {
  constructor({ status = 0, code, title, detail, errors = [], surveyTitle, retryAfter, network = false }) {
    super(title || code || "API hatası");
    this.name = "ApiError";
    this.status = status;
    this.code = code || (network ? "NETWORK_ERROR" : `HTTP_${status}`);
    this.title = title;
    this.detail = detail;
    this.errors = Array.isArray(errors) ? errors : [];
    this.surveyTitle = surveyTitle;
    this.retryAfter = retryAfter;
    this.network = network;
  }
}

export async function toApiError(response) {
  let body = {};
  try {
    const text = await response.text();
    body = text ? JSON.parse(text) : {};
  } catch {
    body = {};
  }
  const retry = Number(response.headers.get("Retry-After"));
  return new ApiError({
    status: response.status,
    code: body.code,
    title: body.title,
    detail: body.detail,
    errors: body.errors,
    surveyTitle: body.surveyTitle,
    retryAfter: Number.isFinite(retry) && retry > 0 ? retry : undefined,
  });
}

export const isApiError = e => e instanceof ApiError;
