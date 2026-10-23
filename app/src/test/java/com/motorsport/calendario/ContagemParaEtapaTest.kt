package com.motorsport.calendario

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ContagemParaEtapaTest {

    private val hoje = LocalDate.of(2026, 10, 21)

    private fun contagem(dias: Long) = ContagemParaEtapa.entre(hoje, hoje.plusDays(dias))

    @Test
    fun ate6Dias_mostraOsDias() {
        assertEquals(ContagemParaEtapa(UnidadeDeContagem.DIAS, 1), contagem(1))
        assertEquals(ContagemParaEtapa(UnidadeDeContagem.DIAS, 6), contagem(6))
    }

    @Test
    fun de7Dias_viraSemanas() {
        assertEquals(ContagemParaEtapa(UnidadeDeContagem.SEMANAS, 1), contagem(7))
        assertEquals(ContagemParaEtapa(UnidadeDeContagem.SEMANAS, 1), contagem(13))
        assertEquals(ContagemParaEtapa(UnidadeDeContagem.SEMANAS, 2), contagem(14))
        assertEquals(ContagemParaEtapa(UnidadeDeContagem.SEMANAS, 3), contagem(27))
        assertEquals(ContagemParaEtapa(UnidadeDeContagem.SEMANAS, 4), contagem(28))
    }

    @Test
    fun deUmMesDeCalendario_viraMeses() {
        val umMes = ContagemParaEtapa.entre(hoje, LocalDate.of(2026, 11, 21))
        val doisMeses = ContagemParaEtapa.entre(hoje, LocalDate.of(2026, 12, 21))

        assertEquals(ContagemParaEtapa(UnidadeDeContagem.MESES, 1), umMes)
        assertEquals(ContagemParaEtapa(UnidadeDeContagem.MESES, 2), doisMeses)
    }

    @Test
    fun umDiaAntesDeCompletarUmMes_aindaSaoSemanas() {
        val faltaUmDia = ContagemParaEtapa.entre(hoje, LocalDate.of(2026, 11, 20))

        assertEquals(ContagemParaEtapa(UnidadeDeContagem.SEMANAS, 4), faltaUmDia)
    }

    @Test
    fun dataNoPassadoOuHoje_contaComoUmDia() {
        assertEquals(ContagemParaEtapa(UnidadeDeContagem.DIAS, 1), contagem(0))
        assertEquals(ContagemParaEtapa(UnidadeDeContagem.DIAS, 1), contagem(-3))
    }
}
