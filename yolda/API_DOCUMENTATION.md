# Yolda: API Documentation (v1)

Bu sənəd backend və frontend üçün **yeganə həqiqət mənbəyidir**. Endpoint yolları, sahə adları, enum dəyərləri və xəta formatı burada yazıldığı kimi olmalıdır. Sənəddə olmayan sahə və ya endpoint uydurulmamalıdır.

---

## 0. Ümumi qaydalar

| Mövzu | Qayda |
|---|---|
| Base URL | `http://localhost:8080` (bütün endpoint-lər `/api` ilə başlayır) |
| Format | JSON (`Content-Type: application/json`), fayl yükləmələri `multipart/form-data` |
| Sahə adları | `camelCase` |
| Tarix/vaxt | ISO-8601, UTC: `2026-10-31T17:05:00Z` |
| Pul | `price` rəqəmdir (məsələn `15500.00`), valyuta ayrıca `currency` sahəsindədir |
| ID | `Long` (rəqəm) |
| Səhifələmə | 0-dan başlayır: `?page=0&size=20`. `size` max 50, default 20 |
| Autentifikasiya | `Authorization: Bearer <token>` |
| Boş cavab | `204 No Content` (silmə, şifrə dəyişmə və s.) |
| `null` sahələr | Cavabda `null` olaraq qalır, sahə silinmir |

### Giriş icazəsi legendası
- **Public**: token lazım deyil
- **Auth**: istənilən daxil olmuş istifadəçi (USER, MODERATOR, ADMIN)
- **Owner**: yalnız obyektin sahibi (başqası `403` və ya `404` alır)
- **MOD**: MODERATOR və ADMIN
- **ADMIN**: yalnız ADMIN

### Səhifələmə cavabı: `PageResponse<T>`
```json
{
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 134,
  "totalPages": 7,
  "first": true,
  "last": false
}
```

### Xəta cavabı: `ErrorResponse` (bütün xətalarda eyni)
```json
{
  "timestamp": "2026-10-31T17:05:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Göndərilən məlumatlar yanlışdır",
  "path": "/api/listings",
  "fieldErrors": [
    { "field": "price", "message": "Qiymət müsbət olmalıdır" }
  ]
}
```
`fieldErrors` yalnız `VALIDATION_ERROR` zamanı doldurulur, digər hallarda `[]` olur.

### Xəta kodları (`code`)

| code | HTTP | Nə vaxt |
|---|---|---|
| `VALIDATION_ERROR` | 400 | `@Valid` yoxlaması keçmədi |
| `INVALID_IMAGE` | 400 | Şəkil sayı (3–15), format (jpg/png/webp) və ya ölçü (max 5MB) səhvdir |
| `INVALID_CREDENTIALS` | 401 | Email və ya şifrə səhvdir |
| `UNAUTHORIZED` | 401 | Token yoxdur, yanlışdır və ya vaxtı bitib |
| `FORBIDDEN` | 403 | İcazə yoxdur (rol və ya başqasının elanı) |
| `USER_BLOCKED` | 403 | İstifadəçi bloklanıb |
| `NOT_FOUND` | 404 | Resurs tapılmadı |
| `DUPLICATE_RESOURCE` | 409 | Email artıq var, elan artıq sevimlidir, təkrar şikayət və s. |
| `LISTING_LIMIT_EXCEEDED` | 409 | 5 aktiv elan limiti dolub |
| `INVALID_STATUS_TRANSITION` | 409 | İcazəsiz status keçidi |
| `SELF_ACTION` | 409 | Öz elanına mesaj yazmaq və ya sevimliyə əlavə etmək |
| `FILE_UPLOAD_ERROR` | 502 | Cloudinary yükləmə xətası |
| `INTERNAL_ERROR` | 500 | Gözlənilməz xəta |

> **401 və 403 fərqi:** `401` token yoxdur/etibarsızdır, frontend login səhifəsinə yönləndirir. `403` token düzgündür, amma icazə yoxdur, frontend xəta mesajı göstərir.

---

## 1. Enum-lar

| Enum | Dəyərlər |
|---|---|
| `Role` | `USER`, `MODERATOR`, `ADMIN` |
| `ListingStatus` | `PENDING`, `ACTIVE`, `REJECTED`, `EXPIRED`, `SOLD`, `DEACTIVATED` |
| `Currency` | `AZN`, `USD`, `EUR` |
| `FuelType` | `PETROL`, `DIESEL`, `GAS`, `HYBRID`, `ELECTRIC` |
| `TransmissionType` | `MANUAL`, `AUTOMATIC`, `SEMI_AUTOMATIC` |
| `BodyType` | `SEDAN`, `HATCHBACK`, `SUV`, `COUPE`, `WAGON`, `MINIVAN`, `PICKUP` |
| `DriveType` | `FRONT`, `REAR`, `ALL` |
| `ReportReason` | `SPAM`, `FAKE`, `WRONG_INFO`, `SOLD`, `OTHER` |
| `ReportStatus` | `OPEN`, `RESOLVED`, `DISMISSED` |

