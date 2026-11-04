package com.motorsport.calendario

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProximaSessaoWidgetTest {

    private val agora = ZonedDateTime.of(
        2026, 11, 4, 12, 0, 0, 0,
        ZoneId.of("America/Sao_Paulo")
    )

    private fun sessao(nome: String, horas: Long) = SessaoParaAviso(
        categoria = "F1",
        local = "São Paulo",
        nome = nome,
        inicio = agora.plusHours(horas)
    )

    @Test
    fun selecionaASessaoFuturaMaisProxima() {
        val maisTarde = sessao("Corrida", 8)
        val maisCedo = sessao("Treino", 2)

        assertEquals(
            maisCedo,
            ProximaSessaoWidget.selecionarProxima(
                listOf(maisTarde, maisCedo),
                agora.toInstant()
            )
        )
    }

    @Test
    fun ignoraSessoesQueJaComecaram() {
        val passada = sessao("Treino", -1)
        val futura = sessao("Corrida", 3)

        assertEquals(
            futura,
            ProximaSessaoWidget.selecionarProxima(
                listOf(passada, futura),
                agora.toInstant()
            )
        )
    }

    @Test
    fun retornaNuloQuandoNaoHaSessoesFuturas() {
        assertNull(
            ProximaSessaoWidget.selecionarProxima(
                listOf(sessao("Corrida", -1)),
                agora.toInstant()
            )
        )
    }
}
