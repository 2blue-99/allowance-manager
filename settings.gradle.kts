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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "allowance-manager"

include(":app")
include(":feature:splash")
include(":feature:home")
include(":feature:setting")
include(":core:domain")
include(":core:data")
include(":core:remote")
include(":core:local")
include(":core:data-store")
include(":core:config")
include(":core:ui")