Enum-lar API-də həmişə **ingilis böyük hərflə** göndərilir. Azərbaycanca göstərmə (məsələn `AUTOMATIC` → "Avtomat") frontend-in işidir.

### Elan status keçidləri

```
PENDING ──approve──> ACTIVE
PENDING ──reject───> REJECTED
ACTIVE ──edit──────> PENDING        (redaktədən sonra yenidən moderasiya)
ACTIVE ──deactivate> DEACTIVATED
DEACTIVATED ─activate> ACTIVE
ACTIVE ──sold──────> SOLD
ACTIVE ──30 gün────> EXPIRED        (scheduler)
ACTIVE / EXPIRED ─renew> ACTIVE     (expiresAt = indi + 30 gün)
ACTIVE ──moderator hide──> DEACTIVATED
ACTIVE ──5 şikayət──> PENDING       (avtomatik)
REJECTED ──edit────> PENDING        (düzəldib yenidən göndərmək)
```
Bunlardan kənar hər keçid `409 INVALID_STATUS_TRANSITION` qaytarır. `REJECTED → ACTIVE` birbaşa mümkün deyil.

---

## 2. Ortaq (nested) obyektlər

### `IdNameResponse`
```json
{ "id": 1, "name": "BMW" }
```

### `SellerResponse`
```json
{ "id": 7, "fullName": "Əli Məmmədov", "phone": "+994501234567", "avatarUrl": "https://...", "memberSince": "2026-01-10T09:00:00Z" }
```

### `ListingImageResponse`
```json
{ "id": 55, "url": "https://res.cloudinary.com/...", "position": 0 }
```
`position = 0` olan şəkil əsas şəkildir.

---

## 3. Auth: `/api/auth`

### 3.1 `POST /api/auth/register` (Public)
Cavab: `201 Created`

**Request: `RegisterRequest`**
| Sahə | Tip | Qayda |
|---|---|---|
| `email` | string | məcburi, düzgün email, unikal |
| `password` | string | məcburi, 8–64 simvol, ən azı 1 hərf və 1 rəqəm |
| `fullName` | string | məcburi, 2–100 simvol |
| `phone` | string | məcburi, format `+994XXXXXXXXX` |

**Response: `AuthResponse`**
```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": 7, "email": "ali@mail.com", "fullName": "Əli Məmmədov",
    "role": "USER", "enabled": true, "blocked": false,
    "createdAt": "2026-10-31T17:05:00Z"
  }
}
```
Xətalar: `400 VALIDATION_ERROR`, `409 DUPLICATE_RESOURCE` (email var).

### 3.2 `POST /api/auth/login` (Public)
**Request: `LoginRequest`**: `email` (string, məcburi), `password` (string, məcburi)

**Response**: `AuthResponse` (3.1 ilə eyni). Cavab: `200`.
Xətalar: `401 INVALID_CREDENTIALS`, `403 USER_BLOCKED`.

> Logout backend-də yoxdur. Frontend tokeni saxladığı yerdən silir. `expiresIn` saniyə ilə JWT ömrüdür. Refresh token yoxdur, token bitəndə `401` gəlir və istifadəçi yenidən login olur.

---

## 4. İstifadəçi və profil: `/api/users`

### 4.1 `GET /api/users/me` (Auth)
Response: `UserResponse`
```json
{
  "id": 7, "email": "ali@mail.com", "fullName": "Əli Məmmədov",
  "role": "USER", "enabled": true, "blocked": false,
  "createdAt": "2026-10-31T17:05:00Z"
}
```

### 4.2 `GET /api/users/me/profile` (Auth)
Response: `ProfileResponse`
```json
{
  "userId": 7, "fullName": "Əli Məmmədov", "phone": "+994501234567",
  "city": { "id": 1, "name": "Bakı" }, "avatarUrl": null
}
```
`city` və `avatarUrl` `null` ola bilər.

### 4.3 `PUT /api/users/me/profile` (Auth)
**Request: `UpdateProfileRequest`**
| Sahə | Tip | Qayda |
|---|---|---|
| `fullName` | string | məcburi, 2–100 |
| `phone` | string | məcburi, `+994XXXXXXXXX` |
| `cityId` | number | ixtiyari (`null` ola bilər), mövcud şəhər olmalıdır |

Response: `ProfileResponse`

### 4.4 `POST /api/users/me/avatar` (Auth)
`multipart/form-data`, part adı: **`file`** (jpg/png/webp, max 5MB)
Response: `ProfileResponse` (yeni `avatarUrl` ilə)

### 4.5 `PUT /api/users/me/password` (Auth)
**Request: `ChangePasswordRequest`**: `oldPassword` (məcburi), `newPassword` (8–64, hərf+rəqəm)
Response: `204`. Köhnə şifrə səhvdirsə `400 VALIDATION_ERROR` (`fieldErrors[0].field = "oldPassword"`).

---

## 5. Kataloq: marka, model, şəhər

### Public (cache-lənir)

