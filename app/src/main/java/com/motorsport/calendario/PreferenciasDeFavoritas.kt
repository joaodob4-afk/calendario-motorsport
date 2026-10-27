package com.motorsport.calendario

import android.content.Context

/** Categorias favoritas do usuário, salvas no aparelho (nomes normalizados). Nenhuma = mostrar todas. */
internal object PreferenciasDeFavoritas {

    private const val ARQUIVO = "preferencias"
    private const val CHAVE_CATEGORIAS = "categorias_favoritas"

    private fun arquivo(context: Context) =
        context.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    fun categorias(context: Context): Set<String> =
        arquivo(context).getStringSet(CHAVE_CATEGORIAS, null)?.toSet().orEmpty()

    fun definirCategorias(context: Context, categorias: Set<String>) {
        arquivo(context).edit().putStringSet(CHAVE_CATEGORIAS, HashSet(categorias)).apply()
    }
}
