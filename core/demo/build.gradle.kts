plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "br.com.odontolivreval.core.demo"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.exifinterface)
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
}
