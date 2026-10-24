package com.motorsport.calendario

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoDeDataTest {

    private val data = LocalDate.of(2026, 10, 5)
    private val ingles = Locale.ENGLISH
    private val portugues = Locale("pt", "BR")

    @Test
    fun padraoDoAparelho_viraDiaMesOuMesDia() {
        assertEquals(FormatoDeData.DIA_MES, FormatadorDeData.resolver(FormatoDeData.SISTEMA, false))
        assertEquals(FormatoDeData.MES_DIA, FormatadorDeData.resolver(FormatoDeData.SISTEMA, true))
    }

    @Test
    fun formatoEscolhidoNaoDependeDoAparelho() {
        FormatoDeData.values().filter { it != FormatoDeData.SISTEMA }.forEach {
            assertEquals(it, FormatadorDeData.resolver(it, true))
            assertEquals(it, FormatadorDeData.resolver(it, false))
        }
    }

    @Test
    fun dataCurta_emCadaFormato() {
        assertEquals("05/10", FormatadorDeData.curta(data, FormatoDeData.DIA_MES))
        assertEquals("10/05", FormatadorDeData.curta(data, FormatoDeData.MES_DIA))
    }

    @Test
    fun dataCompleta_emCadaFormato() {
        assertEquals("05/10/2026", FormatadorDeData.completa(data, FormatoDeData.DIA_MES))
        assertEquals("10/05/2026", FormatadorDeData.completa(data, FormatoDeData.MES_DIA))
    }

    @Test
    fun soHaTresOpcoes_padraoDoAparelhoDiaMesEMesDia() {
        assertEquals(
            listOf(FormatoDeData.SISTEMA, FormatoDeData.DIA_MES, FormatoDeData.MES_DIA),
            FormatoDeData.values().toList()
        )
    }

    @Test
    fun escolhaAntigaComMesAbreviadoVoltaAoPadraoDoAparelho() {
        assertEquals(FormatoDeData.SISTEMA, FormatoDeData.deChave("dia_mes_texto"))
        assertEquals(FormatoDeData.SISTEMA, FormatoDeData.deChave("mes_texto_dia"))
    }

    @Test
    fun diaDaSemana_abreviadoComInicialMaiuscula() {
        val sexta = LocalDate.of(2026, 10, 23)

        assertEquals("Fri", FormatadorDeData.diaDaSemana(sexta, ingles))
        assertEquals("Sex", FormatadorDeData.diaDaSemana(sexta, portugues))
    }

    @Test
    fun chavesVoltamParaOFormatoEValorEstranhoUsaOPadrao() {
        FormatoDeData.values().forEach {
            assertEquals(it, FormatoDeData.deChave(it.chave))
        }
        assertEquals(FormatoDeData.SISTEMA, FormatoDeData.deChave(null))
        assertEquals(FormatoDeData.SISTEMA, FormatoDeData.deChave("outro"))
    }
}
