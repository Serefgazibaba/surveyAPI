# Survey Tool — Veri Modeli ve API Listesi (Taslak v0.1)

Kaynak: *Survey_Tool_Analiz_v1.0* (BRD/SRS). Bu doküman teknolojiden bağımsızdır; kolon tipleri genel tiplerle
(`uuid`, `string`, `text`, `int`, `bool`, `timestamp`, `json`) yazılmıştır. Fiziksel tipler teknoloji seçiminden sonra netleşecek.

Kapsam etiketleri: **MVP** = Faz 1, **F2** = Faz 2, **F3** = Faz 3.

---

## 1. Tablolar

### 1.1 Özet

| #   | Tablo                     | Amaç                                                                 | Kapsam | İlgili gereksinim         |
|-----|---------------------------|----------------------------------------------------------------------|--------|---------------------------|
| T01 | `users`                   | Admin ve katılımcı (USER) hesapları                                  | MVP    | FR-01, Q-01               |
| T02 | `auth_sessions`           | Oturum / refresh token kayıtları, hareketsizlik takibi               | MVP    | FR-02                     |
| T02b| `login_attempts`          | (e-posta, IP) bazlı hatalı giriş sayacı ve kilit                     | MVP    | FR-01                     |
| T03 | `password_reset_tokens`   | Süreli şifre sıfırlama linkleri                                      | MVP    | FR-03                     |
| T04 | `surveys`                 | Anket ana kaydı ve yaşam döngüsü durumu                              | MVP    | FR-10–12, FR-20, FR-50–52 |
| T06 | `questions`               | Sorular; tipe özel ayarlar JSON                                      | MVP    | FR-22–25, 4.4             |
| T07 | `question_options`        | Seçimli soruların seçenekleri                                        | MVP    | FR-22, 4.4                |
| T08 | `survey_shares`           | Anketin başka Admin’lerle paylaşımı / ortak sahiplik                 | F2     | BR-09, Q-07               |
| T09 | `templates`               | Yeniden kullanılabilir anket iskeleti                                | MVP    | FR-30–32                  |
| T10 | `participants`            | Katılımcı kişi kaydı (e-posta, ad)                                   | MVP    | FR-40, NFR-06             |
| T11 | `participant_imports`     | CSV içe aktarım işi ve satır bazlı hata özeti                        | MVP    | FR-41, AS-05              |
| T12 | `invitations`             | Katılımcı ↔ anket erişim kaydı, token hash, davet durumu             | MVP    | FR-42–45, 3.3             |
| T13 | `response_drafts`         | Gönderim öncesi ara kayıt (davete bağlı)                             | MVP    | FR-63, AS-02, BR-06       |
| T14 | `responses`               | Kesinleşmiş (gönderilmiş) yanıt                                      | MVP    | BR-01, BR-06              |
| T15 | `answers`                 | Soru bazlı yanıt değeri                                              | MVP    | FR-71–73                  |
| T16 | `answer_options`          | Seçimli sorularda seçilen seçenekler                                 | MVP    | FR-71                     |
| T17 | `email_templates`         | Davet, hatırlatma vb. konu/gövde şablonları                          | MVP    | FR-46, Bölüm 9            |
| T18 | `dispatches`              | Bir “Gönder / Hatırlat” toplu işlemi ve özeti                        | MVP    | FR-42–44, S-06            |
| T19 | `email_messages`          | E-posta kuyruğu (outbox), deneme ve hata takibi                      | MVP    | NFR-10, NFR-21, NFR-42    |
| T20 | `export_jobs`             | CSV/PDF dışa aktarım işleri (asenkron)                               | MVP    | FR-73, NFR-13             |
| T21 | `audit_logs`              | Admin işlemleri denetim kaydı                                        | MVP    | NFR-07                    |
| T22 | `system_settings`         | Mail sağlayıcı, anonimlik eşiği, saklama süresi vb.                  | MVP    | NFR-21, BR-07, Q-05       |

### 1.2 Tablo detayları

#### T01 `users`
| Kolon | Tip | Not |
|---|---|---|
| id | uuid PK | |
| full_name | string | |
| email | string UNIQUE | küçük harfe normalize |
| password_hash | string | BCrypt (NFR-04); salt hash metninin içindedir |
| sso_provider / sso_subject | string NULL | F3 / Q-01 kararına bağlı (henüz uygulanmadı) |
| role | enum | `ADMIN`, `USER` |
| status | enum | `ACTIVE`, `DISABLED` |
| created_at / updated_at | timestamp | |

