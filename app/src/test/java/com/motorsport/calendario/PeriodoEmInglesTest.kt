package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PeriodoEmInglesTest {

    private fun d(mes: Int, dia: Int, ano: Int = 2026) = LocalDate.of(ano, mes, dia)

    @Test
    fun mesmoMesMostraOMesUmaVez() {
        assertEquals("Oct 23–25", Formatacao.periodoEmIngles(d(10, 23), d(10, 25)))
    }

    @Test
    fun mesesDiferentesMostramOsDois() {
        assertEquals("Oct 30 – Nov 1", Formatacao.periodoEmIngles(d(10, 30), d(11, 1)))
    }

    @Test
    fun diaUnicoOuSemFimMostraSoOInicio() {
        assertEquals("Oct 23", Formatacao.periodoEmIngles(d(10, 23), d(10, 23)))
        assertEquals("Oct 23", Formatacao.periodoEmIngles(d(10, 23), null))
    }

    @Test
    fun fimAntesDoInicioEIgnorado() {
        assertEquals("Oct 23", Formatacao.periodoEmIngles(d(10, 23), d(10, 20)))
    }

    @Test
    fun virandoOAnoMostraOsDoisMeses() {
        assertEquals("Dec 30 – Jan 2", Formatacao.periodoEmIngles(d(12, 30), d(1, 2, 2027)))
    }

    @Test
    fun semDataDeInicioNaoGeraTexto() {
        assertNull(Formatacao.periodoEmIngles(null, d(10, 25)))
    }
}
