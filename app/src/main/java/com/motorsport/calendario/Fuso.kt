package com.motorsport.calendario

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit

/**
 * Os horários dos calendários estão em Brasília. Para mostrar na tela, convertemos para o
 * fuso que o celular está usando no momento. Os alarmes não precisam disso: eles já usam o
 * instante exato (UTC) da sessão.
 */
internal object Fuso {

    private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

    private val formatoDataEstrito = DateTimeFormatter
        .ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    /** Fuso atual do aparelho (acompanha viagens e mudança de horário de verão). */
    fun doAparelho(): ZoneId = ZoneId.systemDefault()

    /**
     * Data (dd/MM/yyyy) e hora (HH:mm) da sessão no fuso [destino]. A data também pode mudar
     * (20:00 em Brasília já é 08:00 do dia seguinte no Japão). Devolve null se o horário
     * não estiver confirmado; quem chama mantém então os textos originais.
     */
    fun converter(
        data: String,
        horario: String,
        destino: ZoneId = doAparelho()
    ): Pair<String, String>? {
        val inicio = PlanoDeAvisos.inicioDaSessao(data, horario) ?: return null
        val local = inicio.withZoneSameInstant(destino)
        return local.format(formatoData) to local.format(formatoHora)
    }

    /**
     * Início e fim da etapa (dd/MM/yyyy) como o celular os vê, tirados das próprias sessões:
     * do dia da primeira ao dia da última, já no fuso do aparelho. É o que bate com a
     * programação mostrada na página da etapa. As datas cadastradas na etapa nem sempre servem
     * (nas corridas da Ásia e da Austrália elas são as do circuito, e as sessões em Brasília
     * caem um dia antes). As [sessoes] são pares (data, horário) em Brasília; uma sessão sem
     * horário confirmado entra com a data cadastrada. Se nenhuma sessão tiver horário
     * confirmado, devolve [inicio] e [fim] como estão.
     */
    fun periodoPelasSessoes(
        inicio: String,
        fim: String,
        sessoes: List<Pair<String, String>>,
        destino: ZoneId = doAparelho()
    ): Pair<String, String> {
        val temConfirmada = sessoes.any { (data, hora) ->
            PlanoDeAvisos.inicioDaSessao(data, hora) != null
        }
        if (!temConfirmada) return inicio to fim

        val datas = sessoes.mapNotNull { (data, hora) ->
            lerData(converter(data, hora, destino)?.first ?: data)
        }
        val primeira = datas.minOrNull() ?: return inicio to fim
        val ultima = datas.maxOrNull() ?: return inicio to fim

        return primeira.format(formatoDataEstrito) to ultima.format(formatoDataEstrito)
    }

    /**
     * Para etapas cujo fim é depois do início da última sessão (as 24 Horas de Le Mans, por
     * exemplo): mantém as datas cadastradas e só as desloca pelos mesmos dias que a primeira
     * e a última sessão mudam ao passar de Brasília para o fuso do aparelho.
     */
    fun periodo(
        inicio: String,
        fim: String,
        sessoes: List<Pair<String, String>>,
        destino: ZoneId = doAparelho()
    ): Pair<String, String> {
        val dataInicio = lerData(inicio) ?: return inicio to fim
        val dataFim = lerData(fim) ?: return inicio to fim

        val horarios = sessoes.mapNotNull { (data, hora) -> PlanoDeAvisos.inicioDaSessao(data, hora) }
        val primeira = horarios.minByOrNull { it.toInstant() } ?: return inicio to fim
        val ultima = horarios.maxByOrNull { it.toInstant() } ?: return inicio to fim

        val novoInicio = dataInicio.plusDays(deslocamentoEmDias(primeira, destino))
        var novoFim = dataFim.plusDays(deslocamentoEmDias(ultima, destino))
        if (novoFim.isBefore(novoInicio)) novoFim = novoInicio

        return novoInicio.format(formatoDataEstrito) to novoFim.format(formatoDataEstrito)
    }

    /**
     * Dia, no fuso [destino], de um horário da API da F1 (data "yyyy-MM-dd" e hora com fuso,
     * como "13:00:00Z"). Sem hora válida devolve a própria data; null se a data for inválida.
     */
    fun dataDaF1(data: String, hora: String, destino: ZoneId = doAparelho()): LocalDate? {
        val dia = try {
            LocalDate.parse(data.trim())
        } catch (e: Exception) {
            return null
        }

        if (hora.isBlank()) return dia

        return try {
            OffsetDateTime.parse("${data.trim()}T${hora.trim()}")
                .atZoneSameInstant(destino)
                .toLocalDate()
        } catch (e: Exception) {
            dia
        }
    }

    private fun deslocamentoEmDias(horarioEmBrasilia: ZonedDateTime, destino: ZoneId): Long =
        ChronoUnit.DAYS.between(
            horarioEmBrasilia.toLocalDate(),
            horarioEmBrasilia.withZoneSameInstant(destino).toLocalDate()
        )

    private fun lerData(texto: String): LocalDate? =
        try {
            LocalDate.parse(texto.trim(), formatoDataEstrito)
        } catch (e: Exception) {
            null
        }
}

/** Os eventos abaixo trazem início e fim já no fuso do celular (as sessões continuam em Brasília). */
internal fun FormulaJuniorEvent.noFusoDoAparelho(): FormulaJuniorEvent =
    Fuso.periodoPelasSessoes(inicio, fim, sessoes.map { it.data to it.horario })
        .let { copy(inicio = it.first, fim = it.second) }

internal fun MotoEvent.noFusoDoAparelho(): MotoEvent =
    Fuso.periodoPelasSessoes(inicio, fim, sessoes.map { it.data to it.horario })
        .let { copy(inicio = it.first, fim = it.second) }

/** Versão para o WEC, que tem corridas longas (ver [Fuso.periodo]). */
internal fun MotoEvent.noFusoDoAparelhoMantendoDatas(): MotoEvent =
    Fuso.periodo(inicio, fim, sessoes.map { it.data to it.horario })
        .let { copy(inicio = it.first, fim = it.second) }

internal fun FormulaEEvent.noFusoDoAparelho(): FormulaEEvent =
    Fuso.periodoPelasSessoes(inicio, fim, sessoes.map { it.data to it.horario })
        .let { copy(inicio = it.first, fim = it.second) }

internal fun IndyCarEvent.noFusoDoAparelho(): IndyCarEvent =
    Fuso.periodoPelasSessoes(inicio, fim, sessoes.map { it.data to it.horario })
        .let { copy(inicio = it.first, fim = it.second) }

internal fun FormulaJuniorSession.noFusoDoAparelho(): FormulaJuniorSession =
    Fuso.converter(data, horario)?.let { copy(data = it.first, horario = it.second) } ?: this

internal fun MotoSession.noFusoDoAparelho(): MotoSession =
    Fuso.converter(data, horario)?.let { copy(data = it.first, horario = it.second) } ?: this

internal fun FormulaESession.noFusoDoAparelho(): FormulaESession =
    Fuso.converter(data, horario)?.let { copy(data = it.first, horario = it.second) } ?: this

internal fun IndyCarSession.noFusoDoAparelho(): IndyCarSession =
    Fuso.converter(data, horario)?.let { copy(data = it.first, horario = it.second) } ?: this
