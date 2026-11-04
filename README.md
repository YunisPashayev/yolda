# Yolda: Avtomobil elanları platforması (Backend)

Turbo.az tipli avtomobil elanları saytının sadələşdirilmiş backend-i (REST API, frontend ayrıca hazırlanır). İstifadəçi qeydiyyatdan keçir, avtomobil elanı yerləşdirir və şəkillər yükləyir. Elan moderator təsdiqləyənə qədər saytda görünmür. Digər istifadəçilər elanları axtarır, filtrləyir, sevimlilərə əlavə edir və satıcıya mesaj yazır. Moderator elanları və şikayətləri idarə edir.

**Layihənin əsas ideyası:** elanın bütün həyat dövrü (yaranması, təsdiqi, vaxtının bitməsi, satılması) düzgün idarə olunmalıdır və hər kəs yalnız öz məlumatını dəyişə bilməlidir.

---

## Nə öyrənəcəksiniz?

- Spring Boot layihəsini sıfırdan qat-qat (controller → service → repository) qurmağı
- JWT ilə autentifikasiyanı və rola görə icazələri (401 və 403 fərqini)
- JPA əlaqələrini (OneToMany, ManyToOne, ManyToMany) düzgün yazmağı
- Çoxlu filtrlə dinamik axtarış, sıralama və səhifələməni (Specification, Pageable)
- Çoxlu şəkil yükləməni (Cloudinary)
- Scheduler ilə avtomatik işləri (elanın müddətinin bitməsi, xatırlatma)
- Cache istifadəsini
- Xətaların bir yerdən idarə olunmasını
- Layihəni Docker ilə işə salmağı

## Texnologiyalar

| Sahə | Texnologiya |
|---|---|
| Dil / Framework | Java 21, Spring Boot 3.x, Maven |
| Verilənlər bazası | PostgreSQL 16 |
| Təhlükəsizlik | Spring Security, JWT |
| ORM | Spring Data JPA (Hibernate), Specification |
| Mapping | MapStruct, Lombok |
| Validasiya | Jakarta Validation + xüsusi validator-lar |
| Fayl saxlama | Cloudinary |
| Email | JavaMailSender |
| Cache | Spring Cache (ConcurrentMapCacheManager) |
| Sənədləşmə | springdoc-openapi (Swagger UI) |
| Test | JUnit 5, Mockito, MockMvc |
| Deploy | Docker, Docker Compose |

---

## Rollar

- **USER:** elan yaradır, yalnız öz elanlarını idarə edir, sevimlilərə əlavə edir, mesaj yazır, şikayət göndərir
- **MODERATOR:** gözləyən elanları təsdiq/rədd edir, şikayətlərə baxır, qaydanı pozan elanı gizlədir
- **ADMIN:** hər şeyə çıxışı var, istifadəçiləri bloklayır, kataloqu (marka, model, şəhər) idarə edir, audit loqlarına və statistikaya baxır

Başqasının elanını dəyişməyə çalışan istifadəçi `403` və ya `404` almalıdır. Bu hər endpoint-də yoxlanmalıdır.

## Əsas axın

1. **Qeydiyyat və login:** istifadəçi qeydiyyatdan keçir, JWT token alır.
2. **Profil:** ad, telefon, şəhər, profil şəkli.
3. **Elan yaratma:** marka, model, il, qiymət, yürüş, mühərrik, qutu, kuzov, rəng, təsvir və şəkillər daxil edilir. Elan `PENDING` statusu ilə yaranır.
4. **Moderasiya:** moderator elanı təsdiq (`ACTIVE`) və ya rədd (`REJECTED` + səbəb) edir.
5. **Axtarış:** alıcı filtr və sıralama ilə yalnız aktiv elanlara baxır, elana daxil olduqda baxış sayı artır.
6. **Sevimlilər və mesaj:** alıcı elanı sevimliyə əlavə edir, satıcıya mesaj yazır.
7. **Elanın sonu:** elan 30 gün sonra avtomatik bitir (`EXPIRED`), satıcı onu yeniləyə və ya `SOLD` edə bilər.
8. **VIP:** satıcı elanı müddətli olaraq önə çıxarır.

