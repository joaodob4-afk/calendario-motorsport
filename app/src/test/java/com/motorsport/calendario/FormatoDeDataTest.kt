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
        assertEquals("05/10", FormatadorDeData.curta(data, FormatoDeData.DIA_MES, ingles))
        assertEquals("10/05", FormatadorDeData.curta(data, FormatoDeData.MES_DIA, ingles))
        assertEquals("05 OCT", FormatadorDeData.curta(data, FormatoDeData.DIA_MES_TEXTO, ingles))
        assertEquals("OCT 05", FormatadorDeData.curta(data, FormatoDeData.MES_TEXTO_DIA, ingles))
        assertEquals("05 OUT", FormatadorDeData.curta(data, FormatoDeData.DIA_MES_TEXTO, portugues))
    }

    @Test
    fun dataCompleta_emCadaFormato() {
        assertEquals("05/10/2026", FormatadorDeData.completa(data, FormatoDeData.DIA_MES, ingles))
        assertEquals("10/05/2026", FormatadorDeData.completa(data, FormatoDeData.MES_DIA, ingles))
        assertEquals("05 OCT 2026", FormatadorDeData.completa(data, FormatoDeData.DIA_MES_TEXTO, ingles))
        assertEquals("OCT 05, 2026", FormatadorDeData.completa(data, FormatoDeData.MES_TEXTO_DIA, ingles))
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
