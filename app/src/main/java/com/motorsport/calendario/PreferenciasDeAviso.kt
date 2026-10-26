package com.motorsport.calendario

import android.content.Context
import android.os.Build

/**
 * Escolhas de notificação do usuário, salvas no aparelho:
 *  - quanto tempo antes de cada sessão avisar (uma ou mais antecedências; nenhuma = avisos desligados);
 *  - para cada categoria, avisar de todas as sessões, só das corridas ou de nenhuma.
 */
internal object PreferenciasDeAviso {

    private const val ARQUIVO = "preferencias"
    private const val CHAVE_MINUTOS = "aviso_minutos_antes"
    private const val CHAVE_ULTIMOS_MINUTOS = "aviso_ultimos_minutos"
    private const val CHAVE_LISTA = "aviso_minutos_lista"
    private const val CHAVE_ULTIMA_LISTA = "aviso_ultimos_minutos_lista"
    private const val PREFIXO_MODO = "aviso_modo_"
    private const val CHAVE_NA_HORA = "aviso_na_hora_de_comecar"

    const val DESATIVADO = 0

    /** Antecedência usada ao religar os avisos quando nunca houve uma escolha. */
    const val PADRAO_MINUTOS = 30

    /** Botões rápidos do painel de alertas, da maior para a menor antecedência. */
    val opcoes: List<Int> = listOf(60, 45, 30, 15)

    /**
     * Antecedências a mais, escolhidas na página "Personalizar alertas". Para oferecer outro
     * tempo basta incluí-lo aqui: o agendamento aceita qualquer valor em minutos.
     */
    val opcoesExtras: List<Int> = listOf(1440, 720, 360, 180, 120, 90, 10, 5)

    /** Todas as antecedências válidas (botões rápidos + extras), da maior para a menor. */
    val todasAsOpcoes: List<Int> = (opcoes + opcoesExtras).distinct().sortedDescending()

    private fun arquivo(context: Context) =
        context.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    /** Antecedências escolhidas, da menor para a maior. Vazia significa "avisos desligados". */
    fun minutosSelecionados(context: Context): List<Int> {
        val salvos = arquivo(context)
        val lista = salvos.getString(CHAVE_LISTA, null)

        if (lista != null) return converter(lista)

        // Versões anteriores guardavam uma antecedência só.
        val antigo = salvos.getInt(CHAVE_MINUTOS, DESATIVADO)

        return if (antigo in todasAsOpcoes) listOf(antigo) else emptyList()
    }

    /** Última escolha feita, mesmo que os avisos tenham sido desligados depois. */
    fun ultimasSelecionadas(context: Context): List<Int> {
        val salvos = arquivo(context)
        val lista = salvos.getString(CHAVE_ULTIMA_LISTA, null)?.let { converter(it) }

        if (!lista.isNullOrEmpty()) return lista

        val antigo = salvos.getInt(CHAVE_ULTIMOS_MINUTOS, PADRAO_MINUTOS)

        return listOf(if (antigo in todasAsOpcoes) antigo else PADRAO_MINUTOS)
    }

    /** Salva as antecedências. Valores fora das opções são ignorados; lista vazia desliga. */
    fun definirSelecionados(context: Context, minutos: Collection<Int>) {
        val validos = minutos.filter { it in todasAsOpcoes }.distinct().sorted()
        val texto = validos.joinToString(",")
        val salvos = arquivo(context)

        // Fixa o aviso da hora de começar que valia até agora, para a mudança da
        // antecedência não alterar essa escolha por baixo dos panos.
        val naHoraAtual = avisarNaHora(context)

        salvos.edit().apply {
            if (!salvos.contains(CHAVE_NA_HORA)) putBoolean(CHAVE_NA_HORA, naHoraAtual)
            putString(CHAVE_LISTA, texto)
            if (validos.isNotEmpty()) putString(CHAVE_ULTIMA_LISTA, texto)
        }.apply()
    }

    private fun converter(texto: String): List<Int> =
        texto.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in todasAsOpcoes }
            .distinct()
            .sorted()

    /** Maior antecedência escolhida, ou [DESATIVADO]. Mantido para quem usa um valor só. */
    fun minutosAntes(context: Context): Int =
        minutosSelecionados(context).maxOrNull() ?: DESATIVADO

    /** Maior antecedência da última escolha. Mantido para quem usa um valor só. */
    fun ultimaAntecedencia(context: Context): Int =
        ultimasSelecionadas(context).maxOrNull() ?: PADRAO_MINUTOS

    /** Escolhe uma antecedência só (ou desliga, com zero ou valor inválido). */
    fun definir(context: Context, minutos: Int) {
        definirSelecionados(context, if (minutos in todasAsOpcoes) listOf(minutos) else emptyList())
    }

    /**
     * Se deve avisar na hora exata em que a sessão começa, mesmo sem nenhuma antecedência.
     *
     * Sem escolha salva: quem já usava o app continua como estava (avisos ligados = ligado,
     * avisos desligados = desligado); quem abre pela primeira vez recebe ligado no Android
     * 12 e anterior, onde não é preciso pedir permissão de notificação.
     */
    fun avisarNaHora(context: Context): Boolean {
        val salvos = arquivo(context)

        if (salvos.contains(CHAVE_NA_HORA)) return salvos.getBoolean(CHAVE_NA_HORA, true)

        if (!salvos.contains(CHAVE_LISTA) && salvos.contains(CHAVE_MINUTOS)) {
            return salvos.getInt(CHAVE_MINUTOS, DESATIVADO) in todasAsOpcoes
        }

        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2
    }

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
            minutos == 1440 -> "1 dia antes"
            minutos in todasAsOpcoes && minutos > 60 && minutos % 60 == 0 -> "${minutos / 60} horas antes"
            minutos in todasAsOpcoes -> "$minutos minutos antes"
            else -> "Desativado"
        }
}
