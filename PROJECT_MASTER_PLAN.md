إليك **وثيقة التوجيه الرئيسية (Master Prompt)** بصيغة Markdown.

هذا الملف مصمم خصيصاً لنسخه وإرساله إلى **AI Agent** (مثل Cursor Composer أو GitHub Copilot Workspace). إنه مكتوب بأسلوب "الأوامر الهندسية" (Engineering Prompting) ليقوم الوكيل بفهم المشروع من الصفر، إعادة هيكلته، ثم البدء بالتنفيذ خطوة بخطوة.

انسخ ما يلي واحفظه في ملف باسم `PROJECT_MASTER_PLAN.md` في جذر مشروعك، أو انسخ النص وأرسله للشات.

***

```markdown
# 🛡️ HerSafe Project Master Plan & Architecture

**Role:** Senior Android Architect & Developer.
**Stack:** Java, XML, Room Database, Android SDK 34+.
**Goal:** Refactor, Organize, and Implement the "HerSafe" women's safety application based on a strictly defined architecture.

---

## 📅 Phase 1: Project Initialization & Context
**Description:** HerSafe is a safety app designed to provide immediate assistance to women in danger.
**Core Features:**
1.  **SOS Panic Button:** Sends SMS/Location & API Alert immediately.
2.  **Safe Journey:** Live location tracking.
3.  **Spy Camera:** Discreet evidence collection.
4.  **Emergency Contacts:** Local database of trusted contacts.
5.  **Main Dashboard:** Sidebar navigation (Drawer) + Quick Actions Grid.

**Current State:** The project exists but has disorganized naming conventions (e.g., `menu.java`, `activity_welcome3`) and mixed logic.
**Objective:** Refactor the entire file structure to Clean Architecture (MVVM where possible, or structured MVC) and implement features one by one.

---

## 📂 Phase 2: Refactored File Structure (The Blueprint)

**ACTION REQUIRED:** You must refactor the existing files to match THIS structure exactly. Delete old files after migration.

### 1. Java Package Structure (`com.example.hersafe`)
```text
com.example.hersafe
├── app
│   ├── MyApplication.java       (Global Context/Hilt Setup if needed)
│
├── data                         (Data Layer)
│   ├── local                    (Room Database)
│   │   ├── AppDatabase.java
│   │   ├── dao
│   │   │   ├── UserDao.java
│   │   │   ├── ContactDao.java
│   │   │   └── IncidentDao.java
│   │   └── entities
│   │       ├── User.java
│   │       ├── Contact.java
│   │       └── Incident.java
│   │
│   ├── remote                   (API / Retrofit)
│   │   ├── ApiClient.java
│   │   └── ApiService.java
│   │
│   └── preferences              (SharedPrefs for simple settings)
│       └── SessionManager.java
│
├── ui                           (Presentation Layer)
│   ├── splash
│   │   └── SplashActivity.java
│   │
│   ├── onboarding               (Welcome Screens)
│   │   ├── OnboardingActivity.java (Container)
│   │   └── fragments
│   │       ├── WelcomeStep1Fragment.java
│   │       ├── WelcomeStep2Fragment.java
│   │       └── WelcomeStep3Fragment.java
│   │
│   ├── auth                     (Authentication)
│   │   ├── LoginActivity.java
│   │   └── SignupActivity.java
│   │
│   ├── main                     (Main Dashboard)
│   │   ├── MainActivity.java    (Contains DrawerLayout & NavHost)
│   │   └── home
│   │       └── HomeFragment.java (The Dashboard Grid)
│   │
│   ├── features                 (Core Features)
│   │   ├── sos
│   │   │   └── SosAlertActivity.java
│   │   ├── journey
│   │   │   └── SafeJourneyActivity.java
│   │   ├── contacts
│   │   │   ├── ContactsActivity.java
│   │   │   └── AddContactDialog.java
│   │   └── history
│   │       ├── HistoryActivity.java
│   │       └── IncidentDetailActivity.java
│   │
│   └── profile
│       └── ProfileActivity.java
│
└── utils                        (Helpers)
    ├── Constants.java
    ├── PermissionsHelper.java
    └── SwipeGestureListener.java
