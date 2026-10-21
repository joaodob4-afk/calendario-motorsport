package com.motorsport.calendario

import java.time.LocalDate
import java.time.YearMonth

/**
 * O que está filtrando a lista da tela inicial.
 *
 * - [categoria]: vazio = todas; senão, só as etapas dessa categoria (botões do topo).
 * - [mostrarFinalizadas]: desligado, as etapas que já terminaram ficam escondidas.
 * - [paises]: vazio = todos; senão, só etapas desses países (o texto é o campo "pais" dos dados).
 * - [meses]: vazio = todos; senão, só etapas que acontecem nesses meses (formato "2026-11").
 */
internal data class FiltrosDeEtapas(
    val categoria: String = "",
    val mostrarFinalizadas: Boolean = false,
    val paises: Set<String> = emptySet(),
    val meses: Set<String> = emptySet()
) {
    /** Quantos filtros do painel (sem contar a categoria) estão ligados. */
    val quantosAtivos: Int
        get() = (if (mostrarFinalizadas) 1 else 0) + paises.size + meses.size
}

/**
 * Regras do filtro da tela inicial, sem dependência do Android para poderem ser testadas
 * automaticamente.
 */
internal object RegrasDeFiltro {

    private fun lerData(texto: String): LocalDate? =
        if (texto.isBlank()) null else runCatching { LocalDate.parse(texto) }.getOrNull()

    fun inicio(etapa: MainActivity.Etapa): LocalDate? = lerData(etapa.dataInicio)

    /**
     * Último dia da etapa. Sem data de fim (ou com fim antes do início), considera 3 dias de
     * evento. Sem data de início, a etapa nunca é tratada como finalizada.
     */
    fun fim(etapa: MainActivity.Etapa, hoje: LocalDate): LocalDate {
        val inicio = inicio(etapa) ?: hoje
        val fim = lerData(etapa.dataFim) ?: inicio.plusDays(2)
        return if (fim.isBefore(inicio)) inicio.plusDays(2) else fim
    }

    fun finalizada(etapa: MainActivity.Etapa, hoje: LocalDate): Boolean =
        fim(etapa, hoje).isBefore(hoje)

    /** Meses ("2026-11") em que a etapa acontece: o do início e o do fim (podem ser diferentes). */
    fun mesesDaEtapa(etapa: MainActivity.Etapa): Set<String> {
        val inicio = inicio(etapa) ?: return emptySet()
        val fim = lerData(etapa.dataFim)?.takeIf { !it.isBefore(inicio) } ?: inicio
        return setOf(YearMonth.from(inicio).toString(), YearMonth.from(fim).toString())
    }

    /**
     * Etapas que a categoria e a opção de finalizadas deixam passar. É a partir delas que se
     * montam as opções de país e de mês, para não oferecer uma opção que não mostra nada.
     */
    fun base(
        etapas: List<MainActivity.Etapa>,
        filtros: FiltrosDeEtapas,
        hoje: LocalDate
    ): List<MainActivity.Etapa> {
        val alvo = Formatacao.normalizar(filtros.categoria)

        return etapas.filter { etapa ->
            (alvo.isEmpty() || Formatacao.normalizar(etapa.categoria) == alvo) &&
                (filtros.mostrarFinalizadas || !finalizada(etapa, hoje))
        }
    }

    /** A lista da tela inicial: todos os filtros aplicados, em ordem de data. */
    fun aplicar(
        etapas: List<MainActivity.Etapa>,
        filtros: FiltrosDeEtapas,
        hoje: LocalDate
    ): List<MainActivity.Etapa> =
        base(etapas, filtros, hoje)
            .filter { etapa ->
                (filtros.paises.isEmpty() || etapa.pais in filtros.paises) &&
                    (filtros.meses.isEmpty() || mesesDaEtapa(etapa).any { it in filtros.meses })
            }
            .sortedBy { inicio(it) ?: LocalDate.MAX }

    /** Países que aparecem nas etapas de [base] (nomes como estão nos dados). */
    fun paisesDisponiveis(base: List<MainActivity.Etapa>): List<String> =
        base.map { it.pais }.filter { it.isNotBlank() }.distinct()

    /** Meses ("2026-11") das etapas de [base], em ordem cronológica. */
    fun mesesDisponiveis(base: List<MainActivity.Etapa>): List<String> =
        base.flatMap { mesesDaEtapa(it) }.distinct().sorted()

    /**
     * Tira dos filtros o que deixou de existir (por exemplo, o país de uma etapa finalizada
     * depois de esconder as finalizadas), para nunca sobrar um filtro que esvazia a lista
     * sem o usuário ver.
     */
    fun podar(
        etapas: List<MainActivity.Etapa>,
        filtros: FiltrosDeEtapas,
        hoje: LocalDate
    ): FiltrosDeEtapas {
        val base = base(etapas, filtros, hoje)

        return filtros.copy(
            paises = filtros.paises.intersect(paisesDisponiveis(base).toSet()),
            meses = filtros.meses.intersect(mesesDisponiveis(base).toSet())
        )
    }
}