### Elan statusları

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
REJECTED ──edit────> PENDING
```
Bunlardan kənar hər keçid `409 INVALID_STATUS_TRANSITION` qaytarır (məsələn `REJECTED → ACTIVE` birbaşa olmaz).

---

## Əsas biznes qaydaları (service qatında yazılır)

**Elan**
- Elan yaratmaq üçün ən azı 3, ən çox 15 şəkil lazımdır.
- Qiymət müsbət olmalı, il 1950 ilə cari il arasında olmalıdır.
- Model seçilən markaya aid olmalıdır.
- Bir istifadəçinin eyni anda ən çox 5 `ACTIVE` elanı ola bilər.
- İstifadəçi yalnız öz elanını redaktə edə, silə və ya deaktiv edə bilər.
- `ACTIVE` elan redaktə olunarsa yenidən `PENDING` olur və moderasiyaya düşür.
- Bloklanmış istifadəçi elan yarada bilməz, onun elanları saytdan gizlənir.

**Axtarış**
- Yalnız `ACTIVE` elanlar görünür.
- VIP elanlar siyahıda həmişə yuxarıda çıxır, sonra seçilmiş sıralama tətbiq olunur.
- Bütün filtrlər ixtiyaridir və birlikdə işləməlidir.
- Səhifələmə məcburidir.

**Sevimlilər və mesaj**
- Eyni elan sevimlilərə iki dəfə əlavə olunmur.
- İstifadəçi öz elanına mesaj yaza və ya onu sevimliyə əlavə edə bilməz.
- Mesajı yalnız söhbətin iki tərəfi görür.

**Şikayət**
- Bir istifadəçi eyni elana yalnız bir dəfə şikayət edə bilər.
- Elana 5 şikayət gələndə elan avtomatik moderasiyaya qaytarılır.

**VIP və scheduler**
- VIP-in bitmə vaxtı olur, vaxtı bitən elan scheduler ilə adi elana çevrilir.
- Elanın bitməsinə 3 gün qalmış satıcıya email gedir.
- Vaxtı bitmiş elanlar scheduler ilə `EXPIRED` olur.
- Uzun müddət `PENDING` qalan elanlar moderatora xatırladılır.

---

## Layihə strukturu

```
az.turboclone
│
├── TurboCloneApplication
│
├── config
│   ├── SecurityConfig              # FilterChain, PasswordEncoder, AuthManager
│   ├── CorsConfig
│   ├── CacheConfig                 # marka/model/şəhər siyahıları üçün
│   ├── CloudinaryConfig
│   ├── SchedulingConfig            # @EnableScheduling
│   ├── SwaggerConfig               # API sənədi
│   ├── JacksonConfig               # tarix formatı
│   └── DataInitializer             # admin/moderator hesabı və seed data
│
├── security
│   ├── JwtService
│   ├── JwtAuthenticationFilter
│   ├── CustomUserDetails
│   ├── CustomUserDetailsService
│   ├── CustomAuthenticationEntryPoint    # 401
│   └── CustomAccessDeniedHandler         # 403
│
├── entity
│   ├── BaseEntity                  # id, createdAt, updatedAt
│   ├── User                        # email, passwordHash, role, enabled, blocked
│   ├── UserProfile                 # fullName, phone, city, avatarUrl
│   ├── Brand                       # name, logoUrl
│   ├── CarModel                    # name, brand (ManyToOne)
│   ├── City                        # name
│   ├── Listing                     # price, currency, year, mileage, engineVolume, horsepower,
│   │                               # fuelType, transmission, bodyType, driveType, color,
│   │                               # description, contactPhone, status, rejectReason,
│   │                               # expiresAt, expiryReminderSent, viewCount, vipUntil
│   ├── ListingImage                # url, publicId, position, listing
│   ├── Favorite                    # user, listing (unikal cüt)
│   ├── Conversation                # listing, buyer, seller
│   ├── Message                     # conversation, sender, text, readAt
│   ├── Report                      # listing, reporter, reason, comment, status
│   ├── VipPackage                  # name, durationDays, price
│   ├── VipPurchase                 # listing, package, startsAt, endsAt
│   └── AuditLog                    # userEmail, action, entityType, entityId, details
│
├── enums
│   ├── Role                        # USER, MODERATOR, ADMIN
│   ├── ListingStatus               # PENDING, ACTIVE, REJECTED, EXPIRED, SOLD, DEACTIVATED
│   ├── Currency                    # AZN, USD, EUR
│   ├── FuelType                    # PETROL, DIESEL, GAS, HYBRID, ELECTRIC
│   ├── TransmissionType            # MANUAL, AUTOMATIC, SEMI_AUTOMATIC
│   ├── BodyType                    # SEDAN, HATCHBACK, SUV, COUPE, WAGON, MINIVAN, PICKUP
│   ├── DriveType                   # FRONT, REAR, ALL
│   ├── ReportReason                # SPAM, FAKE, WRONG_INFO, SOLD, OTHER
│   └── ReportStatus                # OPEN, RESOLVED, DISMISSED
│
├── repository                      # hər entity üçün bir Repository
│
├── specification
│   └── ListingSpecification        # dinamik filtr
│
├── dto
│   ├── request                     # RegisterRequest, LoginRequest, CreateListingRequest, ...
│   └── response                    # AuthResponse, ListingResponse, PageResponse<T>, ErrorResponse, ...
│
├── mapper                          # MapStruct (@Mapper(componentModel = "spring"))
│
├── service
│   ├── AuthService
│   ├── UserService
│   ├── CatalogService              # @Cacheable
│   ├── ListingService
│   ├── ListingSearchService
│   ├── ListingModerationService
│   ├── FavoriteService
│   ├── MessageService
│   ├── ReportService
│   ├── VipService
│   ├── StatisticsService
│   ├── AuditService
│   ├── EmailService                # JavaMailSender
│   └── FileStorageService          # Cloudinary upload/delete
│
├── controller
│   ├── AuthController              # /api/auth
│   ├── UserController              # /api/users
│   ├── CatalogController           # /api/brands, /api/cities (+ admin CRUD)
│   ├── ListingController           # /api/listings
│   ├── MyListingController         # /api/my/listings
│   ├── FavoriteController          # /api/favorites
│   ├── MessageController           # /api/conversations
│   ├── ReportController            # /api/reports
│   ├── VipController               # /api/vip
│   └── AdminController             # /api/admin (MODERATOR/ADMIN)
│
├── scheduler
│   ├── ListingExpirationScheduler  # EXPIRED + 3 gün qalmış xatırlatma
│   ├── VipExpirationScheduler
│   └── PendingListingScheduler
│
├── exception                       # GlobalExceptionHandler + xüsusi exception-lar
├── validation                      # ValidPhone, ValidYear, ValidImage
└── util                            # SecurityUtil, FileUtil, PriceUtil, SlugUtil
```

Tam siyahı (bütün DTO, mapper və exception adları) layihənin əvvəlki struktur sənədindədir. Yazarkən `docs/API_DOCUMENTATION.md`-dəki sahə adları ilə **tam eyni** adlardan istifadə edin.

---

## İşə salmaq

### Tələblər
Java 21, Maven, Docker və Docker Compose.

### 1. Verilənlər bazası
```bash
docker compose up -d db
```

### 2. Mühit dəyişənləri
`.env.example` faylını `.env` olaraq kopyalayın və doldurun. **`.env` faylı Git-ə əlavə olunmur.**

| Dəyişən | İzah |
|---|---|
| `DB_URL` | məsələn `jdbc:postgresql://localhost:5432/yolda` |
| `DB_USERNAME`, `DB_PASSWORD` | baza girişi |
| `JWT_SECRET` | ən azı 32 simvolluq gizli açar |
| `JWT_EXPIRATION` | token ömrü (saniyə), məsələn `86400` |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Cloudinary hesabı |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP (inkişaf üçün Mailtrap tövsiyə olunur) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | ilk ADMIN hesabı (proqram ilk dəfə qalxanda yaranır) |

