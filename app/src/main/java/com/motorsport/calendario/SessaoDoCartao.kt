package com.motorsport.calendario

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Qual sessão o card de "próximo evento" mostra e quando o evento deixa o card.
 *
 * As fontes só informam o INÍCIO de cada sessão, então o fim é estimado pelo tipo da sessão
 * ([duracaoEstimada]). Sem horários confirmados, nada aqui se aplica e o card segue a regra
 * antiga, por data. Não depende do Android para poder ser testado.
 */
internal object SessaoDoCartao {
    private val horasNoNome = Regex("""(\d+)\s*horas""", RegexOption.IGNORE_CASE)

    /** Duração aproximada: vale só para decidir quando passar para a sessão seguinte. */
    fun duracaoEstimada(sessao: SessaoParaAviso): Duration {
        val nome = Formatacao.normalizar(sessao.nome)
        return when {
            "warm" in nome -> Duration.ofMinutes(20)
            "sprint" in nome && "classificacao" !in nome &&
                "qualifying" !in nome && "shootout" !in nome -> Duration.ofMinutes(45)
            "corrida" in nome || "race" in nome -> duracaoDaCorrida(sessao)
            else -> Duration.ofMinutes(60)
        }
    }

    private fun duracaoDaCorrida(sessao: SessaoParaAviso): Duration =
        when (Formatacao.normalizar(sessao.categoria)) {
            "wec" -> duracaoDoWec(sessao.evento)
            "motogp", "moto2", "moto3" -> Duration.ofMinutes(60)
            "indycar" -> Duration.ofHours(3)
            "f1" -> Duration.ofHours(2)
            else -> Duration.ofMinutes(90)
        }

    /** "6 Horas de Imola" dura 6 h; "1812 km do Catar", cerca de 10 h; sem pista, 6 h. */
    private fun duracaoDoWec(evento: String): Duration {
        val horas = horasNoNome.find(evento)?.groupValues?.get(1)?.toLongOrNull()
        return when {
            horas != null && horas > 0 -> Duration.ofHours(horas)
            evento.contains("km", ignoreCase = true) -> Duration.ofHours(10)
            else -> Duration.ofHours(6)
        }
    }

    /** "29 min", "14h 29min", "2d 5h": tempo curto para a contagem regressiva do card. */
    fun duracaoCurta(duracao: Duration): String {
        val minutosTotais = duracao.toMinutes().coerceAtLeast(1)
        val dias = minutosTotais / (24 * 60)
        val horas = (minutosTotais % (24 * 60)) / 60
        val minutos = minutosTotais % 60
        return when {
            dias > 0 -> if (horas > 0) "${dias}d ${horas}h" else "${dias}d"
            horas > 0 -> if (minutos > 0) "${horas}h ${minutos}min" else "${horas}h"
            else -> "$minutos min"
        }
    }

    fun fim(sessao: SessaoParaAviso): Instant =
        sessao.inicio.toInstant().plus(duracaoEstimada(sessao))

    /** Sessões da categoria que caem entre [inicio] e [fim] (datas no fuso [zona]), em ordem. */
    fun sessoesDaEtapa(
        todas: List<SessaoParaAviso>,
        categoria: String,
        inicio: LocalDate,
        fim: LocalDate,
        zona: ZoneId
    ): List<SessaoParaAviso> {
        val alvo = Formatacao.normalizar(categoria)
        return todas
            .filter { Formatacao.normalizar(it.categoria) == alvo }
            .filter {
                val dia = it.inicio.withZoneSameInstant(zona).toLocalDate()
                !dia.isBefore(inicio) && !dia.isAfter(fim)
            }
            .sortedBy { it.inicio.toInstant() }
    }

    /** A sessão em andamento ou, se nenhuma estiver, a próxima. Null quando todas já acabaram. */
    fun atual(sessoes: List<SessaoParaAviso>, agora: Instant): SessaoParaAviso? =
        sessoes.firstOrNull { fim(it).isAfter(agora) }

    /**
     * O evento acabou? Só quando a última sessão conhecida já terminou E ela é do último dia do
     * evento. Se faltam horários por confirmar (a última sessão conhecida é de um dia anterior),
     * o evento continua valendo até a data de fim, como antes.
     */
    fun encerrado(
        sessoes: List<SessaoParaAviso>,
        fimDoEvento: LocalDate,
        agora: Instant,
        zona: ZoneId
    ): Boolean {
        val ultima = sessoes.maxByOrNull { it.inicio.toInstant() } ?: return false
        val diaDaUltima = ultima.inicio.withZoneSameInstant(zona).toLocalDate()
        return !diaDaUltima.isBefore(fimDoEvento) && !fim(ultima).isAfter(agora)
    }
}
