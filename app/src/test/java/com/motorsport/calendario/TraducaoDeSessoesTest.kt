package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Test

class TraducaoDeSessoesTest {

    private fun t(nome: String) = TraducaoDeSessoes.paraIngles(nome)

    @Test
    fun traduzNomesComuns() {
        assertEquals("Free Practice 1", t("Treino Livre 1"))
        assertEquals("Qualifying", t("Classificação"))
        assertEquals("Sprint Qualifying", t("Classificação Sprint"))
        assertEquals("Race", t("Corrida"))
        assertEquals("Sprint Race", t("Corrida Sprint"))
        assertEquals("Feature Race 2", t("Corrida Feature 2"))
        assertEquals("Final Practice", t("Treino Final"))
    }

    @Test
    fun traduzNomesComComplemento() {
        assertEquals("Race (Round 7)", t("Corrida (Rodada 7)"))
        assertEquals("Qualifying Hypercar", t("Classificação Hypercar"))
        assertEquals("Pit Stop Competition", t("Competição de Pit Stop"))
        assertEquals("Final Practice (Carb Day)", t("Treino Final (Carb Day)"))
    }

    @Test
    fun nomesJaEmInglesOuDesconhecidosFicamIguaisAoTraduzirParaIngles() {
        listOf("Practice", "Warm-up", "Fast Friday", "Pre-Race Show", "Hyperpole 1 Hypercar")
            .forEach { assertEquals(it, t(it)) }
    }

    @Test
    fun traduzNomesEmInglesParaPortugues() {
        assertEquals("Treino Livre 1", TraducaoDeSessoes.paraPortugues("Free Practice 1"))
        assertEquals("Classificação", TraducaoDeSessoes.paraPortugues("Qualifying"))
        assertEquals("Corrida Sprint", TraducaoDeSessoes.paraPortugues("Sprint Race"))
        assertEquals("Treino 2", TraducaoDeSessoes.paraPortugues("Practice 2"))
        assertEquals("Aquecimento", TraducaoDeSessoes.paraPortugues("Warm-up"))
    }

    @Test
    fun traduzTodasAsSessoesDaF1AcademyParaIngles() {
        assertEquals("Free Practice", t("Treino Livre"))
        assertEquals("Qualifying", t("Classificação"))
        assertEquals("Reverse Grid Race", t("Corrida Grid Invertido"))
        assertEquals("Feature Race", t("Corrida Feature"))
        assertEquals("Opening Race", t("Corrida de Abertura"))
    }

    @Test
    fun traduzTodasAsSessoesDaF1AcademyParaPortugues() {
        assertEquals("Treino Livre", TraducaoDeSessoes.paraPortugues("Free Practice"))
        assertEquals("Classificação", TraducaoDeSessoes.paraPortugues("Qualifying"))
        assertEquals("Corrida Grid Invertido", TraducaoDeSessoes.paraPortugues("Reverse Grid Race"))
        assertEquals("Corrida Feature", TraducaoDeSessoes.paraPortugues("Feature Race"))
        assertEquals("Corrida de Abertura", TraducaoDeSessoes.paraPortugues("Opening Race"))
    }

    @Test
    fun normalizaEspacosNosNomesDasSessoesDaF1Academy() {
        assertEquals("Reverse Grid Race", t("  Corrida   Grid Invertido  "))
        assertEquals("Feature Race", t("Corrida\u00A0Feature"))
        assertEquals("Corrida Grid Invertido", TraducaoDeSessoes.paraPortugues("Reverse   Grid Race"))
    }

    @Test
    fun mantemNomesEmPortuguesAoTraduzirParaPortugues() {
        assertEquals("Treino Livre 1", TraducaoDeSessoes.paraPortugues("Treino Livre 1"))
        assertEquals("Corrida", TraducaoDeSessoes.paraPortugues("Corrida"))
    }

    @Test
    fun naoSubstituiTermosDentroDeNomesCompostos() {
        assertEquals("Pre-Race Show", TraducaoDeSessoes.paraPortugues("Pre-Race Show"))
        assertEquals("RaceControl", TraducaoDeSessoes.paraPortugues("RaceControl"))
    }

    @Test
    fun reconheceVariacoesDeMaiusculasEMinusculas() {
        assertEquals("Treino Livre 1", TraducaoDeSessoes.paraPortugues("FREE PRACTICE 1"))
        assertEquals("Free Practice 1", t("TREINO LIVRE 1"))
    }
}
