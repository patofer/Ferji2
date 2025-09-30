import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.android)
    kotlin("plugin.serialization") version "1.9.23"
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
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }

        val apiKeyFromProperties = localProperties.getProperty("SENDGRID_API_KEY", "NO_SE_ENCONTRO_LA_CLAVE")
        println("DEBUG: SENDGRID_API_KEY desde local.properties = '$apiKeyFromProperties'")

        buildConfigField(
            "String",
            "SENDGRID_API_KEY",
            "\"" + localProperties.getProperty("SENDGRID_API_KEY", "") + "\""
        )
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.adapters)
    implementation(libs.androidx.lifecycle.viewmodel.android)
    ksp(libs.androidx.room.compiler.ksp)

    // Hilt - VERSIÓN CORREGIDA
    implementation(libs.google.hilt.android)
    ksp(libs.google.hilt.compiler)

    // Hilt para WorkManager - VERSIÓN ACTUALIZADA
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Hilt Navigation Compose
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Otras dependencias
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("androidx.compose.material:material-icons-core:1.5.4")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.compose.material3:material3")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // PDF
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.itextpdf:layout:7.2.5")
    implementation("com.itextpdf:io:7.2.5")
    implementation("org.slf4j:slf4j-nop:1.7.32")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}