| Metod | Yol | Response |
|---|---|---|
| GET | `/api/brands` | `BrandResponse[]` (səhifəsiz, ada görə A–Z) |
| GET | `/api/brands/{brandId}/models` | `CarModelResponse[]` |
| GET | `/api/cities` | `CityResponse[]` |

`BrandResponse`:
```json
{ "id": 1, "name": "BMW", "logoUrl": "https://..." }
```
`CarModelResponse`:
```json
{ "id": 10, "name": "X5", "brandId": 1 }
```
`CityResponse`:
```json
{ "id": 1, "name": "Bakı" }
```
Brend tapılmazsa `404 NOT_FOUND`.

### Admin CRUD (ADMIN)

| Metod | Yol | Request | Response |
|---|---|---|---|
| POST | `/api/admin/brands` | `BrandRequest` | `201 BrandResponse` |
| PUT | `/api/admin/brands/{id}` | `BrandRequest` | `BrandResponse` |
| POST | `/api/admin/brands/{id}/logo` | multipart `file` | `BrandResponse` |
| DELETE | `/api/admin/brands/{id}` | n/a | `204` |
| POST | `/api/admin/models` | `CarModelRequest` | `201 CarModelResponse` |
| PUT | `/api/admin/models/{id}` | `CarModelRequest` | `CarModelResponse` |
| DELETE | `/api/admin/models/{id}` | n/a | `204` |
| POST | `/api/admin/cities` | `CityRequest` | `201 CityResponse` |
| PUT | `/api/admin/cities/{id}` | `CityRequest` | `CityResponse` |
| DELETE | `/api/admin/cities/{id}` | n/a | `204` |

- `BrandRequest`: `name` (string, məcburi, 1–50, unikal)
- `CarModelRequest`: `name` (string, məcburi, 1–50), `brandId` (number, məcburi)
- `CityRequest`: `name` (string, məcburi, 1–50, unikal)

Təkrar ad: `409 DUPLICATE_RESOURCE`. Elanı olan marka, model və ya şəhəri silmək mümkün deyil: `409 DUPLICATE_RESOURCE` qaytarılır, `message` sahəsində "İstifadədədir" yazılır.

---

## 6. Elanlar: `/api/listings`

### 6.1 Cavab obyektləri

**`ListingSummaryResponse`** (siyahı üçün):
```json
{
  "id": 101,
  "title": "BMW X5, 2018",
  "mainImageUrl": "https://...",
  "price": 45000.00,
  "currency": "USD",
  "year": 2018,
  "mileage": 98000,
  "engineVolume": 3.0,
  "fuelType": "DIESEL",
  "transmission": "AUTOMATIC",
  "city": { "id": 1, "name": "Bakı" },
  "vip": true,
  "favorite": false,
  "status": "ACTIVE",
  "createdAt": "2026-10-20T10:00:00Z"
}
```
- `title` backenddə avtomatik qurulur: `"{marka} {model}, {il}"`
- `favorite`: token varsa cari istifadəçi üçün, yoxdursa həmişə `false`
- `vip`: `vipUntil` hələ bitməyibsə `true`

**`ListingResponse`** (elan səhifəsi):
```json
{
  "id": 101,
  "title": "BMW X5, 2018",
  "brand": { "id": 1, "name": "BMW" },
  "model": { "id": 10, "name": "X5" },
  "city": { "id": 1, "name": "Bakı" },
  "price": 45000.00,
  "currency": "USD",
  "year": 2018,
  "mileage": 98000,
  "engineVolume": 3.0,
  "horsepower": 249,
  "fuelType": "DIESEL",
  "transmission": "AUTOMATIC",
  "bodyType": "SUV",
  "driveType": "ALL",
  "color": "Qara",
  "description": "Təzə kimidir...",
  "contactPhone": "+994501234567",
  "images": [ { "id": 55, "url": "https://...", "position": 0 } ],
  "seller": { "id": 7, "fullName": "Əli Məmmədov", "phone": "+994501234567", "avatarUrl": null, "memberSince": "2026-01-10T09:00:00Z" },
  "status": "ACTIVE",
  "rejectReason": null,
  "vip": true,
  "vipUntil": "2026-11-10T10:00:00Z",
  "viewCount": 312,
  "favorite": false,
  "mine": false,
  "expiresAt": "2026-11-19T10:00:00Z",
  "createdAt": "2026-10-20T10:00:00Z",
  "updatedAt": "2026-10-25T12:00:00Z"
}
```
- `mine`: cari istifadəçi elanın sahibidir
- `rejectReason`: yalnız `REJECTED` olanda və yalnız sahibə/moderatora görünür, digər hallarda `null`
- `vipUntil`: VIP yoxdursa `null`

### 6.2 `GET /api/listings`: axtarış (Public)
Yalnız `ACTIVE` elanlar qaytarılır. Bütün parametrlər ixtiyaridir və birlikdə işləyir (AND).

