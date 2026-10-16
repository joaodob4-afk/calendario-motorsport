package com.motorsport.calendario

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Uma sessão com horário confirmado, pronta para receber um aviso. */
internal data class SessaoParaAviso(
    val categoria: String,
    val local: String,
    val nome: String,
    val inicio: ZonedDateTime
) {
    /** Identifica a sessão. Se o horário mudar, vira outra chave e o aviso antigo some. */
    val chave: String
        get() = "$categoria|$local|$nome|${inicio.toInstant().toEpochMilli()}"

    /** Serve de código do alarme e de id da notificação. */
    val codigo: Int
        get() = chave.hashCode()
}

internal data class AvisoPlanejado(
    val sessao: SessaoParaAviso,
    val dispararEm: Instant
)

/** O que avisar de uma categoria. */
internal enum class ModoDeAviso(val rotulo: String) {
    TODAS("Todas"),
    CORRIDAS("Corridas"),
    NENHUMA("Nenhuma");

    fun permite(nomeDaSessao: String): Boolean =
        when (this) {
            TODAS -> true
            CORRIDAS -> PlanoDeAvisos.ehCorrida(nomeDaSessao)
            NENHUMA -> false
        }
}

/**
 * Regras dos avisos, sem dependência do Android, para poderem ser testadas
 * automaticamente. Quem agenda de verdade é o [AgendadorDeAvisos].
 */
internal object PlanoDeAvisos {

    /** Os horários dos calendários do app estão em Brasília. */
    val BRASILIA: ZoneId = ZoneId.of("America/Sao_Paulo")

    /** Só são agendados os avisos dos próximos dias; o restante entra nas próximas aberturas. */
    const val HORIZONTE_DIAS = 21L

    /** O Android limita a quantidade de alarmes por app, então o plano nunca passa disto. */
    const val LIMITE_DE_ALARMES = 40

    /** Um aviso que chega com mais atraso que isto (aparelho desligado, por exemplo) é descartado. */
    const val TOLERANCIA_ATRASO_MINUTOS = 5L

    private val formatoData = DateTimeFormatter
        .ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    private val formatoHora = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")

    /** Início em Brasília, ou null se a data ou o horário não estiverem confirmados ("A confirmar"). */
    fun inicioDaSessao(data: String, horario: String): ZonedDateTime? {
        val hora = formatoHora.matchEntire(horario.trim()) ?: return null

        return try {
            LocalDate.parse(data.trim(), formatoData)
                .atTime(hora.groupValues[1].toInt(), hora.groupValues[2].toInt())
                .atZone(BRASILIA)
        } catch (e: Exception) {
            // Data inexistente (31/02, por exemplo): a sessão simplesmente não recebe aviso.
            // Sem Log aqui para esta classe poder ser testada sem o Android.
            null
        }
    }

    /**
     * Avisos que ainda vão acontecer dentro do horizonte, do mais próximo ao mais distante.
     * [minutosAntes] igual a zero (ou negativo) significa "não avisar".
     * [modoDaCategoria] diz, para cada categoria, se vale todas as sessões, só corridas ou nenhuma.
     */
    fun calcular(
        sessoes: List<SessaoParaAviso>,
        agora: Instant,
        minutosAntes: Int,
        horizonte: Duration = Duration.ofDays(HORIZONTE_DIAS),
        limite: Int = LIMITE_DE_ALARMES,
        modoDaCategoria: (String) -> ModoDeAviso = { ModoDeAviso.TODAS }
    ): List<AvisoPlanejado> {
        if (minutosAntes <= 0) return emptyList()

        val antecedencia = Duration.ofMinutes(minutosAntes.toLong())
        val fimDaJanela = agora.plus(horizonte)

        return sessoes
            .filter { modoDaCategoria(it.categoria).permite(it.nome) }
            .distinctBy { it.chave }
            .map { AvisoPlanejado(it, it.inicio.toInstant().minus(antecedencia)) }
            .filter { it.dispararEm.isAfter(agora) && !it.dispararEm.isAfter(fimDaJanela) }
            .sortedBy { it.dispararEm }
            .take(limite)
    }

    /**
     * Corridas são as sessões cujo nome começa com "Corrida": corrida principal, sprint,
     * feature e as rodadas da Fórmula E. Treino, classificação, warm-up e shows não entram.
     */
    fun ehCorrida(nome: String): Boolean =
        Formatacao.normalizar(nome).startsWith("corrida")

    /** Falso quando a sessão já começou há tempo demais para o aviso ainda fazer sentido. */
    fun aindaVale(milissegundosAteInicio: Long): Boolean =
        milissegundosAteInicio >= -TOLERANCIA_ATRASO_MINUTOS * 60_000L

    /** "Começa em 30 min", "Começa em 1 h 15 min" ou "Começando agora". */
    fun textoDeAntecedencia(minutosRestantes: Long): String {
        if (minutosRestantes <= 0L) return "Começando agora"

        if (minutosRestantes < 60L) return "Começa em $minutosRestantes min"

        val horas = minutosRestantes / 60L
        val minutos = minutosRestantes % 60L

        return if (minutos == 0L) {
            "Começa em $horas h"
        } else {
            "Começa em $horas h $minutos min"
        }
    }
}
