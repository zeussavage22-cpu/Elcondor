pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Shizuku (dev.rikka.shizuku:*) se publica en Maven Central.
        mavenCentral()
    }
}

rootProject.name = "ElCondor"
include(":app")
