package br.com.odontolivreval.core.demo

import java.text.Normalizer

private val combiningMarks = Regex("\\p{M}+")

/**
 * Chave de busca e ordenação que ignora caixa, espaços nas pontas e acentos.
 */
fun String.clinicSearchKey(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase()
