package br.com.odontolivreval.core.demo

/**
 * Placeholder mantido para compatibilidade. A limpeza de ViewModels não é mais
 * gerenciada centralmente aqui; cada ViewModel expõe seu próprio metodo clear()
 * se necessario.
 */
object ClearingManager {
    /** Nenhuma operacao necessaria neste momento. */
    fun register(callback: () -> Unit) {}
    fun unregister(callback: () -> Unit) {}
    fun clearAll() {}
}