| Query parametr | Tip | İzah |
|---|---|---|
| `brandId` | number | marka |
| `modelId` | number | model |
| `cityId` | number | şəhər |
| `minPrice`, `maxPrice` | number | qiymət aralığı |
| `currency` | `Currency` | verilərsə yalnız həmin valyutalı elanlar. Qiymət filtri ilə birlikdə göndərilməlidir |
| `minYear`, `maxYear` | number | il aralığı |
| `minMileage`, `maxMileage` | number | yürüş aralığı (km) |
| `fuelType` | `FuelType` | yanacaq |
| `transmission` | `TransmissionType` | qutu |
| `bodyType` | `BodyType` | kuzov |
| `driveType` | `DriveType` | ötürücü |
| `sort` | string | aşağıda |
| `page`, `size` | number | səhifələmə |

`sort` dəyərləri: `newest` (default), `price_asc`, `price_desc`, `year_desc`, `mileage_asc`.
**VIP elanlar həmişə yuxarıda çıxır**, seçilmiş sıralama VIP qrupunun daxilində və sonra adi elanlar qrupunda tətbiq olunur.

Nümunə: `GET /api/listings?brandId=1&minPrice=10000&maxPrice=50000&currency=USD&sort=price_asc&page=0&size=20`

Response: `PageResponse<ListingSummaryResponse>`
Yanlış enum dəyəri: `400 VALIDATION_ERROR`.

### 6.3 `GET /api/listings/{id}` (Public)
Response: `ListingResponse`
- `ACTIVE` elanı hər kəs görür, baxış sayı **1 artır** (sahibi öz elanına baxanda artmır).
- Digər statusları yalnız sahibi və MOD görür, başqaları üçün `404 NOT_FOUND`.
- Bloklanmış istifadəçinin elanı hər kəs üçün `404`.

### 6.4 `POST /api/listings`: elan yaratma (Auth)
`multipart/form-data`, **iki növ part**:
- `data`: JSON (Content-Type `application/json`), `CreateListingRequest`
- `images`: 3–15 fayl (jpg/png/webp, hər biri max 5MB). Sıra göndərilmə sırasıdır, birinci şəkil əsasdır

Frontend nümunəsi:
```js
const form = new FormData();
form.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
files.forEach(f => form.append('images', f));
fetch('/api/listings', { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: form });
// Content-Type header əl ilə yazılmır
```

**`CreateListingRequest`**
| Sahə | Tip | Qayda |
|---|---|---|
| `brandId` | number | məcburi |
| `modelId` | number | məcburi, seçilmiş markaya aid olmalıdır |
| `cityId` | number | məcburi |
| `price` | number | məcburi, > 0, max 9 999 999 |
| `currency` | `Currency` | məcburi |
| `year` | number | məcburi, 1950 ilə cari il arası |
| `mileage` | number | məcburi, ≥ 0 (km) |
| `engineVolume` | number | məcburi, 0.1–10.0 (litr). Elektrik üçün `0` icazəlidir |
| `horsepower` | number | məcburi, 1–2000 |
| `fuelType` | `FuelType` | məcburi |
| `transmission` | `TransmissionType` | məcburi |
| `bodyType` | `BodyType` | məcburi |
| `driveType` | `DriveType` | məcburi |
| `color` | string | məcburi, 2–30 simvol |
| `description` | string | ixtiyari, max 2000 simvol |
| `contactPhone` | string | məcburi, `+994XXXXXXXXX` |

Response: `201 Created`, `ListingResponse` (`status = "PENDING"`).
Xətalar: `400 VALIDATION_ERROR`, `400 INVALID_IMAGE`, `403 USER_BLOCKED`, `409 LISTING_LIMIT_EXCEEDED` (5 aktiv elan).

> Limit yalnız `ACTIVE` elanları sayır. Bloklanmış istifadəçi elan yarada bilməz.

### 6.5 `PUT /api/listings/{id}`: redaktə (Owner)
JSON body, `UpdateListingRequest`: **`CreateListingRequest` ilə eyni sahələr, eyni qaydalar** (şəkillər ayrıca idarə olunur, bax 6.8).
Response: `ListingResponse`
- `ACTIVE` və ya `REJECTED` elan redaktə olunarsa status **`PENDING`** olur.
- `SOLD`, `EXPIRED` elan redaktə olunmur: `409 INVALID_STATUS_TRANSITION`.
- Başqasının elanı: `403 FORBIDDEN`.

### 6.6 `DELETE /api/listings/{id}` (Owner)
Response: `204`. Şəkillər Cloudinary-dən də silinir.

### 6.7 Status əməliyyatları (Owner)

| Metod | Yol | Keçid | Response |
|---|---|---|---|
| PATCH | `/api/listings/{id}/deactivate` | `ACTIVE → DEACTIVATED` | `ListingResponse` |
| PATCH | `/api/listings/{id}/activate` | `DEACTIVATED → ACTIVE` | `ListingResponse` |
| PATCH | `/api/listings/{id}/sold` | `ACTIVE → SOLD` | `ListingResponse` |
| PATCH | `/api/listings/{id}/renew` | `ACTIVE / EXPIRED → ACTIVE`, `expiresAt = indi + 30 gün` | `ListingResponse` |

