package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MotoCalendarJsonTest {

    private fun json(
        categoria: String = "MotoGP",
        etapas: String = etapa(1)
    ) = """{"temporada": 2026, "categoria": "$categoria", "etapas": [$etapas]}"""

    private fun etapa(
        numero: Int,
        inicio: String = "27/02/2026",
        fim: String = "01/03/2026",
        sessoes: String = """{"nome": "Corrida", "data": "01/03/2026", "horario": "05:00"}"""
    ) = """{"etapa": $numero, "circuito": "Buriram", "pais": "Tailândia",
        "inicio": "$inicio", "fim": "$fim", "sessoes": [$sessoes]}"""

    @Test
    fun jsonsEmbutidosDeTodasAsClassesSaoValidos() {
        val context = RuntimeEnvironment.getApplication()

        for (classe in ClasseMoto.values()) {
            val eventos = MotoCalendarJson.carregar(context, classe)

            assertTrue("${classe.rotulo} sem etapas", eventos.isNotEmpty())
            assertEquals(
                (1..eventos.size).toList(),
                eventos.map { it.etapa }
            )
        }
    }

    @Test
    fun etapasDeTodasJuntaTodasAsClasses() {
        val context = RuntimeEnvironment.getApplication()

        val categorias = MotoCalendarJson.etapasDeTodas(context).map { it.categoria }.toSet()

        assertEquals(setOf("MotoGP", "Moto2", "Moto3", "WEC"), categorias)
    }

    @Test
    fun aceitaEtapaAindaSemSessoes() {
        val eventos = MotoCalendarJson.interpretar(
            json(etapas = etapa(1, sessoes = "")),
            ClasseMoto.MOTOGP
        )

        assertEquals(1, eventos.size)
        assertTrue(eventos[0].sessoes.isEmpty())
    }

    @Test
    fun rejeitaJsonDeOutraCategoria() {
        assertTrue(
            MotoCalendarJson.interpretar(json(categoria = "Moto2"), ClasseMoto.MOTOGP).isEmpty()
        )
    }

    @Test
    fun rejeitaEtapaRepetidaDataInvalidaEIntervaloInvertido() {
        val repetida = json(etapas = etapa(1) + "," + etapa(1))
        val dataInvalida = json(etapas = etapa(1, inicio = "31/02/2026"))
        val invertido = json(etapas = etapa(1, inicio = "05/03/2026", fim = "01/03/2026"))

        for (texto in listOf(repetida, dataInvalida, invertido)) {
            assertTrue(MotoCalendarJson.interpretar(texto, ClasseMoto.MOTOGP).isEmpty())
        }
    }

    @Test
    fun rejeitaTextoQueNaoEJson() {
        assertTrue(MotoCalendarJson.interpretar("não é json", ClasseMoto.MOTOGP).isEmpty())
    }

    @Test
    fun classePorRotuloIgnoraMaiusculasEAcentos() {
        assertEquals(ClasseMoto.MOTO2, ClasseMoto.porRotulo("moto2"))
        assertEquals(ClasseMoto.MOTOGP, ClasseMoto.porRotulo("MOTOGP"))
        assertNull(ClasseMoto.porRotulo("F1"))
        assertNull(ClasseMoto.porRotulo(null))
    }

    @Test
    fun todasAsClassesEstaoEmCategorias() {
        for (classe in ClasseMoto.values()) {
            assertNotNull(classe.rotulo, Categorias.info(classe.rotulo))
        }
    }

    @Test
    fun avisosIncluemSessoesDasMotosEmHorarioDeBrasilia() {
        val context = RuntimeEnvironment.getApplication()

        val corrida = ColetorDeSessoes.coletar(context).firstOrNull {
            it.categoria == "MotoGP" && it.nome == "Corrida" &&
                it.inicio.toLocalDate().toString() == "2026-03-01"
        }

        assertNotNull("Corrida da MotoGP na Tailândia não encontrada", corrida)
        assertEquals(
            ZonedDateTime.of(2026, 3, 1, 5, 0, 0, 0, PlanoDeAvisos.BRASILIA).toInstant(),
            corrida!!.inicio.toInstant()
        )
    }

    @Test
    fun avisosIncluemSessoesDoWecEmHorarioDeBrasilia() {
        val context = RuntimeEnvironment.getApplication()

        val corrida = ColetorDeSessoes.coletar(context).firstOrNull {
            it.categoria == "WEC" && it.nome == "Corrida" && it.local == "Barcelona"
        }

        assertNotNull("Corrida do WEC em Barcelona não encontrada", corrida)
        // 12:00 em Barcelona (CEST, UTC+2) são 07:00 em Brasília.
        assertEquals(
            ZonedDateTime.of(2026, 10, 18, 7, 0, 0, 0, PlanoDeAvisos.BRASILIA).toInstant(),
            corrida!!.inicio.toInstant()
        )
    }

    @Test
    fun classificacaoEHyperpoleDoWecNaoContamComoCorrida() {
        listOf("Classificação Hypercar", "Hyperpole LMGT3", "Treino Livre 3 Hypercar").forEach {
            assertTrue(it, !PlanoDeAvisos.ehCorrida(it))
        }
    }

    @Test
    fun sprintECorridaContamComoCorridaMasTreinoEWarmUpNao() {
        listOf("Corrida", "Corrida Sprint").forEach {
            assertTrue(it, PlanoDeAvisos.ehCorrida(it))
        }
        listOf("Treino Livre 1", "Practice", "Classificação 1", "Warm-up").forEach {
            assertTrue(it, !PlanoDeAvisos.ehCorrida(it))
        }
    }
}
