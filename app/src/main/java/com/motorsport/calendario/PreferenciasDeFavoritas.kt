package com.motorsport.calendario

import android.content.Context

/** Etapas favoritas do usuário, salvas no aparelho (ver [RegrasDeFiltro.chaveDaEtapa]). */
internal object PreferenciasDeFavoritas {

    private const val ARQUIVO = "preferencias"
    private const val CHAVE_FAVORITAS = "etapas_favoritas"
    private const val CHAVE_SOMENTE = "mostrar_somente_favoritas"

    private fun arquivo(context: Context) =
        context.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    fun favoritas(context: Context): Set<String> =
        arquivo(context).getStringSet(CHAVE_FAVORITAS, null)?.toSet().orEmpty()

    fun definirFavoritas(context: Context, chaves: Set<String>) {
        arquivo(context).edit().putStringSet(CHAVE_FAVORITAS, HashSet(chaves)).apply()
    }

    fun somenteFavoritas(context: Context): Boolean =
        arquivo(context).getBoolean(CHAVE_SOMENTE, false)

    fun definirSomenteFavoritas(context: Context, ligado: Boolean) {
        arquivo(context).edit().putBoolean(CHAVE_SOMENTE, ligado).apply()
    }
}
