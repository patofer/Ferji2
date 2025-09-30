// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.ksp) apply false // ASEGÚRATE DE TENER ESTA LÍNEA
    alias(libs.plugins.hilt.android) apply false // También puedes declarar Hilt aquí
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23" apply false

}