Body yoxdur. `activate` və `renew` zamanı 5 aktiv elan limiti yenə yoxlanılır (`409 LISTING_LIMIT_EXCEEDED`). İcazəsiz keçid `409 INVALID_STATUS_TRANSITION`.

### 6.8 Şəkil idarəsi (Owner)

| Metod | Yol | Request | Response |
|---|---|---|---|
| POST | `/api/listings/{id}/images` | multipart, `images` (1 və ya daha çox fayl) | `ListingImageResponse[]` (elanın bütün şəkilləri) |
| DELETE | `/api/listings/{id}/images/{imageId}` | n/a | `204` |
| PUT | `/api/listings/{id}/images/order` | `ReorderImagesRequest` | `ListingImageResponse[]` |

- `ReorderImagesRequest`: `{ "imageIds": [55, 57, 56] }`, elanın bütün şəkil ID-ləri, istənilən yeni sırada.
- Əməliyyatdan sonra şəkil sayı 3–15 aralığında qalmalıdır, əks halda `400 INVALID_IMAGE`.
- `ACTIVE` elanın şəkli dəyişdirilərsə elan `PENDING` olur.

---

## 7. Mənim elanlarım: `/api/my/listings`

### 7.1 `GET /api/my/listings` (Auth)
| Query | Tip | İzah |
|---|---|---|
| `status` | `ListingStatus` | ixtiyari filtr |
| `page`, `size` | number | səhifələmə |

Response: `PageResponse<ListingSummaryResponse>` (ən yeni birinci). Rədd edilmiş elanın səbəbini görmək üçün frontend `GET /api/listings/{id}` çağırır.

### 7.2 `GET /api/my/listings/counts` (Auth)
Kabinet tab-ları üçün status üzrə say:
```json
{ "PENDING": 1, "ACTIVE": 3, "REJECTED": 0, "EXPIRED": 2, "SOLD": 1, "DEACTIVATED": 0 }
```

---

## 8. Sevimlilər: `/api/favorites`

| Metod | Yol | İcazə | Response |
|---|---|---|---|
| GET | `/api/favorites?page=0&size=20` | Auth | `PageResponse<ListingSummaryResponse>` (`favorite` həmişə `true`) |
| POST | `/api/favorites/{listingId}` | Auth | `201`, body yoxdur |
| DELETE | `/api/favorites/{listingId}` | Auth | `204` |

Xətalar: `409 DUPLICATE_RESOURCE` (artıq sevimlidir), `409 SELF_ACTION` (öz elanı), `404 NOT_FOUND` (elan yoxdur və ya ACTIVE deyil). Sevimlidən çıxarma idempotentdir (sevimlidə olmayan elan üçün də `204`).
Sevimlilər siyahısında artıq `ACTIVE` olmayan elanlar görünmür.

---

## 9. Mesajlaşma: `/api/conversations`

Söhbət **elan + alıcı + satıcı** üçlüyü ilə müəyyən olunur. Eyni alıcı eyni elan üçün yeni söhbət yox, mövcud söhbətə yazır.

### 9.1 Cavab obyektləri

**`ConversationResponse`**
```json
{
  "id": 3,
  "listing": { "id": 101, "title": "BMW X5, 2018", "mainImageUrl": "https://...", "price": 45000.00, "currency": "USD" },
  "otherUser": { "id": 7, "fullName": "Əli Məmmədov", "avatarUrl": null },
  "lastMessage": { "text": "Salam, hələ satılıb?", "createdAt": "2026-10-31T10:00:00Z", "mine": false },
  "unreadCount": 2,
  "updatedAt": "2026-10-31T10:00:00Z"
}
```
`otherUser` həmişə söhbətin **qarşı tərəfidir** (cari istifadəçi alıcıdırsa satıcı, satıcıdırsa alıcı).

**`MessageResponse`**
```json
{
  "id": 40, "conversationId": 3, "senderId": 12,
  "text": "Salam, hələ satılıb?", "mine": false,
  "createdAt": "2026-10-31T10:00:00Z", "readAt": null
}
```

### 9.2 Endpoint-lər

| Metod | Yol | İcazə | Request | Response |
|---|---|---|---|---|
| POST | `/api/conversations` | Auth | `StartConversationRequest` | `201` (yeni) və ya `200` (mövcud), `ConversationResponse` |
| GET | `/api/conversations?page=0&size=20` | Auth | n/a | `PageResponse<ConversationResponse>` (son mesaja görə) |
| GET | `/api/conversations/{id}/messages?page=0&size=30` | Tərəf | n/a | `PageResponse<MessageResponse>` (ən yeni birinci) |
| POST | `/api/conversations/{id}/messages` | Tərəf | `SendMessageRequest` | `201 MessageResponse` |
| GET | `/api/conversations/unread-count` | Auth | n/a | `{ "count": 5 }` |

