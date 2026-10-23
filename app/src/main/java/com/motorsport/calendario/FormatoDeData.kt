package com.motorsport.calendario

import android.content.Context
import android.text.format.DateFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Jeitos de escrever uma data. [SISTEMA] segue a ordem dia/mês do aparelho. */
internal enum class FormatoDeData(val chave: String) {
    SISTEMA("sistema"),
    DIA_MES("dia_mes"),
    MES_DIA("mes_dia"),
    DIA_MES_TEXTO("dia_mes_texto"),
    MES_TEXTO_DIA("mes_texto_dia");

    companion object {
        /** Valor desconhecido ou ausente volta ao padrão do aparelho. */
        fun deChave(chave: String?): FormatoDeData =
            values().firstOrNull { it.chave == chave } ?: SISTEMA
    }
}

/** Escreve datas em cada [FormatoDeData]; sem dependência do Android, para ser testado. */
internal object FormatadorDeData {

    /** Troca [FormatoDeData.SISTEMA] pelo formato que a ordem do aparelho pede. */
    fun resolver(formato: FormatoDeData, mesAntesDoDia: Boolean): FormatoDeData =
        when {
            formato != FormatoDeData.SISTEMA -> formato
            mesAntesDoDia -> FormatoDeData.MES_DIA
            else -> FormatoDeData.DIA_MES
        }

    /** Sem o ano: "23/10", "10/23", "23 OUT" ou "OUT 23". */
    fun curta(data: LocalDate, formato: FormatoDeData, locale: Locale): String {
        val dia = dois(data.dayOfMonth)
        val mes = dois(data.monthValue)

        return when (formato) {
            FormatoDeData.MES_DIA -> "$mes/$dia"
            FormatoDeData.DIA_MES_TEXTO -> "$dia ${mesAbreviado(data, locale)}"
            FormatoDeData.MES_TEXTO_DIA -> "${mesAbreviado(data, locale)} $dia"
            else -> "$dia/$mes"
        }
    }

    /** Com o ano: "23/10/2026", "10/23/2026", "23 OUT 2026" ou "OUT 23, 2026". */
    fun completa(data: LocalDate, formato: FormatoDeData, locale: Locale): String {
        val dia = dois(data.dayOfMonth)
        val mes = dois(data.monthValue)

        return when (formato) {
            FormatoDeData.MES_DIA -> "$mes/$dia/${data.year}"
            FormatoDeData.DIA_MES_TEXTO -> "$dia ${mesAbreviado(data, locale)} ${data.year}"
            FormatoDeData.MES_TEXTO_DIA -> "${mesAbreviado(data, locale)} $dia, ${data.year}"
            else -> "$dia/$mes/${data.year}"
        }
    }

    /** "Sex", "Sáb", "Fri": dia da semana abreviado, com a inicial maiúscula. */
    fun diaDaSemana(data: LocalDate, locale: Locale): String =
        data.dayOfWeek
            .getDisplayName(TextStyle.SHORT, locale)
            .trimEnd('.')
            .replaceFirstChar { it.titlecase(locale) }

    private fun mesAbreviado(data: LocalDate, locale: Locale): String =
        data.month.getDisplayName(TextStyle.SHORT, locale).trimEnd('.').uppercase(locale)

    private fun dois(numero: Int): String = numero.toString().padStart(2, '0')
}

/** Guarda o formato de data escolhido nas configurações (padrão: o do aparelho). */
internal object PreferenciasDeData {

    private const val ARQUIVO = "preferencias_data"
    private const val CHAVE = "formato"

    fun atual(context: Context): FormatoDeData =
        FormatoDeData.deChave(
            context.applicationContext
                .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
                .getString(CHAVE, null)
        )

    fun definir(context: Context, formato: FormatoDeData) {
        context.applicationContext
            .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .edit()
            .putString(CHAVE, formato.chave)
            .apply()
    }

    /** Em quase todo lugar o mês vem depois do dia; nos Estados Unidos, antes. */
    private fun mesAntesDoDia(context: Context): Boolean {
        val ordem = DateFormat.getDateFormatOrder(context)
        val mes = ordem.indexOfFirst { it == 'M' || it == 'L' }
        val dia = ordem.indexOf('d')

        return mes >= 0 && dia >= 0 && mes < dia
    }

    /** O formato de fato usado: [FormatoDeData.SISTEMA] vira dia/mês ou mês/dia. */
    fun resolver(context: Context, formato: FormatoDeData = atual(context)): FormatoDeData =
        FormatadorDeData.resolver(formato, mesAntesDoDia(context))

    fun curta(context: Context, data: LocalDate): String =
        FormatadorDeData.curta(data, resolver(context), Idioma.locale(context))

    fun completa(context: Context, data: LocalDate): String =
        FormatadorDeData.completa(data, resolver(context), Idioma.locale(context))
}
