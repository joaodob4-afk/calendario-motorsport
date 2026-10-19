package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NomesDeEventosTest {

    @Test
    fun formula1UsaOIdentificadorDoCircuito() {
        assertEquals("GP da Austrália", NomesDeEventos.nome("F1", "Melbourne", "albert_park"))
        assertEquals("GP de São Paulo", NomesDeEventos.nome("F1", "Interlagos", "interlagos"))
        assertEquals("GP da Espanha", NomesDeEventos.nome("F1", "Madrid", "madring"))
    }

    @Test
    fun formula1SemIdentificadorUsaACidade() {
        assertEquals("GP da Itália", NomesDeEventos.nome("F1", "Monza"))
    }

    @Test
    fun f2EF3UsamOMesmoNomeDaF1PelaCidade() {
        assertEquals("GP de Mônaco", NomesDeEventos.nome("F2", "Monte Carlo"))
        assertEquals("GP de Mônaco", NomesDeEventos.nome("F3", "Monte Carlo"))
        assertEquals("GP do Azerbaijão", NomesDeEventos.nome("F2", "Baku City Circuit"))
    }

    @Test
    fun formulaEMontaEPrixComAPreposicaoCerta() {
        assertEquals("E-Prix de Berlim", NomesDeEventos.nome("Formula E", "Berlim"))
        assertEquals(
            "E-Prix da Cidade do México",
            NomesDeEventos.nome("Formula E", "Cidade do México")
        )
    }

    @Test
    fun motosUsamNomesProprios() {
        assertEquals("GP da Catalunha", NomesDeEventos.nome("MotoGP", "Barcelona"))
        assertEquals("GP do Brasil", NomesDeEventos.nome("Moto2", "Goiânia"))
        assertEquals("GP de Aragão", NomesDeEventos.nome("Moto3", "Aragón"))
    }

    @Test
    fun mesmaCidadeTemNomeDiferenteEmCadaCategoria() {
        assertEquals("GP de Barcelona-Catalunha", NomesDeEventos.nome("F1", "Barcelona"))
        assertEquals("GP da Catalunha", NomesDeEventos.nome("MotoGP", "Barcelona"))
        assertEquals("6 Horas de Barcelona", NomesDeEventos.nome("WEC", "Barcelona"))
    }

    @Test
    fun wecTemNomeDeProva() {
        assertEquals("24 Horas de Le Mans", NomesDeEventos.nome("WEC", "Circuit de la Sarthe"))
        assertEquals("6 Horas de Monza", NomesDeEventos.nome("WEC", "Monza"))
    }

    @Test
    fun ignoraMaiusculasEAcentosNoCircuito() {
        assertEquals("GP da Hungria", NomesDeEventos.nome("F2", "BUDAPESTE"))
        assertEquals("GP de Portugal", NomesDeEventos.nome("MotoGP", "Portimão"))
    }

    @Test
    fun semNomeConhecidoDevolveNulo() {
        assertNull(NomesDeEventos.nome("F2", "Cidade Nova"))
        assertNull(NomesDeEventos.nome("F1", "", "circuito_novo"))
        assertNull(NomesDeEventos.nome("IndyCar", "Streets of St. Petersburg"))
        assertNull(NomesDeEventos.nome("Categoria Inexistente", "Monza"))
    }

    @Test
    fun f1ComCircuitoNovoUsaCidadeEmInglesOuPais() {
        // Circuito desconhecido, mas cidade como a API escreve
        assertEquals("GP da Cidade do México", NomesDeEventos.nome("F1", "Mexico City", "circuito_novo"))
        // Nem circuito nem cidade conhecidos: cai no país, em português ou inglês
        assertEquals("GP da Cidade do México", NomesDeEventos.nome("F1", "Outra", "circuito_novo", "México"))
        assertEquals("GP da Cidade do México", NomesDeEventos.nome("F1", "Outra", "circuito_novo", "Mexico"))
        // País com mais de uma corrida não adivinha
        assertNull(NomesDeEventos.nome("F1", "Outra", "circuito_novo", "Estados Unidos"))
    }
}
