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

    /** Nome de evento ("GP da Itália") no idioma do app. */
    fun evento(context: Context, nome: String): String =
        if (ingles(context)) TraducaoDeNomes.evento(nome) else nome

    /** Nome de sessão ("Treino Livre 1") no idioma do app. */
    fun sessao(context: Context, nome: String): String =
        if (ingles(context)) TraducaoDeSessoes.paraIngles(nome) else nome

    /** País ou cidade no idioma do app. */
    fun lugar(context: Context, nome: String): String =
        if (ingles(context)) TraducaoDeNomes.lugar(nome) else nome

    /** Data dos calendários (dd/MM/yyyy ou yyyy-MM-dd) no formato do idioma do app. */
    fun data(context: Context, texto: String): String =
        if (ingles(context)) Formatacao.dataEmIngles(texto) else texto
}