- `StartConversationRequest`: `listingId` (number, məcburi), `text` (string, məcburi, 1–1000). İlk mesaj söhbətlə birlikdə yaranır.
- `SendMessageRequest`: `text` (string, məcburi, 1–1000)
- `GET .../messages` çağırılanda cari istifadəçiyə gələn oxunmamış mesajlar oxunmuş (`readAt` doldurulur) olur.
- Yalnız söhbətin iki tərəfi görə bilir, üçüncü şəxs üçün `404 NOT_FOUND`.
- Öz elanına yazmaq: `409 SELF_ACTION`.
- Real-time (WebSocket) yoxdur, frontend polling edə bilər (məsələn 10 saniyədən bir `unread-count`).

---

## 10. Şikayət: `/api/reports`

### 10.1 `POST /api/reports` (Auth)
**`CreateReportRequest`**
| Sahə | Tip | Qayda |
|---|---|---|
| `listingId` | number | məcburi |
| `reason` | `ReportReason` | məcburi |
| `comment` | string | ixtiyari, max 500 |

Response: `201`, `ReportResponse`
```json
{
  "id": 9, "listingId": 101, "listingTitle": "BMW X5, 2018",
  "reporterId": 12, "reporterEmail": "user@mail.com",
  "reason": "FAKE", "comment": "Şəkillər başqa maşındandır",
  "status": "OPEN", "createdAt": "2026-10-31T10:00:00Z"
}
```
Xətalar: `409 DUPLICATE_RESOURCE` (eyni elana təkrar), `409 SELF_ACTION` (öz elanına), `404` (elan yoxdur).
Elana **5 açıq şikayət** toplananda elan avtomatik `PENDING` olur və moderasiyaya düşür.

---

## 11. VIP: `/api/vip`

| Metod | Yol | İcazə | Request | Response |
|---|---|---|---|---|
| GET | `/api/vip/packages` | Public | n/a | `VipPackageResponse[]` |
| POST | `/api/vip/buy` | Auth (elan sahibi) | `BuyVipRequest` | `201 VipPurchaseResponse` |
| GET | `/api/vip/my` | Auth | n/a | `VipPurchaseResponse[]` |

`VipPackageResponse`:
```json
{ "id": 1, "name": "7 günlük VIP", "durationDays": 7, "price": 5.00 }
```
`BuyVipRequest`: `listingId` (number, məcburi), `packageId` (number, məcburi)

`VipPurchaseResponse`:
```json
{
  "id": 4, "listingId": 101, "listingTitle": "BMW X5, 2018",
  "package": { "id": 1, "name": "7 günlük VIP", "durationDays": 7, "price": 5.00 },
  "startsAt": "2026-10-31T10:00:00Z", "endsAt": "2026-11-07T10:00:00Z", "active": true
}
```
- Ödəniş **simulyasiyadır** (real ödəniş sistemi yoxdur), sorğu uğurlu olarsa VIP aktivləşir.
- Yalnız `ACTIVE` elan üçün VIP alına bilər, əks halda `409 INVALID_STATUS_TRANSITION`.
- Elan artıq VIP-dirsə yeni müddət **mövcud `vipUntil`-ın üzərinə əlavə olunur**.
- VIP vaxtı scheduler tərəfindən bitirilir.

---

## 12. Admin və moderasiya: `/api/admin`

### 12.1 Elan moderasiyası (MOD)

| Metod | Yol | Request | Response |
|---|---|---|---|
| GET | `/api/admin/listings?status=PENDING&page=0&size=20` | n/a | `PageResponse<ListingSummaryResponse>` |
| GET | `/api/admin/listings/{id}` | n/a | `ListingResponse` (istənilən status) |
| POST | `/api/admin/listings/{id}/review` | `ListingReviewRequest` | `ListingResponse` |
| POST | `/api/admin/listings/{id}/hide` | `HideListingRequest` | `ListingResponse` |

- `status` verilməzsə default `PENDING`. Ən köhnə gözləyən birinci gəlir.
- **`ListingReviewRequest`**: `approve` (boolean, məcburi), `rejectReason` (string, `approve = false` olanda **məcburi**, 5–300 simvol)
  - `approve = true`: `PENDING → ACTIVE`, `expiresAt = indi + 30 gün`
  - `approve = false`: `PENDING → REJECTED`, `rejectReason` yazılır
  - Elan `PENDING` deyilsə: `409 INVALID_STATUS_TRANSITION`
- **`HideListingRequest`**: `reason` (string, məcburi, 5–300). `ACTIVE → DEACTIVATED`, səbəb audit loga yazılır.

### 12.2 Şikayətlər (MOD)

| Metod | Yol | Request | Response |
|---|---|---|---|
| GET | `/api/admin/reports?status=OPEN&page=0&size=20` | n/a | `PageResponse<ReportResponse>` |
| PATCH | `/api/admin/reports/{id}/resolve` | n/a | `ReportResponse` (`RESOLVED`) |
| PATCH | `/api/admin/reports/{id}/dismiss` | n/a | `ReportResponse` (`DISMISSED`) |

