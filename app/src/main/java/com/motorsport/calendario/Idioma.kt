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
     * Nome de evento no original ("Italian Grand Prix", "6 Hours of Monza"), igual em qualquer
     * idioma do app. Os nomes guardados em português só servem de chave para achar o original.
     */
    fun evento(@Suppress("UNUSED_PARAMETER") context: Context, nome: String): String =
        TraducaoDeNomes.evento(nome)

    /** Nome de sessão ("Treino Livre 1") no idioma do app. */
    fun sessao(context: Context, nome: String): String =
        if (ingles(context)) TraducaoDeSessoes.paraIngles(nome) else nome

    /** País ou cidade no idioma do app, inclusive locais compostos como "Barcelona, Spain". */
    fun lugar(context: Context, nome: String): String =
        if (ingles(context)) {
            TraducaoDeNomes.lugar(nome)
        } else {
            TraducaoDeNomes.lugarParaPortugues(nome)
        }

    /**
     * Data dos calendários (dd/MM/yyyy ou yyyy-MM-dd) no formato escolhido nas configurações
     * (por padrão, o do aparelho). Texto que não é data volta como estava.
     */
    fun data(context: Context, texto: String): String {
        val data = Formatacao.lerDataDoCalendario(texto) ?: return texto
        return PreferenciasDeData.completa(context, data)
    }
}