Yalnızca kimlik ve hesap bilgisi tutulur. Kilit bilgisi T02b'de tutulur; son giriş zamanı T02'deki son `created_at`'ten hesaplanır.

#### T02 `auth_sessions`
| Kolon | Tip | Not |
|---|---|---|
| id | uuid PK | JWT'de `sid` claim'i |
| user_id | uuid FK → users | |
| ip_address / user_agent | string | |
| refresh_token_hash | string UNIQUE | geçerli refresh token'ın SHA-256'sı; ham token saklanmaz |
| previous_refresh_token_hash | string NULL | bir önceki token; tekrar gelirse oturum `TOKEN_REUSE` ile iptal edilir |
| last_activity_at | timestamp | login ve her refresh'te güncellenir; 30 dk aşılırsa oturum düşer (FR-02) |
| expires_at | timestamp | created_at + 7 gün (mutlak sınır) |
| revoked_at / revoke_reason | timestamp NULL / enum NULL | `LOGOUT`, `IDLE_TIMEOUT`, `EXPIRED`, `TOKEN_REUSE`, `ACCOUNT_DISABLED` |
| created_at / updated_at | timestamp | |

Refresh token için ayrı tablo yoktur; rotation bu satırdaki iki hash kolonuyla yapılır.

#### T02b `login_attempts`
| Kolon | Tip | Not |
|---|---|---|
| id | uuid PK | |
| email | string | normalize; kullanıcı olmasa da tutulur (hesap varlığı sızmaz) |
| ip_address | string | UNIQUE (email, ip_address) |
| failed_count | int | son hata 15 dk'dan eskiyse 1'den başlar |
| locked_until | timestamp NULL | (e-posta, IP) 5 hatada +15 dk |
| last_failed_at | timestamp | |
| created_at / updated_at | timestamp | |

Kilit kuralları (FR-01):
- Aynı (e-posta, IP) çiftinden 5 hata gelirse yalnızca o IP kilitlenir.
- Bir e-postaya tüm IP'lerden 15 dk içinde toplam 20 hata gelirse e-posta kilitlenir.
- Başarılı girişte o (e-posta, IP) satırı silinir.

#### T03 `password_reset_tokens`
id, user_id FK, token_hash, expires_at, used_at NULL, created_at.

#### T04 `surveys`
| Kolon | Tip | Not |
|---|---|---|
| id | uuid PK | |
| owner_id | uuid | admin'in user id'si (BR-09); modül sınırı nedeniyle JPA ilişkisi yok |
| title | string | kaydetmede zorunlu |
| description | string NULL | |
| status | enum | `DRAFT`, `PUBLISHED`, `EXPIRED`, `CLOSED`, `ARCHIVED`; ilk kayıtta `DRAFT` |
| response_mode | enum NULL | `ANONYMOUS`, `IDENTIFIED`; yayında zorunlu, sonra değişmez (BR-05) |
| ends_at | timestamp NULL | yayında zorunlu ve gelecekte (FR-20); Europe/Istanbul gösterim (FR-50) |
| published_at / closed_at / archived_at | timestamp NULL | |
| created_at / updated_at | timestamp | |

Taslak yarım kaydedilebildiği için `title` dışındaki alanlar nullable'dır; tamlık yayınlamada kontrol edilir.
Eşzamanlı düzenlemede son kaydeden kazanır (sürüm kolonu yok). Şu kolonlar ilgili özellikler geldiğinde eklenecek:
`template_id` (FR-31), `status_before_archive`, `auto_reminder_days_before` (FR-44), `contact_info` (FR-65).

> Anket yapısı **bölümsüzdür**: anket → sorular → seçenekler. `survey_sections` tablosu yoktur.

