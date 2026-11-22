package br.com.odontolivreval.core.model

enum class AppLanguage(val tag: String, val label: String) {
    PORTUGUESE("pt-BR", "Português (Brasil)"),
    ENGLISH("en-US", "English (US)"),
    SPANISH("es", "Español"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: PORTUGUESE
    }
}
