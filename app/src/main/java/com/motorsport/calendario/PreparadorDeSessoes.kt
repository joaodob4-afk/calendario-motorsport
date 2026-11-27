package com.motorsport.calendario

import java.util.Date

/** Prepara a chave de ordenação uma única vez por sessão, antes de montar a interface. */
internal object PreparadorDeSessoes {
    internal data class Preparada<T>(val sessao: T, val data: Date?, val minutos: Int)

    fun <T> ordenar(sessoes: List<T>, obterData: (T) -> Date?, obterHorario: (T) -> String): List<Preparada<T>> =
        sessoes.map { sessao -> Preparada(sessao, obterData(sessao), extrairMinutos(obterHorario(sessao))) }
            .sortedWith(compareBy<Preparada<T>> { it.data?.time ?: Long.MAX_VALUE }.thenBy { it.minutos })

    private fun extrairMinutos(horario: String): Int {
        val partes = horario.trim().split(":")
        if (partes.size < 2) return Int.MAX_VALUE
        return runCatching { partes[0].toInt() * 60 + partes[1].take(2).toInt() }.getOrDefault(Int.MAX_VALUE)
    }
}
