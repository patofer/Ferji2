import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android) version "1.9.23" // Se mantiene con la versión correcta
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.android)
    kotlin("plugin.serialization") version "1.9.23"
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

android {
    namespace = "com.ferji.inspecciones"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ferji.inspecciones"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.0.1"

    // Renombrar el APK generado
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "ferji${variant.versionName}.apk"
            }
    }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"



        buildConfigField(
            "String",
            "SENDGRID_API_KEY",
            "\"" + localProperties.getProperty("SENDGRID_API_KEY", "") + "\""
        )
        buildConfigField(
            "String",
            "SMTP_USER",
            "\"" + localProperties.getProperty("SMTP_USER", "") + "\""
        )
        buildConfigField(
            "String",
            "SMTP_PASSWORD",
            "\"" + localProperties.getProperty("SMTP_PASSWORD", "") + "\""
        )
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // --- LA CORRECCIÓN FINAL ---
    // Reintroducimos este bloque para forzar la versión correcta del compilador.
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/LICENSE"
        }
    }
}


dependencies {
    // ---- Compose ----
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material.icons.extended)




    // ---- Core y Lifecycle ----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)

    // ---- Room (Base de datos) ----
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler.ksp)

    // ---- Hilt (Inyección de Dependencias) ----
    implementation(libs.google.hilt.android)
    ksp(libs.google.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ---- WorkManager (Tareas en segundo plano) con Hilt ----
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // ---- Networking (Retrofit, OkHttp) ----
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // ---- Coroutines ----
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // ---- Serialización y otras utilidades ----
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation(libs.androidx.exifinterface)

    // ---- PDF (iText) ----
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.itextpdf:layout:7.2.5")
    implementation("com.itextpdf:io:7.2.5")
    implementation("org.slf4j:slf4j-nop:1.7.32")

    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))

    // Dependencia para Firestore
    implementation ("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // ---- Jetpack DataStore (para gestionar la sesión) ----
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("org.dhatim:fastexcel:0.18.3")

    // ---- JavaMail para Android (envío automático de email via SMTP) ----
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    // ---- Testing ----
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

