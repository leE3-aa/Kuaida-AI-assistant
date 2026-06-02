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

rootProject.name = "QuickAnswer"

include(
    ":app",
    ":core:common",
    ":core:model",
    ":core:data",
    ":feature:workbench",
    ":feature:knowledge",
    ":feature:history",
    ":feature:settings",
    ":feature:templates",
)
