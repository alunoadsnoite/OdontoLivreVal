plugins { alias(libs.plugins.android.application); alias(libs.plugins.compose) }
android {
 namespace = "br.com.odontolivreval"
 compileSdk = 37
 defaultConfig { minSdk = 26
applicationId = "br.com.odontolivreval"
targetSdk = 37
versionCode = 5
versionName = "0.5.0"
testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
buildFeatures { compose = true; buildConfig = true }
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
implementation(libs.activity.compose)
implementation(libs.lifecycle.runtime)
implementation(libs.lifecycle.viewmodel)
implementation(libs.navigation)
androidTestImplementation(platform(libs.compose.bom))
androidTestImplementation(libs.compose.test)
androidTestImplementation(libs.android.junit)
androidTestImplementation(libs.android.runner)
debugImplementation(libs.compose.test.manifest)
}
