package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZonedDateTime

class AvisoNaHoraDeComecarTest {

    private val brasilia = PlanoDeAvisos.BRASILIA

    // Sábado, 17/10/2026, 12:00 em Brasília.
    private val agora: Instant = ZonedDateTime.of(2026, 10, 17, 12, 0, 0, 0, brasilia).toInstant()

    private fun sessao(nome: String, dia: Int, hora: Int, categoria: String = "F2") =
        SessaoParaAviso(
            categoria,
            "Montreal",
            nome,
            ZonedDateTime.of(2026, 10, dia, hora, 0, 0, 0, brasilia)
        )

    @Test
    fun geraUmAvisoAntesEUmNaHoraDeCadaSessao() {
        val corrida = sessao("Corrida", 18, 10)

        val avisos = PlanoDeAvisos.calcularTodos(listOf(corrida), agora, 30)

        assertEquals(2, avisos.size)
        assertEquals(
            corrida.inicio.toInstant().minusSeconds(30 * 60L),
            avisos.single { !it.naHoraDeComecar }.dispararEm
        )
        assertEquals(
            corrida.inicio.toInstant(),
            avisos.single { it.naHoraDeComecar }.dispararEm
        )
    }

    @Test
    fun avisoNaHoraPodeSerDesligado() {
        val avisos = PlanoDeAvisos.calcularTodos(
            listOf(sessao("Corrida", 18, 10)), agora, 30, avisarNaHora = false
        )

        assertEquals(1, avisos.size)
        assertFalse(avisos.single().naHoraDeComecar)
    }

    @Test
    fun avisosDesativadosNaoGeramNada() {
        assertTrue(PlanoDeAvisos.calcularTodos(listOf(sessao("Corrida", 18, 10)), agora, 0).isEmpty())
    }

    @Test
    fun avisoNaHoraSobreviveQuandoOAvisoAntecipadoJaPassou() {
        // Começa 12:20: o aviso de 30 min já passou, mas o da hora de começar ainda vai ocorrer.
        val sessaoProxima = SessaoParaAviso(
            "F2", "Montreal", "Treino",
            ZonedDateTime.of(2026, 10, 17, 12, 20, 0, 0, brasilia)
        )

        val avisos = PlanoDeAvisos.calcularTodos(listOf(sessaoProxima), agora, 30)

        assertEquals(1, avisos.size)
        assertTrue(avisos.single().naHoraDeComecar)
    }

    @Test
    fun alarmesDaMesmaSessaoTemCodigosDiferentes() {
        val corrida = sessao("Corrida", 18, 10)

        val avisos = PlanoDeAvisos.calcularTodos(listOf(corrida), agora, 30)
        val codigos = avisos.map { it.codigoDoAlarme }.toSet()

        assertEquals(2, codigos.size)
        assertFalse(corrida.codigo == corrida.codigoDoInicio)
    }

    @Test
    fun ordenaPorHorarioEAplicaOLimiteTotal() {
        val sessoes = listOf(sessao("A", 18, 10), sessao("B", 18, 11), sessao("C", 18, 12))

        val avisos = PlanoDeAvisos.calcularTodos(sessoes, agora, 15, limite = 4)

        assertEquals(4, avisos.size)
        assertEquals(avisos.sortedBy { it.dispararEm }, avisos)
        // 09:45 A, 10:00 A, 10:45 B, 11:00 B
        assertEquals(listOf("A", "A", "B", "B"), avisos.map { it.sessao.nome })
    }

    @Test
    fun respeitaOModoDaCategoriaNoAvisoNaHora() {
        val sessoes = listOf(sessao("Treino Livre", 17, 18), sessao("Corrida", 18, 10))

        val avisos = PlanoDeAvisos.calcularNaHoraDeComecar(
            sessoes, agora, modoDaCategoria = { ModoDeAviso.CORRIDAS }
        )

        assertEquals(listOf("Corrida"), avisos.map { it.sessao.nome })
    }
}
