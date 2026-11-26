import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.elcondor.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.elcondor.app"
        // Shizuku API 13 soporta API 24+, pero usamos 26 para tener
        // Process.waitFor(timeout) y las APIs de TileService modernas.
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        // Necesario para la interfaz IShellService.aidl del UserService de Shizuku.
        aidl = true
        // Usamos BuildConfig.VERSION_CODE para versionar el UserService.
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // --- Shizuku ---------------------------------------------------------
    val shizukuVersion = "13.1.5"
    // API cliente: binder, permisos, UserService.
    implementation("dev.rikka.shizuku:api:$shizukuVersion")
    // ShizukuProvider: canal por el que Shizuku entrega su binder a la app.
    implementation("dev.rikka.shizuku:provider:$shizukuVersion")

    // --- Jetpack Compose (Material 3) -------------------------------------
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- AndroidX / Lifecycle ---------------------------------------------
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // --- Corrutinas -------------------------------------------------------
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
