package com.motorsport.calendario

import java.text.Normalizer

/**
 * Funções de texto sem dependência do Android. Ficam fora das telas para poderem ser
 * testadas automaticamente (os testes rodam no GitHub Actions a cada Pull Request).
 */
internal object Formatacao {

    private val doisMeses =
        Regex("^(\\d{1,2})\\s+(\\p{L}{3,4})\\s*[–—-]\\s*(\\d{1,2})\\s+(\\p{L}{3,4})$")
    private val mesmoMes =
        Regex("^(\\d{1,2})\\s*[–—-]\\s*(\\d{1,2})\\s+(\\p{L}{3,4})$")
    private val diaUnico =
        Regex("^(\\d{1,2})\\s+(\\p{L}{3,4})$")

    /** Minúsculas e sem acentos: "Fórmula E" e "formula e" são tratados como iguais. */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()

    /**
     * Separa a data de uma etapa em (dias, mês) para o bloco de data dos cards:
     * "30 OUT–01 NOV" vira ("30–01", "OUT–NOV"), "06–08 NOV" vira ("06–08", "NOV")
     * e "16 JAN" vira ("16", "JAN"). Formatos desconhecidos voltam inteiros, sem mês.
     */
    fun partirData(data: String): Pair<String, String> {
        val limpa = data.trim().uppercase()

        doisMeses.matchEntire(limpa)?.let {
            val g = it.groupValues
            return "${g[1]}–${g[3]}" to "${g[2]}–${g[4]}"
        }

        mesmoMes.matchEntire(limpa)?.let {
            val g = it.groupValues
            return "${g[1]}–${g[2]}" to g[3]
        }

        diaUnico.matchEntire(limpa)?.let {
            val g = it.groupValues
            return g[1] to g[2]
        }

        return limpa to ""
    }
}
