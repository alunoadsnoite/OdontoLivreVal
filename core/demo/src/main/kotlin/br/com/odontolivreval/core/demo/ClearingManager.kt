package br.com.odontolivreval.core.demo

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Coordena a limpeza dos estados sensíveis das telas compostas quando a atividade
 * é bloqueada ou enviada para segundo plano.
 */
object ClearingManager {
    private val callbacks = CopyOnWriteArraySet<() -> Unit>()

    fun register(callback: () -> Unit) {
        callbacks.add(callback)
    }

    fun unregister(callback: () -> Unit) {
        callbacks.remove(callback)
    }

    fun clearAll() {
        callbacks.forEach { callback -> runCatching(callback) }
    }
}
