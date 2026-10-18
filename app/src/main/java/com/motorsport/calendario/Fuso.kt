package com.motorsport.calendario

import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Os horários dos calendários estão em Brasília. Para mostrar na tela, convertemos para o
 * fuso que o celular está usando no momento. Os alarmes não precisam disso: eles já usam o
 * instante exato (UTC) da sessão.
 */
internal object Fuso {

    private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

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
}

internal fun F2Session.noFusoDoAparelho(): F2Session =
    Fuso.converter(data, horario)?.let { copy(data = it.first, horario = it.second) } ?: this

internal fun F3Session.noFusoDoAparelho(): F3Session =
    Fuso.converter(data, horario)?.let { copy(data = it.first, horario = it.second) } ?: this

internal fun MotoSession.noFusoDoAparelho(): MotoSession =
    Fuso.converter(data, horario)?.let { copy(data = it.first, horario = it.second) } ?: this

internal fun FormulaESession.noFusoDoAparelho(): FormulaESession =
    Fuso.converter(data, horario)?.let { copy(data = it.first, horario = it.second) } ?: this

internal fun IndyCarSession.noFusoDoAparelho(): IndyCarSession =
    Fuso.converter(data, horario)?.let { copy(data = it.first, horario = it.second) } ?: this
