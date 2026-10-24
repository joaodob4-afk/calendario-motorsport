package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Test

class TraducaoDeNomesTest {

    private fun t(nome: String) = TraducaoDeNomes.evento(nome)

    @Test
    fun traduzGrandesPremios() {
        assertEquals("Australian Grand Prix", t("GP da Austrália"))
        assertEquals("United States Grand Prix", t("GP dos Estados Unidos"))
        assertEquals("Mexico City Grand Prix", t("GP da Cidade do México"))
        assertEquals("Czech Grand Prix", t("GP da Tchéquia"))
    }

    @Test
    fun traduzProvasDeResistencia() {
        assertEquals("6 Hours of Imola", t("6 Horas de Imola"))
        assertEquals("24 Hours of Le Mans", t("24 Horas de Le Mans"))
        assertEquals("1812 km of Qatar", t("1812 km do Catar"))
    }

    @Test
    fun traduzEPrixComNomeDaCidade() {
        assertEquals("Berlin E-Prix", t("E-Prix de Berlim"))
        assertEquals("Mexico City E-Prix", t("E-Prix da Cidade do México"))
        assertEquals("Miami E-Prix", t("E-Prix de Miami"))
    }

    @Test
    fun nomesDesconhecidosOuJaEmInglesFicamIguais() {
        listOf("Lone Star Le Mans", "Firestone Grand Prix of St. Petersburg", "Qualquer coisa")
            .forEach { assertEquals(it, t(it)) }
    }

    @Test
    fun traduzPaisesECidades() {
        assertEquals("Brazil", TraducaoDeNomes.lugar("Brasil"))
        assertEquals("United States", TraducaoDeNomes.lugar("Estados Unidos"))
        assertEquals("Czech Republic", TraducaoDeNomes.lugar("República Tcheca"))
        assertEquals("Shanghai", TraducaoDeNomes.lugar("Xangai"))
        assertEquals("Mexico City", TraducaoDeNomes.lugar("Cidade do México"))
    }

    @Test
    fun lugaresIguaisOuDesconhecidosFicamComoEstao() {
        assertEquals("Interlagos", TraducaoDeNomes.lugar("Interlagos"))
        assertEquals("Austin", TraducaoDeNomes.lugar("Austin"))
        assertEquals("Portugal", TraducaoDeNomes.lugar("Portugal"))
    }

    @Test
    fun traduzPaisEmLocaisCompostosNosDoisIdiomas() {
        assertEquals("Barcelona, Spain", TraducaoDeNomes.lugar("Barcelona, Espanha"))
        assertEquals("Barcelona, Espanha", TraducaoDeNomes.lugarParaPortugues("Barcelona, Spain"))
        assertEquals("Long Beach, California", TraducaoDeNomes.lugarParaPortugues("Long Beach, California"))
    }
}
