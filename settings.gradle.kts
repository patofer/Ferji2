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
        google()
        mavenCentral()

        // SOLO si necesitas repositorios alternativos, usa HTTPS:
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
    }
}

rootProject.name = "Ferji"
include(":app")