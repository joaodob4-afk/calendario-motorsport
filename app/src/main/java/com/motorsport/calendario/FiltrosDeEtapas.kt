package com.motorsport.calendario

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

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

    /** Horas que a etapa ainda conta como em andamento depois de começar a última sessão. */
    internal const val HORAS_APOS_ULTIMA_SESSAO = 4L

    /**
     * Terminou quando o último dia já passou ou, se o horário da última sessão é conhecido
     * ([MainActivity.Etapa.terminaEmMs]), quando esse horário já passou. Assim a etapa não fica
     * "em andamento" o dia inteiro depois de a corrida acabar.
     */
    fun finalizada(
        etapa: MainActivity.Etapa,
        hoje: LocalDate,
        agora: Instant = Instant.now()
    ): Boolean =
        fim(etapa, hoje).isBefore(hoje) ||
            (etapa.terminaEmMs > 0L && agora.toEpochMilli() >= etapa.terminaEmMs)

    /**
     * Preenche [MainActivity.Etapa.terminaEmMs] com o fim da última sessão + uma margem. Só vale
     * quando a última sessão conhecida cai no último dia da etapa (se alguma sessão final ainda está
     * "A confirmar", a última conhecida é mais cedo e não diz nada sobre o fim). Resistência (WEC)
     * fica de fora: a corrida de 24 horas começa um dia antes do fim.
     */
    fun comFimPorHorario(
        etapas: List<MainActivity.Etapa>,
        sessoes: List<SessaoParaAviso>
    ): List<MainActivity.Etapa> {
        if (sessoes.isEmpty()) return etapas

        val ultimas = ultimasSessoes(etapas, sessoes)
        val fuso = ZoneId.systemDefault()

        return etapas.map { etapa ->
            val ultima = ultimas[etapa]
            if (ultima == null || Formatacao.normalizar(etapa.categoria) == "wec") {
                etapa
            } else {
                val diaDaUltima = ultima.atZone(fuso).toLocalDate()
                if (diaDaUltima.isBefore(fim(etapa, diaDaUltima))) {
                    etapa
                } else {
                    etapa.copy(
                        terminaEmMs = ultima.plus(Duration.ofHours(HORAS_APOS_ULTIMA_SESSAO)).toEpochMilli()
                    )
                }
            }
        }
    }

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

    /**
     * Os horários (instantes exatos) das sessões de cada etapa. A sessão é da etapa se for da
     * mesma categoria e cair entre dois dias antes do início e dois depois do fim (as etapas de
     * uma mesma categoria nunca ficam tão perto uma da outra). Etapa sem nenhuma sessão com
     * horário confirmado não aparece no resultado.
     */
    private fun sessoesPorEtapa(
        etapas: List<MainActivity.Etapa>,
        sessoes: List<SessaoParaAviso>
    ): Map<MainActivity.Etapa, List<Instant>> {
        if (sessoes.isEmpty()) return emptyMap()

        val porCategoria = sessoes.groupBy { Formatacao.normalizar(it.categoria) }
        val resultado = HashMap<MainActivity.Etapa, List<Instant>>()

        for (etapa in etapas) {
            val inicio = inicio(etapa) ?: continue
            val fim = lerData(etapa.dataFim)?.takeIf { !it.isBefore(inicio) } ?: inicio
            val daCategoria = porCategoria[Formatacao.normalizar(etapa.categoria)] ?: continue

            val instantes = daCategoria
                .filter {
                    val dia = it.inicio.toLocalDate()
                    !dia.isBefore(inicio.minusDays(2)) && !dia.isAfter(fim.plusDays(2))
                }
                .map { it.inicio.toInstant() }

            if (instantes.isNotEmpty()) resultado[etapa] = instantes
        }

        return resultado
    }

    /** Quando começa a primeira sessão de cada etapa. */
    fun primeirasSessoes(
        etapas: List<MainActivity.Etapa>,
        sessoes: List<SessaoParaAviso>
    ): Map<MainActivity.Etapa, Instant> =
        sessoesPorEtapa(etapas, sessoes).mapValues { (_, instantes) -> instantes.minOf { it } }

    /** Quando começa a última sessão de cada etapa (em geral, a corrida). */
    fun ultimasSessoes(
        etapas: List<MainActivity.Etapa>,
        sessoes: List<SessaoParaAviso>
    ): Map<MainActivity.Etapa, Instant> =
        sessoesPorEtapa(etapas, sessoes).mapValues { (_, instantes) -> instantes.maxOf { it } }

    /**
     * A lista da tela inicial: todos os filtros aplicados. A ordem é a de quem começa mais cedo:
     * pela primeira sessão de cada etapa, quando os horários são conhecidos ([sessoes]); sem
     * eles, pelo primeiro dia. Etapas sem data vão para o fim.
     */
    fun aplicar(
        etapas: List<MainActivity.Etapa>,
        filtros: FiltrosDeEtapas,
        hoje: LocalDate,
        sessoes: List<SessaoParaAviso> = emptyList()
    ): List<MainActivity.Etapa> {
        val filtradas = base(etapas, filtros, hoje)
            .filter { etapa ->
                (filtros.paises.isEmpty() || etapa.pais in filtros.paises) &&
                    (filtros.meses.isEmpty() || mesesDaEtapa(etapa).any { it in filtros.meses })
            }

        val primeiras = primeirasSessoes(filtradas, sessoes)
        val fuso = ZoneId.systemDefault()

        return filtradas.sortedBy { etapa ->
            primeiras[etapa]
                ?: inicio(etapa)?.atStartOfDay(fuso)?.toInstant()
                ?: Instant.MAX
        }
    }

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
