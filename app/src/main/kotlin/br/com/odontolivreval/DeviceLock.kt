package br.com.odontolivreval

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class DeviceLock(private val activity: ComponentActivity) {
    private val keyguard = requireNotNull(activity.getSystemService(KeyguardManager::class.java))
    private val settings = activity.getSharedPreferences("device-lock", Context.MODE_PRIVATE)
    private var completion: ((Boolean) -> Unit)? = null
    private var signal: CancellationSignal? = null
    private val credential = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        complete(it.resultCode == Activity.RESULT_OK)
    }

    var enabled by mutableStateOf(settings.getBoolean("enabled", false))
        private set
    var unlocked by mutableStateOf(!enabled)
        private set
    var secure by mutableStateOf(keyguard.isDeviceSecure)
        private set

    fun refreshCredentialStatus() {
        secure = keyguard.isDeviceSecure
    }

    fun lock() {
        if (enabled) unlocked = false
    }

    fun unlock() = authenticate { if (it) unlocked = true }

    fun changeEnabled(enabled: Boolean) = authenticate { accepted ->
        if (accepted) {
            settings.edit().putBoolean("enabled", enabled).apply()
            this.enabled = enabled
            unlocked = true
        }
    }

    fun dispose() {
        completion = null
        signal?.cancel()
    }

    private fun authenticate(callback: (Boolean) -> Unit) {
        if (!secure || completion != null) return
        completion = callback
        if (Build.VERSION.SDK_INT >= 30) {
            authenticateModern()
        } else {
            @Suppress("DEPRECATION")
            val intent = keyguard.createConfirmDeviceCredentialIntent(
                activity.getString(R.string.app_name),
                activity.getString(R.string.security_unlock),
            )
            if (intent == null) complete(false) else credential.launch(intent)
        }
    }

    @RequiresApi(30)
    private fun authenticateModern() {
        val cancellation = CancellationSignal().also { signal = it }
        BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.app_name))
            .setSubtitle(activity.getString(R.string.security_unlock))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
            .authenticate(cancellation, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = complete(true)

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = complete(false)
            })
    }

    private fun complete(accepted: Boolean) {
        val callback = completion
        completion = null
        signal = null
        callback?.invoke(accepted)
    }
}
