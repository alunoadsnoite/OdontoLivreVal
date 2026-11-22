plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose)
  alias(libs.plugins.detekt)
}

val releaseSigning = mapOf(
  "ODONTO_RELEASE_STORE_FILE" to System.getenv("ODONTO_RELEASE_STORE_FILE"),
  "ODONTO_RELEASE_STORE_PASSWORD" to System.getenv("ODONTO_RELEASE_STORE_PASSWORD"),
  "ODONTO_RELEASE_KEY_ALIAS" to System.getenv("ODONTO_RELEASE_KEY_ALIAS"),
  "ODONTO_RELEASE_KEY_PASSWORD" to System.getenv("ODONTO_RELEASE_KEY_PASSWORD"),
)
val releaseSigningConfigured = releaseSigning.values.any { !it.isNullOrBlank() }
require(!releaseSigningConfigured || releaseSigning.values.all { !it.isNullOrBlank() }) {
  "Configure all ODONTO_RELEASE_* environment variables to sign a release, or leave them unset for an unsigned build."
}
val releaseStoreFile = releaseSigning["ODONTO_RELEASE_STORE_FILE"]?.let(rootProject::file)
if (releaseSigningConfigured) {
  require(releaseStoreFile?.isFile == true) { "ODONTO_RELEASE_STORE_FILE must point to an existing keystore." }
}

android {
  namespace = "br.com.odontolivreval"
  compileSdk = 37
  defaultConfig {
    minSdk = 26
    applicationId = "br.com.odontolivreval"
    targetSdk = 37
    versionCode = 15
    versionName = "0.14.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
  }
  signingConfigs {
    if (releaseSigningConfigured) {
      create("release") {
        storeFile = releaseStoreFile
        storePassword = releaseSigning.getValue("ODONTO_RELEASE_STORE_PASSWORD")
        keyAlias = releaseSigning.getValue("ODONTO_RELEASE_KEY_ALIAS")
        keyPassword = releaseSigning.getValue("ODONTO_RELEASE_KEY_PASSWORD")
      }
    }
  }
  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      if (releaseSigningConfigured) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      isMinifyEnabled = false
    }
  }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
  buildFeatures { compose = true; buildConfig = true }
  composeOptions {
    kotlinCompilerExtensionVersion = "2026.09.00"
  }
  lint {
    disable += "ObsoleteLintCustomCheck"
    checkReleaseBuilds = false
    abortOnError = false
  }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)
  implementation(libs.compose.foundation.layout)
  implementation(libs.compose.material3)
  implementation(project(":core:model"))
  implementation(project(":core:designsystem"))
  implementation(project(":feature:settings"))
  implementation(project(":feature:patients"))
  implementation(project(":core:demo"))
  implementation(project(":feature:schedule"))
  implementation(project(":feature:documents"))
  implementation(project(":feature:treatment"))
  implementation(project(":feature:consent"))
  implementation(project(":feature:signature"))
  implementation(libs.activity.compose)
  implementation(libs.lifecycle.runtime)
  implementation(libs.lifecycle.viewmodel)
  implementation(libs.navigation)
  androidTestImplementation(platform(libs.compose.bom))
  androidTestImplementation(libs.compose.test)
  androidTestImplementation(libs.android.junit)
  androidTestImplementation(libs.android.runner)
  debugImplementation(libs.compose.test.manifest)

  // Detekt
  detekt(libs.detekt.rules)

  // JUnit for local unit tests
  testImplementation(libs.junit)
}

detekt {
  config = files("detekt.yml")
  debug = false
  buildUponDefaultConfig = false
  ignoreFailures = false
}

tasks.register("detektAll", io.gitlab.arturbosch.detekt.Detekt::class) {
  description = "Run detekt on the app module"
  group = "verification"
}