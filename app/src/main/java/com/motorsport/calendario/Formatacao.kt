package com.motorsport.calendario

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

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

    // Compilado uma vez só: normalizar() roda milhares de vezes ao montar a tela inicial.
    private val marcasDeAcento = Regex("\\p{M}+")

    /** Minúsculas e sem acentos: "Fórmula E" e "formula e" são tratados como iguais. */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replace(marcasDeAcento, "")
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

    private val formatoDiaMes = DateTimeFormatter.ofPattern("dd/MM")
    private val formatoMesDiaIngles = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    private val formatoDataBrasileiraEstrito =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val formatoDataCompletaIngles = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

    /**
     * Período da etapa em DD/MM, sem nome de mês nem ano (o mês e o ano já aparecem no
     * cabeçalho da lista): "30/10 – 01/11". Etapa de um dia só (ou sem data de fim) mostra
     * uma data.
     * Devolve null se não houver data de início; quem chama usa então o texto original.
     */
    fun periodo(inicio: LocalDate?, fim: LocalDate?): String? {
        if (inicio == null) return null
        if (fim == null || fim.isBefore(inicio) || fim == inicio) {
            return inicio.format(formatoDiaMes)
        }
        return "${inicio.format(formatoDiaMes)} – ${fim.format(formatoDiaMes)}"
    }

    /**
     * Mesmo período em inglês, com o mês escrito para não confundir com o formato dos
     * Estados Unidos: "Oct 23–25", "Oct 30 – Nov 1" ou "Oct 23". Null sem data de início.
     */
    fun periodoEmIngles(inicio: LocalDate?, fim: LocalDate?): String? {
        if (inicio == null) return null
        if (fim == null || fim.isBefore(inicio) || fim == inicio) {
            return inicio.format(formatoMesDiaIngles)
        }
        if (inicio.month == fim.month && inicio.year == fim.year) {
            return "${inicio.format(formatoMesDiaIngles)}–${fim.dayOfMonth}"
        }
        return "${inicio.format(formatoMesDiaIngles)} – ${fim.format(formatoMesDiaIngles)}"
    }

    /**
     * Data dos calendários (dd/MM/yyyy ou yyyy-MM-dd) escrita com o mês por extenso
     * ("Oct 23, 2026"). Se o texto não for uma data conhecida, devolve o original.
     */
    fun dataEmIngles(texto: String): String {
        val data = lerDataDoCalendario(texto) ?: return texto

        return data.format(formatoDataCompletaIngles)
    }

    /** Lê uma data dos calendários (dd/MM/yyyy ou yyyy-MM-dd); null se o texto não for uma data. */
    fun lerDataDoCalendario(texto: String): LocalDate? {
        val limpo = texto.trim()

        return runCatching {
            if (limpo.contains('/')) {
                LocalDate.parse(limpo, formatoDataBrasileiraEstrito)
            } else {
                LocalDate.parse(limpo.take(10))
            }
        }.getOrNull()
    }
}
