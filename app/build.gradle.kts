plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.hersafe"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hersafe"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // المكتبات الأساسية الموجودة مسبقاً
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // مكتبة خرائط جوجل (تمت الإضافة هنا بالصيغة الصحيحة لـ Kotlin DSL)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Room Database - قاعدة البيانات المحلية
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Retrofit & Gson
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)
    implementation(libs.camera.view)
    implementation(libs.camera.extensions)

    implementation(libs.lifecycle.service)
    implementation("androidx.media:media:1.7.0")
}