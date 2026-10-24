package com.motorsport.calendario

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UltimaSessaoTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    private val etapa = MainActivity.Etapa(
        categoria = "F1",
        pais = "X",
        circuito = "Austin",
        data = "",
        dataInicio = "2026-10-23",
        dataFim = "2026-10-25"
    )

    private fun sessao(dia: Int, hora: Int, categoria: String = "F1") =
        SessaoParaAviso(categoria, "local", "Sessão", ZonedDateTime.of(2026, 10, dia, hora, 0, 0, 0, saoPaulo))

    @Test
    fun primeiraEUltimaSessaoSaoOsExtremosDaEtapa() {
        val sessoes = listOf(sessao(24, 16), sessao(23, 14), sessao(25, 17), sessao(25, 10))

        assertEquals(sessoes[1].inicio.toInstant(), RegrasDeFiltro.primeirasSessoes(listOf(etapa), sessoes)[etapa])
        assertEquals(sessoes[2].inicio.toInstant(), RegrasDeFiltro.ultimasSessoes(listOf(etapa), sessoes)[etapa])
    }

    @Test
    fun comUmaSoSessao_primeiraEUltimaSaoAMesma() {
        val sessoes = listOf(sessao(24, 16))

        assertEquals(
            RegrasDeFiltro.primeirasSessoes(listOf(etapa), sessoes)[etapa],
            RegrasDeFiltro.ultimasSessoes(listOf(etapa), sessoes)[etapa]
        )
    }

    @Test
    fun semSessoesDaCategoria_naoHaResultado() {
        val sessoes = listOf(sessao(24, 16, categoria = "MotoGP"))

        assertTrue(RegrasDeFiltro.ultimasSessoes(listOf(etapa), sessoes).isEmpty())
    }
}
