package com.motorsport.calendario

import android.content.Context

/**
 * Quanto tempo antes de cada sessão o usuário quer ser avisado.
 * O valor fica salvo no aparelho; 0 significa "não avisar".
 */
internal object PreferenciasDeAviso {

    private const val ARQUIVO = "preferencias"
    private const val CHAVE_MINUTOS = "aviso_minutos_antes"
    private const val CHAVE_APENAS_CORRIDAS = "aviso_apenas_corridas"

    const val DESATIVADO = 0

    /** Opções oferecidas ao usuário, da maior para a menor antecedência. */
    val opcoes: List<Int> = listOf(60, 45, 30, 15)

    /** Valor salvo. Qualquer valor fora das opções (ou nenhum) conta como desativado. */
    fun minutosAntes(context: Context): Int {
        val salvo = context.applicationContext
            .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .getInt(CHAVE_MINUTOS, DESATIVADO)

        return if (salvo in opcoes) salvo else DESATIVADO
    }

    /** Salva a escolha. Valores fora das opções são gravados como desativado. */
    fun definir(context: Context, minutos: Int) {
        val valido = if (minutos in opcoes) minutos else DESATIVADO

        context.applicationContext
            .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .edit()
            .putInt(CHAVE_MINUTOS, valido)
            .apply()
    }

    /** true: avisa só das corridas. false (padrão): avisa de todas as sessões. */
    fun apenasCorridas(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .getBoolean(CHAVE_APENAS_CORRIDAS, false)

    fun definirApenasCorridas(context: Context, apenasCorridas: Boolean) {
        context.applicationContext
            .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CHAVE_APENAS_CORRIDAS, apenasCorridas)
            .apply()
    }

    fun descricao(minutos: Int): String =
        when {
            minutos == 60 -> "1 hora antes"
            minutos in opcoes -> "$minutos minutos antes"
            else -> "Desativado"
        }
}
