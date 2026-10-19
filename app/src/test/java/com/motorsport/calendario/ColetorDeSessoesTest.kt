package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ColetorDeSessoesTest {

    private val json = """
        {"MRData":{"RaceTable":{"season":"2026","Races":[
          {
            "raceName":"Australian Grand Prix",
            "Circuit":{"circuitId":"albert_park","Location":{"locality":"Melbourne"}},
            "date":"2026-03-08","time":"04:00:00Z",
            "FirstPractice":{"date":"2026-03-06","time":"01:30:00Z"},
            "Qualifying":{"date":"2026-03-07","time":"05:00:00Z"}
          },
          {
            "raceName":"Chinese Grand Prix",
            "Circuit":{"circuitId":"shanghai","Location":{"locality":"Shanghai"}},
            "date":"2026-03-15",
            "FirstPractice":{"date":"2026-03-13"}
          }
        ]}}}
    """.trimIndent()

    @Test
    fun converteHorariosDaF1ParaBrasilia() {
        val sessoes = ColetorDeSessoes.sessoesDaF1(json)
        val corrida = sessoes.first { it.nome == "Corrida" }

        assertEquals("F1", corrida.categoria)
        assertEquals("Melbourne", corrida.local)
        // 04:00 UTC são 01:00 em Brasília (UTC-3).
        assertEquals(
            ZonedDateTime.of(2026, 3, 8, 1, 0, 0, 0, PlanoDeAvisos.BRASILIA).toInstant(),
            corrida.inicio.toInstant()
        )
    }

    @Test
    fun guardaONomeDoEventoDaF1ParaAAparenciaDoAviso() {
        val sessoes = ColetorDeSessoes.sessoesDaF1(json)

        assertTrue(sessoes.filter { it.local == "Melbourne" }.all { it.evento == "GP da Austrália" })
    }

    @Test
    fun nomeDoEventoNaoMudaACaveDoAviso() {
        val inicio = ZonedDateTime.of(2026, 3, 8, 1, 0, 0, 0, PlanoDeAvisos.BRASILIA)
        val a = SessaoParaAviso("F1", "Melbourne", "Corrida", inicio, "GP da Austrália")
        val b = SessaoParaAviso("F1", "Melbourne", "Corrida", inicio)

        assertEquals(a.chave, b.chave)
        assertEquals(a.codigo, b.codigo)
    }

    @Test
    fun incluiTreinosEClassificacaoComNomesEmPortugues() {
        val nomes = ColetorDeSessoes.sessoesDaF1(json).map { it.nome }

        assertTrue("Treino Livre 1" in nomes)
        assertTrue("Classificação" in nomes)
    }

    @Test
    fun ignoraSessaoSemHorarioConfirmado() {
        val sessoes = ColetorDeSessoes.sessoesDaF1(json)

        // A etapa da China só tem data, sem hora: nenhuma sessão dela entra.
        assertTrue(sessoes.none { it.local == "Shanghai" })
        assertEquals(3, sessoes.size)
    }
}
