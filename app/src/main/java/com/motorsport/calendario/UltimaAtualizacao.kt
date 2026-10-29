package com.motorsport.calendario

import android.content.Context
import java.time.Instant

/** Quando um calendário foi baixado e gravado pela última vez neste aparelho. */
internal object UltimaAtualizacao {

    private const val ARQUIVO = "preferencias"
    private const val CHAVE = "ultima_atualizacao_calendarios"

    private fun arquivo(context: Context) =
        context.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    fun registrar(context: Context, agora: Long = System.currentTimeMillis()) {
        arquivo(context).edit().putLong(CHAVE, agora).apply()
    }

    /** Instante da última atualização, ou null se nunca houve nenhuma. */
    fun ler(context: Context): Long? =
        arquivo(context).getLong(CHAVE, 0L).takeIf { it > 0L }

    /** "29/10 · 14:32", nos formatos de data e hora escolhidos; null se nunca atualizou. */
    fun texto(context: Context): String? {
        val instante = ler(context) ?: return null
        val local = Instant.ofEpochMilli(instante).atZone(Fuso.doAparelho())

        return PreferenciasDeData.curta(context, local.toLocalDate()) +
            " · " + Hora.deZonedDateTime(context, local)
    }
}
