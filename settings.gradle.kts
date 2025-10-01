// En C:/Proyectos/GITHUB/Jerji/Ferji2/settings.gradle.kts

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()       // <- Fuente oficial de librerías de Android/Google
        mavenCentral() // <- Fuente principal para librerías de Java/Kotlin
        // Se han eliminado los repositorios de Aliyun que causaban el conflicto.
    }
}

rootProject.name = "Ferji2" // Corregido de "Ferji" a "Ferji2" para coincidir con el nombre de tu carpeta de proyecto
include(":app")
