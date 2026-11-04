// Backend hata kodu → kullanıcının net anlayacağı Türkçe başlık + ne yapması gerektiği.
// Sayfalar hata metni üretmez; tüm hatalar describeError() üzerinden ErrorAlert/ErrorState/Toast'a gider.

const CODE_MESSAGES = {
  // Genel
  VALIDATION_ERROR: { title: "Bazı alanlar hatalı", hint: "İşaretli alanları düzeltip tekrar deneyin." },
  BAD_REQUEST: { title: "İstek anlaşılamadı", hint: "Sayfayı yenileyip tekrar deneyin." },
  UNAUTHORIZED: { title: "Oturum gerekli", hint: "Lütfen tekrar giriş yapın." },
  FORBIDDEN: { title: "Bu işlem için yetkiniz yok", hint: "Doğru hesapla giriş yaptığınızdan emin olun." },
  NOT_FOUND: { title: "Kayıt bulunamadı", hint: "Kayıt silinmiş veya taşınmış olabilir." },
  METHOD_NOT_ALLOWED: { title: "Bu işlem desteklenmiyor" },
  UNSUPPORTED_MEDIA_TYPE: { title: "Bu işlem desteklenmiyor" },
  NOT_ACCEPTABLE: { title: "Bu işlem desteklenmiyor" },
  INTERNAL_ERROR: { title: "Sunucuda beklenmeyen bir hata oluştu", hint: "Biraz sonra tekrar deneyin. Sorun sürerse sistem yöneticisine bildirin." },
  NETWORK_ERROR: { title: "Sunucuya ulaşılamadı", hint: "İnternet bağlantınızı kontrol edip tekrar deneyin." },

  // Admin giriş
  INVALID_CREDENTIALS: { title: "E-posta veya şifre hatalı", hint: "Bilgilerinizi kontrol edip tekrar deneyin." },
  ACCOUNT_LOCKED: { title: "Çok fazla hatalı deneme", hint: "Güvenliğiniz için giriş geçici olarak kilitlendi." },
  SESSION_EXPIRED: { title: "Oturumunuz sona erdi", hint: "Devam etmek için tekrar giriş yapın." },

  // Anket
  SURVEY_NOT_FOUND: { title: "Anket bulunamadı", hint: "Anket silinmiş olabilir ya da size ait değil." },
  REPORT_NOT_AVAILABLE: { title: "Bu anketin raporu yok", hint: "Rapor, anket yayınlandıktan sonra oluşur." },
  TEMPLATE_NOT_FOUND: { title: "Şablon bulunamadı", hint: "Şablon silinmiş olabilir ya da size ait değil." },
  SURVEY_STRUCTURE_LOCKED: { title: "Yayındaki ankette yalnızca metinler değiştirilebilir", hint: "Soru ekleme/silme, sıralama, tip, zorunluluk, bitiş tarihi ve yanıt modu yayından sonra değiştirilemez." },
  SURVEY_NOT_EDITABLE: { title: "Bu anket artık düzenlenemez", hint: "Süresi dolmuş, kapanmış veya arşivlenmiş anketler değiştirilemez." },
  SURVEY_NOT_DELETABLE: { title: "Bu anket silinemez", hint: "Yalnızca taslak durumundaki anketler silinebilir." },
  SURVEY_HAS_RESPONSES: { title: "Yanıtı olan anket silinemez" },
  INVALID_STRUCTURE: { title: "Anket yapısı geçersiz", hint: "Sayfayı yenileyip değişikliklerinizi tekrar uygulayın." },
  INVALID_SURVEY_STATUS: { title: "Anket bu durumda yayınlanamaz", hint: "Yalnızca taslak anketler yayınlanabilir." },
  SURVEY_INCOMPLETE: { title: "Anket yayına hazır değil", hint: "Aşağıdaki eksikleri tamamlayıp tekrar gönderin." },
  INVALID_PARTICIPANTS: { title: "Bazı katılımcılar davet edilemiyor", hint: "İşaretli kişileri listeden çıkarıp tekrar deneyin." },

  // Mail
  MAILS_NOT_RESENDABLE: { title: "Seçilen maillerin bir kısmı yeniden gönderilemez", hint: "Hiçbir mail gönderilmedi. İşaretli satırları seçimden çıkarıp tekrar deneyin." },

  // Katılımcı
  INVALID_INVITATION: { title: "Davet bağlantısı geçersiz", hint: "Bağlantı hatalı, iptal edilmiş veya yenisiyle değiştirilmiş olabilir. Size gelen en son e-postadaki bağlantıyı kullanın." },
  ALREADY_RESPONDED: { title: "Bu anketi zaten yanıtladınız", hint: "Her davet yalnızca bir kez yanıtlanabilir. Katılımınız için teşekkür ederiz." },
  SURVEY_EXPIRED: { title: "Bu anketin süresi dolmuştur", hint: "Anket yanıtlara kapanmıştır. Bir sorun olduğunu düşünüyorsanız anket sahibiyle iletişime geçin." },
  SURVEY_CLOSED: { title: "Bu anket kapatılmıştır", hint: "Anket artık yanıt kabul etmiyor." },
  NO_PARTICIPANT_SESSION: { title: "Anket oturumu bulunamadı", hint: "Size e-postayla gönderilen davet bağlantısını yeniden açın." },
  INVALID_ANSWERS: { title: "Bazı cevaplar geçersiz", hint: "İşaretli soruları kontrol edin." },
  REQUIRED_ANSWERS_MISSING: { title: "Zorunlu sorular yanıtlanmadı", hint: "İşaretli soruları yanıtlayıp tekrar gönderin." },
};