#### T06 `questions`
| Kolon | Tip | Not |
|---|---|---|
| id | uuid PK | |
| survey_id | uuid FK | |
| sort_order | int | gösterim sırası; soru numarası = sort_order + 1 |
| text | string NULL | yayında zorunlu |
| type | string NULL | `LIKERT`, `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `FREE_TEXT`; CHECK kısıtı yok (NFR-20 genişletilebilirlik) |
| required | bool | |
| settings | jsonb NULL | tipe özel, varsayılanlarla tamamlanır: Likert `{scale, minLabel, maxLabel}`, tekli `{allowOther, display}`, çoklu `{allowOther, minSelect, maxSelect}`, metin `{multiline, maxLength}` |
| created_at / updated_at | timestamp | |

#### T07 `question_options`
id, question_id FK, sort_order, label NULL, created_at, updated_at. "Diğer" seçeneği ayrı satır değil; sorunun `settings.allowOther` alanıyla ifade edilir.

#### T08 `survey_shares` (F2)
survey_id FK, user_id FK, role (`CO_OWNER`, `VIEWER`), created_at. PK (survey_id, user_id).

#### T09 `templates`
id, owner_id FK, name, description, content (json — soru/seçenek ağacı; snapshot olduğu için anketlerden bağımsız), visibility (`PRIVATE`, `TEAM` — F2), created_at, updated_at.

#### T10 `participants`
id, email UNIQUE (normalize), full_name NULL, created_at, anonymized_at NULL (KVKK silme talebi — NFR-06).

> Katılımcı kişi düzeyinde global tutulur; anket bağlantısı `invitations` üzerindendir. Böylece KVKK silme talebi tek kayıttan yürütülür.

#### T11 `participant_imports`
id, survey_id FK, file_name, status (`VALIDATED`, `CONFIRMED`, `CANCELLED`), total_rows, valid_rows, invalid_rows, duplicate_rows, errors (json: `[{row, value, reason}]`), staged_rows (json — onay bekleyen geçerli satırlar), created_by FK, created_at, confirmed_at.

#### T12 `invitations`
| Kolon | Tip | Not |
|---|---|---|
| id | uuid PK | |
| survey_id | uuid FK | |
| participant_id | uuid FK | UNIQUE (survey_id, participant_id) — FR-40 |
| source | enum | `MANUAL`, `CSV` |
| token_hash | string UNIQUE | ≥128 bit token, yalnızca hash (NFR-02) |
| status | enum | `CREATED`, `SENT`, `STARTED`, `COMPLETED`, `EXPIRED`, `CANCELLED`, `FAILED` |
| sent_at / first_opened_at / last_opened_at / completed_at / cancelled_at | timestamp NULL | |
| reminder_count | int | |
| last_reminded_at | timestamp NULL | |
| created_at | timestamp | |

> Token geçerliliği ayrı kolonda tutulmaz; `surveys.ends_at`’ten türetilir. Böylece süre uzatmada (FR-51) tüm tokenlar kendiliğinden güncellenir.

#### T13 `response_drafts`
invitation_id PK/FK, answers (json), current_question_id NULL, started_at, updated_at.

> **Tasarım kararı (BR-06 / Q-03):** Ara kayıt davete bağlı olmak zorunda (kaldığı yerden devam). Anonim modda yanıt ile kimlik arasında ilişki kalmaması için gönderimde taslak `responses`/`answers`’a kopyalanır ve taslak **silinir**; anonim modda `responses.invitation_id` boş kalır.

#### T14 `responses`
id, survey_id FK, invitation_id FK NULL (yalnızca `IDENTIFIED` modda dolu, UNIQUE), started_at, submitted_at, duration_seconds.

> Anonim modda `submitted_at` ile `invitations.completed_at` eşleştirilerek kimlik çıkarımı yapılabilir; anonim modda `submitted_at` saat/gün hassasiyetine yuvarlanması önerilir.

#### T15 `answers`
id, response_id FK, question_id FK, numeric_value NULL (Likert), text_value NULL (serbest metin / “Diğer”). UNIQUE (response_id, question_id).

#### T16 `answer_options`
answer_id FK, option_id FK. PK (answer_id, option_id).

#### T17 `email_templates`
id, survey_id FK NULL (NULL = sistem varsayılanı), type (`INVITATION`, `REMINDER`, `DEADLINE_EXTENDED`, `DISPATCH_SUMMARY`, `SURVEY_CLOSED`), subject, body. Değişkenler: `{ad}`, `{anket_adi}`, `{link}`, `{son_tarih}`.

#### T18 `dispatches`
id, survey_id FK, type (`INITIAL`, `ADDITIONAL`, `REMINDER`, `DEADLINE_EXTENDED`, `RESEND`), status (`QUEUED`, `IN_PROGRESS`, `COMPLETED`), total_count, sent_count, failed_count, triggered_by (FK users NULL = otomatik), created_at, completed_at.

#### T19 `email_messages`
id, dispatch_id FK NULL, invitation_id FK NULL, type, recipient_email, subject, body_rendered, status (`QUEUED`, `SENDING`, `SENT`, `FAILED`, `BOUNCED`), attempt_count (max 3 — NFR-42), last_error, provider_message_id, next_attempt_at, sent_at, created_at.

#### T20 `export_jobs`
id, survey_id FK, format (`CSV`, `PDF`), filters (json), status (`QUEUED`, `RUNNING`, `READY`, `FAILED`), file_location, expires_at, requested_by FK, created_at, completed_at.

#### T21 `audit_logs`
id, user_id FK NULL, action (ör. `SURVEY_PUBLISHED`, `SURVEY_DELETED`, `EXPORT_DOWNLOADED`, `DEADLINE_EXTENDED`), entity_type, entity_id, details (json), ip, user_agent, created_at. Kişisel veri / token yazılmaz (NFR-43).

#### T22 `system_settings`
key PK, value (json), updated_by, updated_at. Örnek anahtarlar: `mail.provider`, `mail.from`, `report.anonymity_threshold` (5), `data.retention_days` (730), `auth.lockout`.

### 1.3 İlişkiler

```
users 1─N surveys 1─N questions 1─N question_options
users 1─N templates
surveys 1─N invitations N─1 participants
invitations 1─0..1 response_drafts
invitations 1─0..1 responses        (yalnız IDENTIFIED)
surveys 1─N responses 1─N answers 1─N answer_options N─1 question_options
surveys 1─N dispatches 1─N email_messages N─0..1 invitations
surveys 1─N email_templates | participant_imports | export_jobs | survey_shares
```

---

## 2. API Listesi

Taban yol: `/api/v1`. Admin uçları oturum gerektirir; `/public/*` uçları yalnızca davet tokenı ile çalışır ve rate-limit uygulanır (NFR-03).

### 2.1 Kimlik Doğrulama (S-01)
| ID | Metot | Yol | Açıklama | Ref |
|---|---|---|---|---|
| API-01 | POST | `/auth/login` | E-posta + şifre ile giriş, kilitleme kuralı | FR-01 |
| API-02 | POST | `/auth/refresh` | Oturum yenileme | FR-02 |
| API-03 | POST | `/auth/logout` | Oturumu sonlandır | |
| API-04 | GET | `/auth/me` | Oturumdaki kullanıcı | |
| API-05 | POST | `/auth/password/forgot` | Sıfırlama linki gönder | FR-03 |
| API-06 | POST | `/auth/password/reset` | Token ile yeni şifre belirle | FR-03 |
| API-07 | GET | `/auth/sso/{provider}/authorize` + `/callback` | Kurumsal SSO (F3 / Q-01) | FR-01 |

### 2.2 Kullanıcı Yönetimi — Sistem Yöneticisi
| ID | Metot | Yol | Açıklama | Ref |
|---|---|---|---|---|
| API-10 | GET | `/admin/users` | Admin listesi | Rol tablosu |
| API-11 | POST | `/admin/users` | Admin hesabı aç (davet e-postası) | Q-01 |
| API-12 | GET | `/admin/users/{userId}` | Detay | |
| API-13 | PATCH | `/admin/users/{userId}` | Ad, rol, durum (pasifleştirme) | |
| API-14 | POST | `/admin/users/{userId}/unlock` | Kilidi kaldır | FR-01 |

### 2.3 Anketler (S-02, S-03)
| ID | Metot | Yol | Açıklama | Ref |
|---|---|---|---|---|
| API-20 | GET | `/admin/surveys` | Liste: arama, durum filtresi, sıralama, sayfalama; soru sayısı | FR-10, BR-09 |
| API-21 | POST | `/admin/surveys/upsert` | **Kaydet (upsert)**: `id` yoksa oluştur, varsa güncelle; sorular ve seçenekler gövdede | FR-12, FR-20, FR-27, BR-04, BR-05 |
| API-22 | GET | `/admin/surveys/{surveyId}` | Anket + soru + seçenek ağacı; izinli aksiyonlar | FR-11 |
| API-24 | DELETE | `/admin/surveys/{surveyId}` | Sil (yalnızca DRAFT) | BR-08 |
| API-25 | POST | `/surveys/{surveyId}/duplicate` | Kopyala (katılımcı/yanıt hariç) | FR-11 |
| API-26 | POST | `/admin/surveys/{surveyId}/publish` | Yayınla: `USER` rolündeki seçili kullanıcıları ankete ekle, davetleri gönder (yeniden tasarlanacak) | FR-42, S-06 |
| API-27 | POST | `/surveys/{surveyId}/close` | Manuel kapat | 3.2 |
| API-28 | POST | `/surveys/{surveyId}/archive` | Arşivle | 3.2 |
| API-29 | POST | `/surveys/{surveyId}/unarchive` | Geri al | 3.2 |
| API-30 | PUT | `/surveys/{surveyId}/deadline` | Bitiş uzat; ops. bilgilendirme e-postası; EXPIRED → PUBLISHED | FR-51, AS-04 |
| API-31 | GET | `/surveys/{surveyId}/preview` | Katılımcı formatında önizleme (yanıt kaydedilmez) | FR-26 |

### 2.4 Sorular (S-03, S-04)
Sorular ve seçenekler ayrı uçlarla değil, anket kaydetme ucuyla (API-21) yönetilir: ekleme, silme, düzenleme ve sıralama tek istekte yapılır.

| ID | Metot | Yol | Açıklama | Ref |
|---|---|---|---|---|
| API-48 | GET | `/question-types` | Desteklenen tipler ve ayar şemaları (strateji/eklenti) | NFR-20 |

### 2.5 Şablonlar
| ID | Metot | Yol | Açıklama | Ref |
|---|---|---|---|---|
| API-50 | GET | `/templates` | Şablon listesi (kişisel + paylaşılan) | FR-31, FR-32 |
| API-51 | POST | `/surveys/{surveyId}/save-as-template` | Anketi şablon olarak kaydet | FR-30 |
| API-52 | GET | `/templates/{templateId}` | Detay | FR-31 |
| API-53 | PATCH | `/templates/{templateId}` | Ad, açıklama, içerik, görünürlük | FR-31, FR-32 |
| API-54 | DELETE | `/templates/{templateId}` | Sil (türetilmiş anketler etkilenmez) | FR-31 |

### 2.6 Katılımcılar ve Davetler (S-05)
| ID | Metot | Yol | Açıklama | Ref |
|---|---|---|---|---|
| API-60 | GET | `/surveys/{surveyId}/participants` | Katılımcı + davet durumu listesi (anonimde yalnız tamamlanma) | FR-45 |
| API-61 | POST | `/surveys/{surveyId}/participants` | Bir veya birden çok e-posta ekle; yayındaysa yalnız yenilere davet | FR-40, FR-43 |
| API-62 | DELETE | `/surveys/{surveyId}/participants/{invitationId}` | Çıkar (taslakta sil, yayında İptal) | FR-40, 3.3 |
| API-63 | GET | `/participants/import-template` | Örnek CSV indir | FR-41 |
| API-64 | POST | `/surveys/{surveyId}/participant-imports` | CSV yükle → doğrulama özeti (geçerli/hatalı/tekrar) | FR-41, AS-05 |
| API-65 | POST | `/surveys/{surveyId}/participant-imports/{importId}/confirm` | İçe aktarımı onayla | AS-05 |
| API-66 | POST | `/surveys/{surveyId}/reminders` | Yanıtlamayanlara manuel hatırlatma | FR-44 |
| API-67 | POST | `/surveys/{surveyId}/invitations/resend` | Hatalı/seçili davetleri yeniden gönder | 3.3 Hata |
| API-68 | GET | `/surveys/{surveyId}/dispatches` + `/{dispatchId}` | Gönderim geçmişi ve özeti | S-06 |

### 2.7 E-posta Şablonları
| ID | Metot | Yol | Açıklama | Ref |
|---|---|---|---|---|
| API-70 | GET | `/surveys/{surveyId}/email-templates` | Ankete ait (yoksa varsayılan) şablonlar | FR-46 |
| API-71 | PUT | `/surveys/{surveyId}/email-templates/{type}` | Konu/gövde özelleştir | FR-46 |
| API-72 | POST | `/surveys/{surveyId}/email-templates/{type}/preview` | Değişkenler doldurulmuş önizleme | FR-46 |

### 2.8 Katılımcı Formu — Public (S-08, S-09, S-10)
| ID | Metot | Yol | Açıklama | Ref |
|---|---|---|---|---|
| API-80 | GET | `/public/surveys/{token}` | Token doğrula; form + taslak yanıtlar + kaldığı soru. Hata: geçersiz/iptal, süresi doldu, zaten yanıtlandı | FR-60, FR-65, AS-01–03 |
| API-81 | PUT | `/public/surveys/{token}/draft` | Ara kayıt (sayfa geçişinde) | FR-63, AS-02 |
| API-82 | POST | `/public/surveys/{token}/submit` | Zorunlu alan kontrolü, yanıtı kesinleştir, tokenı tüket | FR-62, FR-64, BR-01–03 |

### 2.9 Raporlama ve Dışa Aktarım (S-07)
| ID | Metot | Yol | Açıklama | Ref |
|---|---|---|---|---|
| API-90 | GET | `/surveys/{surveyId}/report/summary` | Davet, yanıt, katılım oranı, ort. skor, ort. süre | FR-70, BR-10 |
| API-91 | GET | `/surveys/{surveyId}/report/questions` | Soru bazlı dağılım; eşik altıysa kırılım yok | FR-71, BR-07 |
| API-92 | GET | `/surveys/{surveyId}/report/questions/{questionId}/text-answers` | Serbest metin yanıtları, arama + sayfalama | FR-72 |
| API-93 | GET | `/surveys/{surveyId}/report/responses` + `/{responseId}` | Katılımcı bazlı yanıtlar (yalnız IDENTIFIED) | FR-74 |
| API-94 | POST | `/surveys/{surveyId}/exports` | CSV/PDF dışa aktarım başlat (küçükse hazır, büyükse 202) | FR-73, NFR-13 |
| API-95 | GET | `/exports/{exportId}` | Durum | NFR-13 |
| API-96 | GET | `/exports/{exportId}/download` | Dosyayı indir (audit kaydı) | FR-73, NFR-07 |

### 2.10 Sistem / Operasyon
| ID | Metot | Yol | Açıklama | Ref |
|---|---|---|---|---|
| API-100 | GET | `/admin/audit-logs` | Denetim kayıtları (filtreli) | NFR-07 |
| API-101 | GET / PUT | `/admin/settings` | Genel ayarlar | NFR-21 |
| API-102 | POST | `/admin/settings/mail/test` | Test e-postası | NFR-21 |
| API-103 | DELETE | `/admin/participants/{participantId}` | KVKK silme / anonimleştirme talebi | NFR-06 |
| API-104 | POST | `/webhooks/email-events` | Sağlayıcıdan bounce/teslim bildirimi | 3.3 Hata, NFR-42 |

### 2.11 Arka Plan İşleri (API değil, referans için)
| ID | İş | Sıklık | Ref |
|---|---|---|---|
| JOB-01 | Süresi dolan anketleri `EXPIRED` yap, davetleri güncelle, “Anket kapandı” e-postası | 1 dk | FR-52 |
| JOB-02 | E-posta kuyruğunu işle, 3 kez yeniden dene | sürekli | NFR-10, NFR-42 |
| JOB-03 | Otomatik hatırlatma (`auto_reminder_days_before`) | saatlik | FR-44 |
| JOB-04 | Büyük dışa aktarımları üret, bitince bildir | kuyruk | NFR-13 |
| JOB-05 | Saklama süresi dolan verileri anonimleştir/sil | günlük | Q-05 |

---

## 3. Bu dokümanda alınan, onay bekleyen kararlar
1. **Ara kayıt ayrı tabloda (`response_drafts`)**: Anonim modda gönderimde davetle bağ koparılır (BR-06, Q-03).
2. **Token geçerliliği `surveys.ends_at`’ten türetilir**: Uzatma tek güncellemeyle tüm tokenlara yansır (FR-51).
3. **Katılımcı global, davet anket bazlı**: Tekrar kontrolü `(survey_id, participant_id)` üzerinden yapılır.
4. **CSV içe aktarım iki adımlı** (yükle → onayla): AS-05’teki “onaylar” adımı.
5. **Anket içeriği tek uçta** (API-21, upsert): Soru/seçenek ekleme, silme ve sıralama tek kaydetme isteğiyle yapılır; bölüm yoktur.
6. **SignUp ucu yok**: Admin’i Sistem Yöneticisi açıyor (API-11); SSO F3’te (Q-01).
