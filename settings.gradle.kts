pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "OdontoLivreVal"
include(":app", ":core:designsystem", ":core:model", ":feature:settings", ":feature:patients", ":core:demo", ":feature:schedule", ":feature:documents")
