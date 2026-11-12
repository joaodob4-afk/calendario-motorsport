package com.motorsport.calendario

import android.content.Context
import java.util.Locale

/**
 * Idioma do app: português quando o aparelho está em português; inglês em qualquer outro
 * caso. O valor vem de res/values/strings.xml (inglês) e res/values-pt/strings.xml.
 */
internal object Idioma {

    fun ingles(context: Context): Boolean =
        context.resources.getBoolean(R.bool.traduzir_sessoes)

    /** Para formatar datas (meses, dias da semana) no mesmo idioma dos textos. */
    fun locale(context: Context): Locale =
        if (ingles(context)) Locale.ENGLISH else Locale("pt", "BR")

    /**
     * Converte o identificador interno da categoria para o nome localizado nas notificações.
     * Identificadores desconhecidos são preservados para não esconder categorias futuras.
     */
    fun categoria(context: Context, nome: String): String {
        val recurso = recursoCategoria(nome) ?: return nome
        return context.getString(recurso)
    }

    /** Separado para permitir testar o mapeamento sem depender de um Context Android. */
    internal fun recursoCategoria(nome: String): Int? =
        when (nome.trim().lowercase(Locale.ROOT)) {
            "f1", "formula 1", "fórmula 1" -> R.string.formula_1
            "f2", "formula 2", "fórmula 2" -> R.string.formula_2
            "f3", "formula 3", "fórmula 3" -> R.string.formula_3
            "f1 academy" -> R.string.f1_academy
            "formula e", "fórmula e" -> R.string.formula_e
            "indycar" -> R.string.nome_indycar
            "nascar cup series" -> R.string.nascar_cup
            "nascar xfinity series", "nascar o'reilly auto parts series" -> R.string.nascar_xfinity
            "nascar craftsman truck series" -> R.string.nascar_truck
            else -> null
        }

    /**
     * Nome de evento no original ("Italian Grand Prix", "6 Hours of Monza"), igual em qualquer
     * idioma do app. Os nomes guardados em português só servem de chave para achar o original.
     */
    fun evento(@Suppress("UNUSED_PARAMETER") context: Context, nome: String): String =
        TraducaoDeNomes.evento(nome)

    /** Traduz nomes de sessão conforme o idioma do app, mesmo quando a fonte já vem em inglês. */
    fun sessao(context: Context, nome: String): String =
        if (ingles(context)) TraducaoDeSessoes.paraIngles(nome)
        else TraducaoDeSessoes.paraPortugues(nome)

    /** País ou cidade no idioma do app. */
    fun lugar(context: Context, nome: String): String =
        if (ingles(context)) TraducaoDeNomes.lugar(nome) else nome

    /**
     * Data dos calendários (dd/MM/yyyy ou yyyy-MM-dd) no formato escolhido nas configurações
     * (por padrão, o do aparelho). Texto que não é data volta como estava.
     */
    fun data(context: Context, texto: String): String {
        val data = Formatacao.lerDataDoCalendario(texto) ?: return texto
        return PreferenciasDeData.completa(context, data)
    }
}
