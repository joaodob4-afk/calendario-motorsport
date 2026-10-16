package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormatacaoTest {

    // ---------- partirData ----------

    @Test
    fun partirData_periodoNoMesmoMes() {
        assertEquals("06–08" to "NOV", Formatacao.partirData("06–08 NOV"))
    }

    @Test
    fun partirData_periodoQueCruzaOMes() {
        assertEquals("30–01" to "OUT–NOV", Formatacao.partirData("30 OUT–01 NOV"))
    }

    @Test
    fun partirData_diaUnico() {
        assertEquals("16" to "JAN", Formatacao.partirData("16 JAN"))
    }

    @Test
    fun partirData_aceitaHifenMinusculasEEspacos() {
        assertEquals("06–08" to "NOV", Formatacao.partirData("06-08 nov"))
        assertEquals("23–25" to "OUT", Formatacao.partirData("  23–25 OUT  "))
    }

    @Test
    fun partirData_aceitaTracoLongoComEspacos() {
        assertEquals("30–01" to "OUT–NOV", Formatacao.partirData("30 OUT — 01 NOV"))
    }

    @Test
    fun partirData_aceitaMesComQuatroLetras() {
        assertEquals("12–14" to "SETE", Formatacao.partirData("12–14 SETE"))
    }

    @Test
    fun partirData_formatoDesconhecidoVoltaInteiroSemMes() {
        assertEquals("A CONFIRMAR" to "", Formatacao.partirData("a confirmar"))
        assertEquals("2026-10-23" to "", Formatacao.partirData("2026-10-23"))
        assertEquals("" to "", Formatacao.partirData(""))
    }

    @Test
    fun partirData_diaComTresDigitosNaoEReconhecido() {
        assertEquals("123–25 OUT" to "", Formatacao.partirData("123–25 OUT"))
    }

    // ---------- normalizar ----------

    @Test
    fun normalizar_removeAcentosEMinusculas() {
        assertEquals("formula e", Formatacao.normalizar("Fórmula E"))
        assertEquals("mexico", Formatacao.normalizar("MÉXICO"))
        assertEquals("monaco", Formatacao.normalizar("Mônaco"))
        assertEquals("arabia saudita", Formatacao.normalizar("Arábia Saudita"))
    }

    @Test
    fun normalizar_removeEspacosNasPontas() {
        assertEquals("sao paulo", Formatacao.normalizar("  São Paulo  "))
    }

    @Test
    fun normalizar_textoVazio() {
        assertEquals("", Formatacao.normalizar(""))
    }

    @Test
    fun normalizar_buscaSemAcentoEncontraComAcento() {
        val digitado = Formatacao.normalizar("mexico")
        assertTrue(Formatacao.normalizar("México").contains(digitado))
    }

    @Test
    fun periodo_mostraInicioEFimEmDiaMesAno() {
        assertEquals(
            "23/10/2026 – 25/10/2026",
            Formatacao.periodo(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 25))
        )
    }

    @Test
    fun periodo_queCruzaOMesNaoUsaNomeDeMes() {
        assertEquals(
            "30/10/2026 – 01/11/2026",
            Formatacao.periodo(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 1))
        )
    }

    @Test
    fun periodo_queCruzaOAnoMostraOsDoisAnos() {
        assertEquals(
            "30/12/2026 – 02/01/2027",
            Formatacao.periodo(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2))
        )
    }

    @Test
    fun periodo_usaDoisDigitosParaDiaEMes() {
        assertEquals(
            "06/03/2026 – 08/03/2026",
            Formatacao.periodo(LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 8))
        )
    }

    @Test
    fun periodo_etapaDeUmDiaMostraUmaData() {
        val dia = LocalDate.of(2026, 5, 24)
        assertEquals("24/05/2026", Formatacao.periodo(dia, dia))
    }

    @Test
    fun periodo_semFimOuComFimAntesDoInicioMostraSoOInicio() {
        val inicio = LocalDate.of(2026, 5, 24)
        assertEquals("24/05/2026", Formatacao.periodo(inicio, null))
        assertEquals("24/05/2026", Formatacao.periodo(inicio, LocalDate.of(2026, 5, 20)))
    }

    @Test
    fun periodo_semInicioDevolveNull() {
        assertNull(Formatacao.periodo(null, LocalDate.of(2026, 5, 24)))
        assertNull(Formatacao.periodo(null, null))
    }
}