// errors[].code için yedek mesajlar (backend zaten Türkçe `message` döner; o yoksa kullanılır).
const FIELD_CODE_MESSAGES = {
  NotBlank: "Bu alan boş bırakılamaz",
  NotNull: "Bu alan zorunludur",
  NotEmpty: "En az bir değer seçilmelidir",
  Size: "Uzunluk sınırı aşıldı",
  Email: "Geçerli bir e-posta adresi girin",
  Min: "Değer çok küçük",
  Max: "Değer çok büyük",
  REQUIRED: "Bu soru zorunludur",
  END_DATE_REQUIRED: "Bitiş tarihi girilmelidir",
  END_DATE_IN_PAST: "Bitiş tarihi gelecekte olmalıdır",
  RESPONSE_MODE_REQUIRED: "Yanıt modu seçilmelidir",
  SURVEY_HAS_NO_QUESTIONS: "Ankete en az bir soru eklenmelidir",
  TEXT_REQUIRED: "Soru metni girilmelidir",
  TYPE_REQUIRED: "Soru tipi seçilmelidir",
  MIN_TWO_OPTIONS: "En az iki seçenek girilmelidir",
  LABEL_REQUIRED: "Seçenek metni girilmelidir",
  MAX_SELECT_EXCEEDS_OPTIONS: "En fazla seçim sayısı seçenek sayısını aşamaz",
  NOT_INVITABLE: "Bu kişi davet edilemez",
  NOT_FAILED: "Yalnızca başarısız mailler yeniden gönderilebilir",
  INVITATION_CLOSED: "Davet kapandığı için yeniden gönderilemez",
  TOO_MANY_SELECTIONS: "Çok fazla seçim yapıldı",
  TOO_FEW_SELECTIONS: "Daha fazla seçim yapılmalı",
  TOO_LONG: "Metin çok uzun",
  OUT_OF_RANGE: "Değer izin verilen aralıkta değil",
};

const STATUS_DEFAULTS = {
  400: CODE_MESSAGES.BAD_REQUEST,
  401: CODE_MESSAGES.UNAUTHORIZED,
  403: CODE_MESSAGES.FORBIDDEN,
  404: CODE_MESSAGES.NOT_FOUND,
  409: { title: "İşlem şu anda yapılamıyor", hint: "Kayıt başka bir işlemle değişmiş olabilir. Sayfayı yenileyip tekrar deneyin." },
  410: { title: "Bu kayda artık erişilemiyor" },
  422: { title: "İşlem tamamlanamadı", hint: "Aşağıdaki maddeleri düzeltip tekrar deneyin." },
  423: CODE_MESSAGES.ACCOUNT_LOCKED,
};

