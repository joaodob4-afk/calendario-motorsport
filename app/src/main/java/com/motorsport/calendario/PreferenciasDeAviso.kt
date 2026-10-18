package com.motorsport.calendario

import android.content.Context

/**
 * Escolhas de notificação do usuário, salvas no aparelho:
 *  - quanto tempo antes de cada sessão avisar (0 significa "não avisar");
 *  - para cada categoria, avisar de todas as sessões, só das corridas ou de nenhuma.
 */
internal object PreferenciasDeAviso {

    private const val ARQUIVO = "preferencias"
    private const val CHAVE_MINUTOS = "aviso_minutos_antes"
    private const val CHAVE_ULTIMOS_MINUTOS = "aviso_ultimos_minutos"
    private const val PREFIXO_MODO = "aviso_modo_"
    private const val CHAVE_NA_HORA = "aviso_na_hora_de_comecar"

    const val DESATIVADO = 0

    /** Antecedência usada ao religar os avisos quando nunca houve uma escolha. */
    const val PADRAO_MINUTOS = 30

    /** Opções oferecidas ao usuário, da maior para a menor antecedência. */
    val opcoes: List<Int> = listOf(60, 45, 30, 15)

    private fun arquivo(context: Context) =
        context.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    /** Valor salvo. Qualquer valor fora das opções (ou nenhum) conta como desativado. */
    fun minutosAntes(context: Context): Int {
        val salvo = arquivo(context).getInt(CHAVE_MINUTOS, DESATIVADO)

        return if (salvo in opcoes) salvo else DESATIVADO
    }

    /** Última antecedência escolhida, mesmo que os avisos tenham sido desligados depois. */
    fun ultimaAntecedencia(context: Context): Int {
        val salvo = arquivo(context).getInt(CHAVE_ULTIMOS_MINUTOS, PADRAO_MINUTOS)

        return if (salvo in opcoes) salvo else PADRAO_MINUTOS
    }

    /** Salva a escolha. Valores fora das opções são gravados como desativado. */
    fun definir(context: Context, minutos: Int) {
        val valido = if (minutos in opcoes) minutos else DESATIVADO

        arquivo(context).edit().apply {
            putInt(CHAVE_MINUTOS, valido)
            if (valido != DESATIVADO) putInt(CHAVE_ULTIMOS_MINUTOS, valido)
        }.apply()
    }

    /** Se também deve avisar na hora exata em que a sessão começa. Ligado por padrão. */
    fun avisarNaHora(context: Context): Boolean =
        arquivo(context).getBoolean(CHAVE_NA_HORA, true)

    fun definirAvisarNaHora(context: Context, ligado: Boolean) {
        arquivo(context).edit().putBoolean(CHAVE_NA_HORA, ligado).apply()
    }

    private fun chaveDoModo(categoria: String) =
        PREFIXO_MODO + Formatacao.normalizar(categoria)

    /** O que avisar da categoria. Sem escolha, avisa de todas as sessões. */
    fun modoDaCategoria(context: Context, categoria: String): ModoDeAviso {
        val salvo = arquivo(context).getString(chaveDoModo(categoria), null)

        return ModoDeAviso.values().firstOrNull { it.name == salvo } ?: ModoDeAviso.TODAS
    }

    fun definirModo(context: Context, categoria: String, modo: ModoDeAviso) {
        arquivo(context).edit().putString(chaveDoModo(categoria), modo.name).apply()
    }

    fun descricao(minutos: Int): String =
        when {
            minutos == 60 -> "1 hora antes"
            minutos in opcoes -> "$minutos minutos antes"
            else -> "Desativado"
        }
}
