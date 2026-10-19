package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZonedDateTime

class AlarmeJaDisparadoTest {

    private val brasilia = PlanoDeAvisos.BRASILIA

    // Sessão às 10:00 de Brasília.
    private val sessao = SessaoParaAviso(
        "F2", "Montreal", "Corrida",
        ZonedDateTime.of(2026, 10, 18, 10, 0, 0, 0, brasilia)
    )
    private val inicio: Instant = sessao.inicio.toInstant()

    private fun avisos(agora: Instant) =
        PlanoDeAvisos.comAlarmeNoFuturo(
            PlanoDeAvisos.calcularTodos(listOf(sessao), agora, 30),
            agora
        )

    @Test
    fun alarmeDeAntecedenciaTocaDezSegundosAntes() {
        val aviso = PlanoDeAvisos.calcular(listOf(sessao), inicio.minusSeconds(3600), 30).single()

        assertEquals(
            inicio.minusSeconds(30 * 60L + 10),
            PlanoDeAvisos.momentoDoAlarme(aviso)
        )
    }

    @Test
    fun alarmeNaHoraNaoEAdiantado() {
        val aviso = PlanoDeAvisos.calcularNaHoraDeComecar(listOf(sessao), inicio.minusSeconds(3600)).single()

        assertEquals(inicio, PlanoDeAvisos.momentoDoAlarme(aviso))
    }

    @Test
    fun comTempoDeSobraGeraOsDoisAlarmes() {
        assertEquals(2, avisos(inicio.minusSeconds(3600)).size)
    }

    @Test
    fun logoDepoisDoAlarmeDeAntecedenciaEleNaoVoltaParaAFila() {
        // O alarme tocou 10 s antes das 09:30 e o Android entregou 3 s depois: 09:29:53.
        val agora = inicio.minusSeconds(30 * 60L).minusSeconds(7)

        val restantes = avisos(agora)

        assertEquals(1, restantes.size)
        assertTrue("só sobra o aviso da hora de começar", restantes.single().naHoraDeComecar)
    }

    @Test
    fun logoDepoisDoAlarmeDaHoraEleNaoVoltaParaAFila() {
        // O alarme das 10:00 tocou com 200 ms de diferença.
        val agora = inicio.minusMillis(200)

        assertTrue(avisos(agora).isEmpty())
    }

    @Test
    fun depoisDoInicioNaoHaMaisAlarmes() {
        assertTrue(avisos(inicio.plusSeconds(30)).isEmpty())
    }
}