```

### 2. XML Layout Naming Convention
You must rename layouts to match their Activity/Fragment:
*   `activity_splash.xml`
*   `activity_onboarding.xml`
*   `activity_login.xml`
*   `activity_main.xml` (DrawerLayout)
*   `fragment_home.xml` (Dashboard content)
*   `activity_sos_alert.xml`
*   `activity_safe_journey.xml`
*   `activity_contacts.xml`
*   `item_contact.xml` (RecyclerView Item)

---

## 💾 Phase 3: Database Schema

### A. Local Database (Room - SQLite)
The app must work **Offline-First**.

**1. Table: `emergency_contacts`**
| Column | Type | Notes |
| :--- | :--- | :--- |
| `id` | int | PK, AutoGenerate |
| `name` | String | |
| `phone` | String | |
| `relation`| String | |
| `is_synced`| boolean | True if uploaded to API |

**2. Table: `incidents_history`**
| Column | Type | Notes |
| :--- | :--- | :--- |
| `id` | int | PK |
| `type` | String | (SOS, Journey) |
| `timestamp`| long | |
| `status` | String | (Sent, Failed) |

### B. Global Database (Server - MySQL/Laravel)
*This is for reference to build the JSON models in Java.*
*   `users` (id, name, email, phone, token)
*   `contacts` (user_id, name, phone)
*   `alerts` (user_id, location_lat, location_lng, type, evidence_url)

---

## 📝 Phase 4: Implementation Plan (The Task List)

**Agent Instructions:** Execute these tasks sequentially. Do not skip steps.

### 🟢 Milestone 1: Refactoring & Setup
- [ ] **Task 1.1:** Analyze current project files.
- [ ] **Task 1.2:** Create the new package structure (`data`, `ui`, `utils`).
- [x] **Task 1.3:** Move and Rename existing Activities to the new structure (e.g., `mainmenu` -> `MainActivity`, `welcome` -> `OnboardingActivity`).
- [ ] **Task 1.4:** Update `AndroidManifest.xml` with new Activity paths and Permissions (SMS, Camera, Location).

### 🟡 Milestone 2: UI/UX Standardization
- [ ] **Task 2.1:** Standardize `colors.xml` (Purple/Pink Theme).
- [ ] **Task 2.2:** Ensure all XML layouts use `ConstraintLayout` as root.
- [ ] **Task 2.3:** Fix `MainActivity` (DrawerLayout) to include `HomeFragment` properly without crashing.
- [x] **Task 2.4:** Implement `SwipeGestureListener` correctly for Onboarding screens.

### 🔴 Milestone 3: Core Logic & Database
- [ ] **Task 3.1:** Implement **Room Database**:
    - Create Entities (`Contact`, `User`).
    - Create DAOs.
    - Create `AppDatabase` singleton.
- [ ] **Task 3.2:** Connect `ContactsActivity` to Room DB (Add/Delete contacts functionality).
- [ ] **Task 3.3:** Implement **SOS Logic**:
    - On button click -> Start 5s Countdown.
    - On Finish -> Send SMS to contacts from Room DB.

### 🔵 Milestone 4: Advanced Features
- [ ] **Task 4.1:** Implement **Safe Journey**:
    - Integration with Google Maps SDK.
    - UI state toggling (Setup Mode vs Tracking Mode).
- [ ] **Task 4.2:** Implement **Camera Feature**:
    - Handle Runtime Permissions.
    - Launch Camera Intent.

---

## 🚀 Execution Command
**Agent:** Please start with **Milestone 1**.
1. Review the current project files.
2. Refactor the file names and package structure as defined in "Phase 2".
3. Update the Manifest.
   **Confirm when Milestone 1 is complete before moving to Milestone 2.**
```

### كيف تستخدم هذا الملف؟
1.  انسخ الكود أعلاه كاملاً.
2.  أنشئ ملفاً جديداً في مشروعك اسمه `README_PLAN.md` أو `ARCHITECTURE.md` والصق الكود فيه.
3.  إذا كنت تستخدم **Cursor** أو **Copilot Chat**، افتح الشات واكتب:
    > "Read the PROJECT_MASTER_PLAN.md file I just created. I want you to start executing Milestone 1: Refactoring & Setup. Please analyze the current file structure and propose the moves."

هذا سيجعل الذكاء الاصطناعي يعمل كمهندس برمجيات حقيقي يتبع خطة صارمة بدلاً من تقديم حلول ترقيعية (Patching).