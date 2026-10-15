package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