### 3. Tətbiqi işə salmaq
```bash
./mvnw spring-boot:run
```
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4. Hamısını Docker ilə
```bash
docker compose up --build
```

### Testlər
```bash
./mvnw test
```

---

## Sənədlər

| Fayl | Nə üçündür |
|---|---|
| [`docs/API_DOCUMENTATION.md`](docs/API_DOCUMENTATION.md) | Bütün endpoint-lər, DTO-lar, enum-lar, xəta formatı. **Frontend ilə razılaşma sənədidir, ona tam əməl olunmalıdır.** |
| [`tasks/`](tasks) | Faza tapşırıqları (hər tapşırıq GitHub Issue-ya çevrilir) |

## Fazalar

| Faza | Müddət | Tapşırıqlar |
|---|---|---|
| 1. Hazırlıq | 3–4 gün | [`tasks/PHASE-1-HAZIRLIQ.md`](tasks/PHASE-1-HAZIRLIQ.md) |
| 2. Auth | 1 həftə | [`tasks/PHASE-2-AUTH.md`](tasks/PHASE-2-AUTH.md) |
| 3. Kataloq | 3–4 gün | [`tasks/PHASE-3-KATALOQ.md`](tasks/PHASE-3-KATALOQ.md) |
| 4. Elanlar | 1,5–2 həftə | [`tasks/PHASE-4-ELANLAR.md`](tasks/PHASE-4-ELANLAR.md) |
| 5. Axtarış və moderasiya | 1 həftə | [`tasks/PHASE-5-AXTARIS-MODERASIYA.md`](tasks/PHASE-5-AXTARIS-MODERASIYA.md) |
| 6. Sevimlilər, mesaj, şikayət | 1 həftə | [`tasks/PHASE-6-SEVIMLILER-MESAJ-SIKAYET.md`](tasks/PHASE-6-SEVIMLILER-MESAJ-SIKAYET.md) |
| 7. Scheduler, email, VIP, admin | 1 həftə | [`tasks/PHASE-7-SCHEDULER-VIP-ADMIN.md`](tasks/PHASE-7-SCHEDULER-VIP-ADMIN.md) |
| 8. Keyfiyyət və təqdimat | 1 həftə | [`tasks/PHASE-8-KEYFIYYET.md`](tasks/PHASE-8-KEYFIYYET.md) |

## İş qaydası

1. Tapşırıq sizə **Issue** kimi təyin olunur. Qəbul meyarlarını (checklist) diqqətlə oxuyun.
2. Hər issue üçün `main`-dən ayrıca branch açın: `feature/<issue-nömrəsi>-qısa-ad`.
3. Mənalı commit mesajları yazın, işi bitirəndə Pull Request açın və issue-nu bağlayın.
4. Endpoint yazdıqdan sonra Swagger-dən və ya Postman-dən əl ilə yoxlayın: həm uğurlu, həm xəta hallarını (400, 401, 403, 404, 409).