const FIELD_LABELS = {
  title: "Anket başlığı",
  name: "Şablon adı",
  status: "Durum",
  description: "Açıklama",
  endsAt: "Bitiş tarihi",
  responseMode: "Yanıt modu",
  questions: "Sorular",
  email: "E-posta",
  password: "Şifre",
  userIds: "Katılımcılar",
  emailIds: "Mailler",
  sort: "Sıralama",
};

const QUESTION_PARTS = {
  text: "metin", type: "tip", required: "zorunluluk", settings: "ayarlar", options: "seçenekler",
  "settings.scale": "ölçek", "settings.minLabel": "en düşük etiket", "settings.maxLabel": "en yüksek etiket",
  "settings.minSelect": "en az seçim", "settings.maxSelect": "en fazla seçim", "settings.maxLength": "maks. uzunluk",
  "settings.allowOther": "\"Diğer\" seçeneği", "settings.display": "görünüm", "settings.multiline": "çok satırlı",
};

/** `questions[2].options[0].label` gibi bir alan yolunu okunabilir etikete çevirir. */
export function fieldLabel(path) {
  if (!path) return "";
  const q = path.match(/^questions\[(\d+)\](?:\.(.+))?$/);
  if (q) {
    const n = Number(q[1]) + 1;
    const rest = q[2];
    if (!rest) return `Soru ${n}`;
    const opt = rest.match(/^options\[(\d+)\]/);
    if (opt) return `Soru ${n} · Seçenek ${Number(opt[1]) + 1}`;
    return `Soru ${n} · ${QUESTION_PARTS[rest] || rest}`;
  }
  const base = path.replace(/^[a-zA-Z]+\./, ""); // getAllUsers.size → size
  return FIELD_LABELS[path] || FIELD_LABELS[base.replace(/\[\d+\].*$/, "")] || "";
}

/** Tek bir errors[] maddesinin mesajı (backend mesajı öncelikli). */
export function fieldMessage(item) {
  return item?.message || FIELD_CODE_MESSAGES[item?.code] || "Geçersiz değer";
}

/**
 * Herhangi bir hatayı ekranda gösterilecek forma çevirir.
 * → { title, hint, items: [{ field, label, message }], tone }
 */
export function describeError(error) {
  if (!error) return null;
  const known = CODE_MESSAGES[error.code];
  const byStatus = STATUS_DEFAULTS[error.status];
  const fallback = error.status >= 500 ? CODE_MESSAGES.INTERNAL_ERROR : error.network ? CODE_MESSAGES.NETWORK_ERROR : null;
  const title = known?.title || error.title || byStatus?.title || fallback?.title || "Bir hata oluştu";
  let hint = known?.hint || byStatus?.hint || fallback?.hint || (error.title && error.title !== title ? error.title : "");
  if (error.code === "ACCOUNT_LOCKED" && error.retryAfter) {
    hint = `Güvenliğiniz için giriş geçici olarak kilitlendi. Yaklaşık ${Math.ceil(error.retryAfter / 60)} dakika sonra tekrar deneyin.`;
  }
  const items = (error.errors || []).map(e => ({ field: e.field, label: fieldLabel(e.field), message: fieldMessage(e), code: e.code }));
  const tone = [409, 410, 423].includes(error.status) ? "warning" : "error";
  return { title, hint, items, tone };
}

/** errors[] → { [alanYolu]: mesaj }. Field bileşeninin `error` prop'una verilir. */
export function mapFieldErrors(error) {
  const map = {};
  (error?.errors || []).forEach(e => {
    if (e.field && !map[e.field]) map[e.field] = fieldMessage(e);
  });
  return map;
}

/** Alan yolundan soru indeksini çıkarır (`questions[3].text` → 3, `answers[1].x` → 1). */
export function indexFromPath(path, prefix = "questions") {
  const m = path?.match(new RegExp(`^${prefix}\\[(\\d+)\\]`));
  return m ? Number(m[1]) : null;
}

/** Katılımcının ankete erişimini sonlandıran hata mı? (S-10 erişim hatası sayfasına yönlendirilir) */
export const isParticipantAccessError = error => [401, 403, 409, 410].includes(error?.status);
