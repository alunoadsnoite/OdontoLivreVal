plugins { alias(libs.plugins.android.library); alias(libs.plugins.compose) }
android {
 namespace = "br.com.odontolivreval.feature.settings"
 compileSdk = 37
 defaultConfig { minSdk = 26
}
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
buildFeatures { compose = true }
}
dependencies {
implementation(platform(libs.compose.bom))
implementation(libs.compose.ui)
implementation(libs.compose.material3)
implementation(project(":core:model"))
implementation(libs.datastore)
implementation(libs.lifecycle.viewmodel)
implementation(libs.lifecycle.runtime)
testImplementation(libs.junit)
testImplementation(libs.coroutines.test)
}
