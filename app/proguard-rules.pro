# OdontoLivreVal — ProGuard/R8 rules for release builds
# Keep application class and Android components
-keep class br.com.odontolivreval.OdontoApplication { *; }
-keep class br.com.odontolivreval.MainActivity { *; }

# Keep ViewModels and their factories (reflection used by ViewModel providers)
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * implements androidx.lifecycle.ViewModelProvider.Factory { *; }

# Keep data classes used in navigation state restoration
-keep class br.com.odontolivreval.core.demo.DemoPatient { *; }
-keep class br.com.odontolivreval.core.demo.DemoAppointment { *; }

# Keep Compose-related classes used via reflection
-keep class androidx.compose.runtime.saveable.* { *; }

# Keep JNI methods if any
-keepclasseswithmembers class * {
    native <methods>;
}

# Preserve entry points
-keepclasseswithmembers class * {
    @androidx.lifecycle.OnLifecycleEvent *;
}

# Do not obfuscate model classes serialized in backup
-keep class br.com.odontolivreval.core.demo.ClinicSnapshot { *; }
-keep class br.com.odontolivreval.core.demo.ProtectedPayload { *; }

# Allow R8 to shrink and optimize but keep public API of the app
-keep public class br.com.odontolivreval.** {
    public <methods>;
    public <fields>;
}