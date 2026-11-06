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
        mavenCentral()
    }
}

rootProject.name = "PIE"
include(":app")
include(":core:domain", ":core:data", ":core:designsystem")
include(":feature:home", ":feature:editor", ":feature:history", ":feature:settings")
include(":feature:pet")
