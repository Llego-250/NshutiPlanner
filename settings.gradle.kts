pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Unity .aar integration: place the Unity-exported .aar in app/libs/ and uncomment
        // the implementation line in app/build.gradle.kts dependencies block.
        flatDir { dirs("app/libs") }
    }
}

rootProject.name = "NshutiPlanner"
include(":app")
 