# HerSafe - Technical Reference Guide (AI Agent Edition)

> **هام جداً للـ AI Agent:** هذا الملف هو المرجع الأساسي لفهم المشروع. يجب تحديثه عند كل تعديل جوهري.

---

## 📋 جدول المحتويات
1. [نظرة عامة على المشروع](#overview)
2. [التقنيات المستخدمة](#technologies)
3. [بنية الباك إند (Cloudflare Worker)](#backend-architecture)
4. [API Endpoints](#api-endpoints)
5. [قاعدة البيانات (D1)](#database-schema)
6. [تطبيق الأندرويد](#android-app)
7. [عملية النشر (Deployment)](#deployment)
8. [المشكلات الشائعة وحلولها](#common-issues)
9. [إرشادات للـ AI Agent](#ai-agent-guidelines)

---

## 🎯 نظرة عامة على المشروع {#overview}

**HerSafe** هو تطبيق أمان للنساء يعمل على نظام Android، يتضمن:
- **زر SOS للطوارئ:** إرسال رسائل SMS + الموقع لجهات الاتصال.
- **رحلة آمنة (Safe Journey):** تتبع مباشر للموقع مع إشعارات.
- **كاميرا سرية (Spy Camera):** تسجيل فيديو خفي ورفعه لبوت تلغرام.
- **لوحة تحكم ويب (Admin Dashboard):** عرض البلاغات على خريطة تفاعلية.

---

## 🛠️ التقنيات المستخدمة {#technologies}

### تطبيق Android
| التقنية | الاستخدام |
|---------|-----------|
| **Java** | لغة البرمجة الأساسية |
| **XML** | تصميم الواجهات |
| **Room** | قاعدة بيانات محلية (SQLite) |
| **Retrofit** | طلبات HTTP للـ API |
| **CameraX** | تسجيل الفيديو الخفي |
| **Telegram Bot API** | رفع الفيديوهات |
| **Google Maps API** | الخرائط والاتجاهات |

### الباك إند (Backend)
| التقنية | الاستخدام |
|---------|-----------|
| **Cloudflare Workers** | Serverless Runtime |
| **JavaScript (ES Modules)** | لغة البرمجة |
| **Cloudflare D1** | قاعدة بيانات SQLite سحابية |
| **Wrangler CLI** | أداة النشر |

---

## 🏗️ بنية الباك إند (Cloudflare Worker) {#backend-architecture}

### الملفات الأساسية
```
hersafe-backend/
├── src/
│   └── index.js          # ← الملف الرئيسي (كل الـ API هنا)
├── wrangler.toml         # ← إعدادات Cloudflare
├── schema.sql            # ← DDL لقاعدة البيانات (مرجعي فقط)
├── seed.sql              # ← بيانات اختبار (مرجعي فقط)
└── package.json          # ← تبعيات Node.js
```

### ملف `wrangler.toml` (الإعدادات)
```toml
name = "hersafe-backend"
main = "src/index.js"
compatibility_date = "2024-01-30"
account_id = "129a2bf74612ba7e7b38e498017a17ce"

[[d1_databases]]
binding = "DB"                              # ← اسم المتغير في الكود
database_name = "hersafe-db"
database_id = "5b98ba6e-7722-4159-aaa1-cf5e88355dda"
```

### كيف يعمل `index.js`
```javascript
export default {
    async fetch(request, env, ctx) {
        // env.DB ← هذا هو الاتصال بقاعدة البيانات D1
        // request.method ← GET, POST, etc.
        // url.pathname ← المسار مثل /api/report
        
        // Router بسيط (if/else على المسارات)
        if (path === "/api/report" && request.method === "POST") {
            return handleReport(request, env, corsHeaders);
        }
        // ...
    }
}
```

---

## 🔌 API Endpoints {#api-endpoints}

### الرابط الأساسي
```
https://hersafe-backend.zenabhamsho3.workers.dev/
```

### قائمة الـ Endpoints

| Method | Path | الوصف | المصادقة |
|--------|------|-------|----------|
| `GET` | `/` | لوحة التحكم (HTML) أو صفحة الدخول | Cookie |
| `POST` | `/login` | تسجيل الدخول للوحة التحكم | ❌ |
| `POST` | `/auth/setup` | إعادة تهيئة قاعدة البيانات (يحذف الجداول!) | ❌ |
| `POST` | `/auth/register` | تسجيل مستخدم جديد | ❌ |
| `POST` | `/api/report` | إرسال بلاغ جديد (من التطبيق) | ❌ |
| `GET` | `/api/public/alerts` | جلب كل البلاغات (للخريطة) | ❌ |
| `POST` | `/api/admin/seed` | توليد بيانات اختبارية | ❌ |
| `GET` | `/api/data` | بيانات الإدارة | Cookie |

### تفاصيل الـ Endpoints المهمة

#### `POST /api/report`
```json
// Request Body
{
    "phone": "+963912345678",
    "lat": 33.5138,
    "lng": 36.2765,
    "video_id": "12345"   // ← رقم رسالة التلغرام (اختياري)
}

// Response
{ "success": true }
```

#### `GET /api/public/alerts`
```json
// Response (Array)
[
    {
        "id": 1,
        "latitude": 35.135,
        "longitude": 36.758,
        "governorate": "حماه",
        "area_detail": "حماه - الحاضر",
        "telegram_video_id": "SEED_12345",
        "phone_number": "+963912345678",
        "created_at": "2026-02-05 10:30:00"
    }
]
```

#### `POST /api/admin/seed`
يقوم بتوليد 200 بلاغ وهمي بمواقع سورية حقيقية (حماه، دمشق، حلب...).
```json
// Response
{ "success": true, "count": 200, "message": "Seeded with real location names" }
```

---

## 🗄️ قاعدة البيانات (D1) {#database-schema}

### جدول `alerts` (البلاغات)
| Column | Type | الوصف |
|--------|------|-------|
| `id` | INTEGER | مفتاح أساسي (Auto) |
| `phone_number` | TEXT | رقم هاتف المُبلِّغ |
| `latitude` | REAL | خط العرض |
| `longitude` | REAL | خط الطول |
| `governorate` | TEXT | اسم المحافظة (بالعربي) |
| `area_detail` | TEXT | اسم الحي/المنطقة (بالعربي) |
| `telegram_video_id` | TEXT | رقم رسالة الفيديو في التلغرام |
| `created_at` | DATETIME | وقت الإنشاء |

### جدول `users`
| Column | Type | الوصف |
|--------|------|-------|
| `id` | INTEGER | مفتاح أساسي |
| `name` | TEXT | الاسم |
| `email` | TEXT | البريد (فريد) |
| `phone` | TEXT | رقم الهاتف |
| `password_hash` | TEXT | كلمة المرور المشفرة |
| `api_token` | TEXT | توكن الوصول |

### جدول `emergency_contacts`
| Column | Type | الوصف |
|--------|------|-------|
| `id` | INTEGER | مفتاح أساسي |
| `user_id` | INTEGER | FK → users.id |
| `name` | TEXT | اسم جهة الاتصال |
| `phone` | TEXT | رقم الهاتف |

---

## 📱 تطبيق الأندرويد {#android-app}

### الملفات الأساسية للتكامل مع الباك إند

```
app/src/main/java/com/example/hersafe/
├── data/
│   ├── remote/
│   │   ├── RetrofitClient.java       # ← إعداد Retrofit مع الـ Base URL
│   │   ├── ReportsApiService.java    # ← تعريف endpoints البلاغات
│   │   ├── TelegramApiService.java   # ← رفع الفيديو للتلغرام
│   │   └── models/
│   │       ├── Alert.java            # ← موديل البلاغ
│   │       └── ReportRequest.java    # ← موديل طلب البلاغ
│   └── preferences/
│       └── SessionManager.java       # ← تخزين Token + Chat ID
├── service/
│   └── VideoRecordingService.java    # ← تسجيل + رفع الفيديو
```

### تدفق رفع الفيديو والبلاغ
```
1. المستخدم يضغط SOS أو 3 نقرات على زر الصوت
                    ↓
2. VideoRecordingService يبدأ التسجيل
                    ↓
3. عند انتهاء كل مقطع (60 ثانية):
   - يُرفع للتلغرام عبر TelegramApiService
   - يُستخرج message_id من الرد
                    ↓
4. يُرسل البلاغ للباك إند عبر ReportsApiService.reportAlert()
   مع: phone, lat, lng, video_id
                    ↓
5. يظهر البلاغ في لوحة التحكم على الخريطة
```

### إعداد Telegram Bot
```java
// في SessionManager.java - القيم الافتراضية:
private static final String DEFAULT_BOT_TOKEN = "8012345678:AAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
private static final String DEFAULT_CHAT_ID = "5244567403";
```

---

## 🚀 عملية النشر (Deployment) {#deployment}

### متطلبات النشر
1. **Node.js** مثبت على الجهاز
2. **Wrangler CLI** (يُثبت تلقائياً عبر npx)
3. **حساب Cloudflare** مع تسجيل الدخول

### أوامر النشر (من مجلد `hersafe-backend`)

```powershell
# 1. النشر للسيرفر
npx wrangler deploy

# 2. إعادة تهيئة قاعدة البيانات (يحذف كل البيانات!)
curl -X POST https://hersafe-backend.zenabhamsho3.workers.dev/auth/setup

# 3. توليد بيانات اختبارية
curl -X POST https://hersafe-backend.zenabhamsho3.workers.dev/api/admin/seed
```

### النشر على GitHub

```powershell
# من المجلد الرئيسي للمشروع
cd c:\Users\MCC\AndroidStudioProjects\hersafe

# إضافة التغييرات
git add .

# رسالة Commit
git commit -m "وصف التعديلات"

# الرفع للريبو
git push origin main
```

### مشكلة PowerShell مع npx
إذا ظهر خطأ `scripts is disabled`:
```powershell
# استخدم cmd بدلاً من PowerShell:
cmd /c npx wrangler deploy
```

---

## ⚠️ المشكلات الشائعة وحلولها {#common-issues}

### 1. خطأ `cannot find symbol: ResponseBody`
**السبب:** نسيان import في ملف Java.
**الحل:**
```java
import okhttp3.ResponseBody;
```

### 2. خطأ `resource color/dark_purple not found`
**السبب:** استخدام لون غير معرف في XML.
**الحل:** إضافة اللون في `res/values/colors.xml`:
```xml
<color name="dark_purple">#764053</color>
<color name="lavender_light">#E1BEE7</color>
```

### 3. خطأ `RetrofitClient.getInstance() not found`
**السبب:** استخدام method غير موجود.
**الحل:** استخدم الـ static methods المتاحة:
```java
// ❌ خطأ
RetrofitClient.getInstance(context).create(...)

// ✅ صحيح
RetrofitClient.getReportsService()
RetrofitClient.getTelegramService(token)
```

### 4. خطأ `Telegram 404 Not Found`
**السبب:** Bot Token غير صحيح أو يحتوي مسافات.
**الحل:** التأكد من التنسيق:
```
Token Format: 1234567890:AAHxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 5. خطأ `CLOUDFLARE_API_TOKEN required`
**السبب:** تشغيل wrangler في بيئة غير تفاعلية.
**الحل:** تشغيل `npx wrangler login` أولاً أو استخدام Environment Variable.

### 6. البيانات لا تظهر في لوحة التحكم
**السبب:** Schema قديم أو البيانات فارغة.
**الحل:**
```powershell
# إعادة التهيئة
curl -X POST .../auth/setup
# ثم التوليد
curl -X POST .../api/admin/seed
```

---

## 🤖 إرشادات للـ AI Agent {#ai-agent-guidelines}

> **ملاحظة صارمة:** عند إجراء أي تعديل، يجب تحديث هذا الملف!

### قبل أي تعديل
1. ✅ اقرأ هذا الملف (`BACKEND_GUIDE.md`)
2. ✅ اقرأ `GEMINI.md` لفهم هيكل Android
3. ✅ تحقق من آخر Schema في `/auth/setup` بـ `index.js`

### عند تعديل الباك إند
1. عدّل `index.js`
2. انشر: `npx wrangler deploy`
3. إذا غيرت Schema:
   - حدّث قسم [قاعدة البيانات](#database-schema) هنا
   - نفذ `/auth/setup` ثم `/api/admin/seed`

### عند تعديل Android API
1. عدّل `ReportsApiService.java` أو أنشئ Service جديد
2. تأكد من وجود الـ imports المطلوبة
3. استخدم `RetrofitClient.getXxxService()` وليس `getInstance()`

### عند إضافة Resource جديد (لون/أيقونة)
1. أضف الألوان في `res/values/colors.xml`
2. أضف الأيقونات في `res/drawable/`
3. تأكد من عدم وجود أخطاء Resource Linking

### عند ظهور Compile Errors
1. اقرأ رسالة الخطأ بعناية
2. تحقق من الـ imports
3. تحقق من أسماء Methods/Classes

### الأوامر الأساسية للحفظ
```powershell
# نشر الباك إند
cmd /c npx wrangler deploy

# إعادة تهيئة DB
cmd /c curl -X POST https://hersafe-backend.zenabhamsho3.workers.dev/auth/setup

# توليد بيانات
cmd /c curl -X POST https://hersafe-backend.zenabhamsho3.workers.dev/api/admin/seed

# بناء Android
./gradlew build
```

---

## 📝 سجل التحديثات

| التاريخ | الوصف |
|---------|-------|
| 2026-02-05 | إنشاء الملف - توثيق كامل للباك إند والـ API |
| 2026-02-05 | إضافة `telegram_video_id` للـ Schema |
| 2026-02-05 | تعريب أسماء المناطق في السيدر |
| 2026-02-05 | إضافة لون الأيقونات الوردي للخريطة |

---

> **آخر تحديث:** 2026-02-05 15:05 (Damascus Time)