### 12.3 İstifadəçilər (ADMIN)

| Metod | Yol | Response |
|---|---|---|
| GET | `/api/admin/users?q=&blocked=&role=&page=0&size=20` | `PageResponse<UserResponse>` |
| PATCH | `/api/admin/users/{id}/block` | `UserResponse` |
| PATCH | `/api/admin/users/{id}/unblock` | `UserResponse` |

- `q`: email və ya ada görə axtarış (ixtiyari), `blocked`: `true/false`, `role`: `Role`.
- Bloklananda onun bütün elanları saytdan gizlənir. Blokdan çıxarılanda geri görünür (statuslar dəyişmir).
- ADMIN öz hesabını blok edə bilməz: `409 SELF_ACTION`.

### 12.4 Statistika (ADMIN): `GET /api/admin/statistics`
`StatisticsResponse`:
```json
{
  "totalUsers": 1200,
  "blockedUsers": 14,
  "newUsersLast7Days": 63,
  "totalListings": 4300,
  "newListingsLast7Days": 210,
  "pendingListings": 18,
  "openReports": 5,
  "activeVipListings": 40,
  "listingsByStatus": { "PENDING": 18, "ACTIVE": 3100, "REJECTED": 90, "EXPIRED": 800, "SOLD": 250, "DEACTIVATED": 42 },
  "topBrands": [ { "brandName": "Mercedes", "count": 520 }, { "brandName": "BMW", "count": 480 } ]
}
```
`topBrands`: ən çox elanı olan 5 marka.

### 12.5 Audit loglar (ADMIN): `GET /api/admin/audit-logs`
Query: `userEmail`, `action`, `entityType` (hamısı ixtiyari), `page`, `size`.
Response: `PageResponse<AuditLogResponse>`
```json
{
  "id": 501, "userEmail": "mod@mail.com", "action": "LISTING_REJECTED",
  "entityType": "Listing", "entityId": 101,
  "details": "Səbəb: şəkillər keyfiyyətsizdir", "createdAt": "2026-10-31T10:00:00Z"
}
```
Audit loga yazılan `action` dəyərləri: `USER_REGISTERED`, `USER_BLOCKED`, `USER_UNBLOCKED`, `LISTING_CREATED`, `LISTING_UPDATED`, `LISTING_DELETED`, `LISTING_APPROVED`, `LISTING_REJECTED`, `LISTING_HIDDEN`, `LISTING_SOLD`, `VIP_PURCHASED`, `REPORT_RESOLVED`, `REPORT_DISMISSED`, `CATALOG_CHANGED`.

---

## 13. Bütün endpoint-lərin xülasə cədvəli

| # | Metod | Yol | İcazə |
|---|---|---|---|
| 1 | POST | `/api/auth/register` | Public |
| 2 | POST | `/api/auth/login` | Public |
| 3 | GET | `/api/users/me` | Auth |
| 4 | GET | `/api/users/me/profile` | Auth |
| 5 | PUT | `/api/users/me/profile` | Auth |
| 6 | POST | `/api/users/me/avatar` | Auth |
| 7 | PUT | `/api/users/me/password` | Auth |
| 8 | GET | `/api/brands` | Public |
| 9 | GET | `/api/brands/{brandId}/models` | Public |
| 10 | GET | `/api/cities` | Public |
| 11 | GET | `/api/listings` | Public |
| 12 | GET | `/api/listings/{id}` | Public |
| 13 | POST | `/api/listings` | Auth |
| 14 | PUT | `/api/listings/{id}` | Owner |
| 15 | DELETE | `/api/listings/{id}` | Owner |
| 16 | PATCH | `/api/listings/{id}/deactivate` | Owner |
| 17 | PATCH | `/api/listings/{id}/activate` | Owner |
| 18 | PATCH | `/api/listings/{id}/sold` | Owner |
| 19 | PATCH | `/api/listings/{id}/renew` | Owner |
| 20 | POST | `/api/listings/{id}/images` | Owner |
| 21 | DELETE | `/api/listings/{id}/images/{imageId}` | Owner |
| 22 | PUT | `/api/listings/{id}/images/order` | Owner |
| 23 | GET | `/api/my/listings` | Auth |
| 24 | GET | `/api/my/listings/counts` | Auth |
| 25 | GET | `/api/favorites` | Auth |
| 26 | POST | `/api/favorites/{listingId}` | Auth |
| 27 | DELETE | `/api/favorites/{listingId}` | Auth |
| 28 | POST | `/api/conversations` | Auth |
| 29 | GET | `/api/conversations` | Auth |
| 30 | GET | `/api/conversations/{id}/messages` | Tərəf |
| 31 | POST | `/api/conversations/{id}/messages` | Tərəf |
| 32 | GET | `/api/conversations/unread-count` | Auth |
| 33 | POST | `/api/reports` | Auth |
| 34 | GET | `/api/vip/packages` | Public |
| 35 | POST | `/api/vip/buy` | Auth |
| 36 | GET | `/api/vip/my` | Auth |
| 37 | GET | `/api/admin/listings` | MOD |
| 38 | GET | `/api/admin/listings/{id}` | MOD |
| 39 | POST | `/api/admin/listings/{id}/review` | MOD |
| 40 | POST | `/api/admin/listings/{id}/hide` | MOD |
| 41 | GET | `/api/admin/reports` | MOD |
| 42 | PATCH | `/api/admin/reports/{id}/resolve` | MOD |
| 43 | PATCH | `/api/admin/reports/{id}/dismiss` | MOD |
| 44 | GET | `/api/admin/users` | ADMIN |
| 45 | PATCH | `/api/admin/users/{id}/block` | ADMIN |
| 46 | PATCH | `/api/admin/users/{id}/unblock` | ADMIN |
| 47 | GET | `/api/admin/statistics` | ADMIN |
| 48 | GET | `/api/admin/audit-logs` | ADMIN |
| 49 | POST/PUT/DELETE | `/api/admin/brands`, `/api/admin/brands/{id}`, `/api/admin/brands/{id}/logo` | ADMIN |
| 50 | POST/PUT/DELETE | `/api/admin/models`, `/api/admin/models/{id}` | ADMIN |
| 51 | POST/PUT/DELETE | `/api/admin/cities`, `/api/admin/cities/{id}` | ADMIN |

