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
    fun nomesJaEmInglesOuDesconhecidosFicamIguais() {
        listOf("Practice", "Warm-up", "Fast Friday", "Pre-Race Show", "Hyperpole 1 Hypercar")
            .forEach { assertEquals(it, t(it)) }
    }
}
