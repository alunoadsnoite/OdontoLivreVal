package br.com.odontolivreval.core.model

/** Stable storage keys: never persist enum ordinals. */
enum class ThemeMode(val key: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");
    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