---

## 14. Frontend səhifələri və istifadə etdikləri endpoint-lər

| Səhifə | Marşrut | Endpoint-lər |
|---|---|---|
| Ana səhifə / axtarış | `/` | 11, 8, 9, 10 (filtr paneli üçün marka, model, şəhər) |
| Elan səhifəsi | `/listings/:id` | 12, 26/27 (sevimli), 28 (mesaj yaz), 33 (şikayət) |
| Qeydiyyat | `/register` | 1 |
| Giriş | `/login` | 2 |
| Elan yerləşdir | `/listings/new` | 8, 9, 10, 13 |
| Elanı redaktə et | `/listings/:id/edit` | 12, 14, 20, 21, 22 |
| Mənim elanlarım | `/my/listings` | 23, 24, 15, 16, 17, 18, 19, 35 (VIP) |
| Sevimlilər | `/favorites` | 25, 27 |
| Mesajlar | `/messages`, `/messages/:id` | 29, 30, 31, 32 |
| Profil | `/profile` | 3, 4, 5, 6, 7 |
| Moderator paneli | `/admin/moderation` | 37, 38, 39, 40, 41, 42, 43 |
| Admin paneli | `/admin` | 44, 45, 46, 47, 48, 49, 50, 51 |

### Frontend üçün vacib qeydlər
1. Token `localStorage`-də saxlanır, hər sorğuya `Authorization: Bearer ...` əlavə olunur. Bütün `401` cavablarında token silinir və `/login`-ə yönləndirilir.
2. Marka/model/şəhər siyahıları nadir dəyişir, frontend onları da cache edə bilər.
3. Model dropdown-u marka seçiləndən sonra `GET /api/brands/{brandId}/models` ilə yüklənir. Marka dəyişəndə model sıfırlanır.
4. Rolu `user.role` sahəsindən oxuyub menyu göstərilir (MOD paneli yalnız `MODERATOR` və `ADMIN` üçün, admin paneli yalnız `ADMIN` üçün). Əsl icazə həmişə backend-də yoxlanılır.
5. Forma xətalarını `fieldErrors[].field` ilə uyğun inputun altında göstərmək lazımdır.
6. Qiymət göstərərkən valyuta simvolu: `AZN → ₼`, `USD → $`, `EUR → €`.
7. Tarixlər UTC gəlir, `Asia/Baku` (UTC+4) vaxtına çevrilərək göstərilir.
8. Şəkil yükləyərkən `Content-Type` header-i əl ilə yazılmır, brauzer `boundary` ilə özü qoyur.

---

## 15. Backend üçün əlavələr (əvvəlki struktura nisbətən)

Bu sənəddəki API-nin işləməsi üçün əvvəlki struktura aşağıdakılar əlavə olunmalıdır:

**DTO (request):** `CityRequest`, `StartConversationRequest`, `ReorderImagesRequest`, `HideListingRequest`
**DTO (response):** `CityResponse`, `IdNameResponse`, `SellerResponse`, `VipPurchaseResponse`, `AuditLogResponse`, `ListingCountsResponse` (və ya `Map<ListingStatus, Long>`), `UnreadCountResponse`
**Enum xəta kodları:** `ErrorResponse`-da `code` sahəsi (bölmə 0-dakı cədvəl)
**Query parametrləri:** `ListingFilterRequest`-də `currency`, `driveType`, `sort` sahələri
**Entity:** `Listing`-də `contactPhone`, `rejectReason` sahələri; `Report`-da `comment` sahəsi
**Controller:** `CatalogController`-ə admin CRUD endpoint-ləri, `AdminController`-ə users, reports, statistics, audit-logs
