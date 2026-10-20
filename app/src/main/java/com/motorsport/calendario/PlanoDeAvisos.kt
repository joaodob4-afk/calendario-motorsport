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
    val inicio: ZonedDateTime,
    /** Nome do evento (\"GP da Austrália\"), só para exibir. Não entra na chave do aviso. */
    val evento: String = ""
) {
    /** Identifica a sessão. Se o horário mudar, vira outra chave e o aviso antigo some. */
    val chave: String
        get() = "$categoria|$local|$nome|${inicio.toInstant().toEpochMilli()}"

    /** Serve de código do alarme e de id da notificação. */
    val codigo: Int
        get() = chave.hashCode()

    /** Código do alarme da hora de início (diferente do alarme de antecedência). */
    val codigoDoInicio: Int
        get() = "$chave|inicio".hashCode()
}

internal data class AvisoPlanejado(
    val sessao: SessaoParaAviso,
    val dispararEm: Instant,
    /** Verdadeiro para o aviso "começando agora", que dispara na hora exata da sessão. */
    val naHoraDeComecar: Boolean = false,
    /** Antecedência deste aviso em minutos (zero no aviso da hora de começar). */
    val minutosAntes: Int = 0
) {
    /**
     * Código do alarme no Android. Cada aviso da mesma sessão tem o seu: o da hora de
     * começar e um para cada antecedência escolhida.
     */
    val codigoDoAlarme: Int
        get() = when {
            naHoraDeComecar -> sessao.codigoDoInicio
            minutosAntes > 0 -> "${sessao.chave}|antes|$minutosAntes".hashCode()
            else -> sessao.codigo
        }
}

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

    /**
     * O Android limita a quantidade de alarmes por app. Agendamos só os próximos, em
     * qualquer data; os seguintes entram sozinhos a cada aviso mostrado e a cada
     * abertura do app.
     */
    const val LIMITE_DE_ALARMES = 50

    /**
     * O aviso de antecedência dispara um pouco antes do horário calculado, porque o Android
     * costuma entregar o alarme alguns segundos depois. O "começando agora" não é adiantado.
     */
    const val ADIANTAMENTO_DO_AVISO_ANTECIPADO_MS = 10_000L

    /**
     * Alarmes que disparariam dentro desta margem são ignorados ao refazer a fila. Sem isso, o
     * alarme que acabou de tocar seria agendado de novo para um instante já passado, tocaria
     * outra vez na hora e repetiria a notificação várias vezes.
     */
    const val MARGEM_MINIMA_DO_ALARME_MS = 2_000L

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
     * Próximos avisos que ainda vão acontecer, do mais próximo ao mais distante.
     * [minutosAntes] igual a zero (ou negativo) significa "não avisar".
     * [modoDaCategoria] diz, para cada categoria, se vale todas as sessões, só corridas ou nenhuma.
     */
    fun calcular(
        sessoes: List<SessaoParaAviso>,
        agora: Instant,
        minutosAntes: Int,
        limite: Int = LIMITE_DE_ALARMES,
        modoDaCategoria: (String) -> ModoDeAviso = { ModoDeAviso.TODAS }
    ): List<AvisoPlanejado> =
        calcular(sessoes, agora, listOf(minutosAntes), limite, modoDaCategoria)

    /**
     * Igual ao anterior, mas com várias antecedências ao mesmo tempo (15 e 45 minutos, por
     * exemplo): cada sessão recebe um aviso para cada uma. Valores repetidos ou menores que
     * 1 são ignorados; sem nenhum valor válido, não há avisos.
     */
    fun calcular(
        sessoes: List<SessaoParaAviso>,
        agora: Instant,
        minutosAntes: Collection<Int>,
        limite: Int = LIMITE_DE_ALARMES,
        modoDaCategoria: (String) -> ModoDeAviso = { ModoDeAviso.TODAS }
    ): List<AvisoPlanejado> {
        val antecedencias = minutosAntes.filter { it > 0 }.distinct()
        if (antecedencias.isEmpty()) return emptyList()

        val permitidas = sessoes
            .filter { modoDaCategoria(it.categoria).permite(it.nome) }
            .distinctBy { it.chave }

        return antecedencias
            .flatMap { minutos ->
                val antecedencia = Duration.ofMinutes(minutos.toLong())
                permitidas.map {
                    AvisoPlanejado(it, it.inicio.toInstant().minus(antecedencia), minutosAntes = minutos)
                }
            }
            .filter { it.dispararEm.isAfter(agora) }
            .sortedBy { it.dispararEm }
            .take(limite)
    }

    /**
     * Avisos "começando agora": um por sessão, na hora exata do início. Respeita o modo de
     * cada categoria, como [calcular]. Não depende da antecedência escolhida.
     */
    fun calcularNaHoraDeComecar(
        sessoes: List<SessaoParaAviso>,
        agora: Instant,
        limite: Int = LIMITE_DE_ALARMES,
        modoDaCategoria: (String) -> ModoDeAviso = { ModoDeAviso.TODAS }
    ): List<AvisoPlanejado> =
        sessoes
            .filter { modoDaCategoria(it.categoria).permite(it.nome) }
            .distinctBy { it.chave }
            .map { AvisoPlanejado(it, it.inicio.toInstant(), naHoraDeComecar = true) }
            .filter { it.dispararEm.isAfter(agora) }
            .sortedBy { it.dispararEm }
            .take(limite)

    /**
     * Junta os avisos de antecedência e os da hora de começar, do mais próximo ao mais
     * distante, respeitando o limite total de alarmes.
     */
    fun calcularTodos(
        sessoes: List<SessaoParaAviso>,
        agora: Instant,
        minutosAntes: Int,
        avisarNaHora: Boolean = true,
        limite: Int = LIMITE_DE_ALARMES,
        modoDaCategoria: (String) -> ModoDeAviso = { ModoDeAviso.TODAS }
    ): List<AvisoPlanejado> =
        calcularTodos(sessoes, agora, listOf(minutosAntes), avisarNaHora, limite, modoDaCategoria)

    /**
     * Versão com várias antecedências. Sem nenhuma antecedência válida, sobra só o aviso da
     * hora de começar (se [avisarNaHora]); sem os dois, não há avisos.
     */
    fun calcularTodos(
        sessoes: List<SessaoParaAviso>,
        agora: Instant,
        minutosAntes: Collection<Int>,
        avisarNaHora: Boolean = true,
        limite: Int = LIMITE_DE_ALARMES,
        modoDaCategoria: (String) -> ModoDeAviso = { ModoDeAviso.TODAS }
    ): List<AvisoPlanejado> {
        val antes = calcular(sessoes, agora, minutosAntes, limite, modoDaCategoria)
        val naHora = if (avisarNaHora) {
            calcularNaHoraDeComecar(sessoes, agora, limite, modoDaCategoria)
        } else {
            emptyList()
        }

        return (antes + naHora)
            .sortedWith(compareBy<AvisoPlanejado>({ it.dispararEm }, { it.naHoraDeComecar }))
            .take(limite)
    }

    /** Instante em que o alarme deve tocar no Android (já com o adiantamento, se houver). */
    fun momentoDoAlarme(aviso: AvisoPlanejado): Instant =
        if (aviso.naHoraDeComecar) {
            aviso.dispararEm
        } else {
            aviso.dispararEm.minusMillis(ADIANTAMENTO_DO_AVISO_ANTECIPADO_MS)
        }

    /** Só os avisos cujo alarme ainda está no futuro (com a margem de segurança). */
    fun comAlarmeNoFuturo(avisos: List<AvisoPlanejado>, agora: Instant): List<AvisoPlanejado> =
        avisos.filter {
            momentoDoAlarme(it).isAfter(agora.plusMillis(MARGEM_MINIMA_DO_ALARME_MS))
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