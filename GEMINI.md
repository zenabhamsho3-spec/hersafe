# HerSafe Project Context (GEMINI.md)

## Project Overview

This is the codebase for **HerSafe**, a women's safety application for Android. The project is currently undergoing a significant refactoring effort to migrate from a disorganized structure to a well-defined **Clean Architecture** (MVVM/MVC).

The `PROJECT_MASTER_PLAN.md` file is the primary source of truth for the project's architecture, features, and implementation plan.

### Core Features:

*   **SOS Panic Button:** Sends immediate SMS/Location alerts.
*   **Safe Journey:** Live location tracking.
*   **Spy Camera:** Discreet evidence collection.
*   **Emergency Contacts:** Management of trusted contacts in a local database.
*   **Main Dashboard:** A central navigation hub.

### Technology Stack:

*   **Language:** Java
*   **UI:** XML
*   **Database:** Room (for local, offline-first storage)
*   **Build System:** Gradle
*   **Android SDK:** Target SDK 36

## Building and Running

This is a standard Android project.

*   **Build the project:**
    ```shell
    ./gradlew build
    ```
*   **Install on a connected device/emulator:**
    ```shell
    ./gradlew installDebug
    ```
*   **Run tests:**
    ```shell
    ./gradlew test
    ```
*   **Running the app:** The application can be launched from an Android device or emulator after installation.

## Development Conventions

The `PROJECT_MASTER_PLAN.md` file outlines a strict set of development conventions that must be followed.

### Architecture

The project is being refactored into a Clean Architecture with three main packages:

*   `com.example.hersafe.data`: The data layer, containing the Room database, API clients, and shared preferences.
*   `com.example.hersafe.ui`: The presentation layer, containing all activities, fragments, and UI-related logic.
*   `com.example.hersafe.utils`: Utility classes and helpers.

### File Naming Conventions

*   **Activities:** Activities should be named according to their function, e.g., `SplashActivity`, `OnboardingActivity`.
*   **Layouts:** XML layout files should correspond to their associated Activity or Fragment, e.g., `activity_splash.xml` for `SplashActivity.java`.

### Database

*   **Technology:** Room is used for the local database.
*   **Design:** The application follows an "Offline-First" approach. All data is written to the local database before being synced with a remote server.
*   **Schema:** The database schema is defined in `PROJECT_MASTER_PLAN.md` and includes tables for `emergency_contacts` and `incidents_history`.

### UI and UX

*   **Layouts:** `ConstraintLayout` is to be used as the root element for all layouts.
*   **Theme:** The application uses a purple and pink color scheme, defined in `colors.xml`.
*   **Onboarding:** The onboarding flow uses a `SwipeGestureListener` for navigation.
