package com.motorsport.calendario

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdemPorPrimeiraSessaoTest {

    private val hoje = LocalDate.of(2026, 10, 21)
    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    private fun etapa(categoria: String, circuito: String, inicio: String, fim: String) =
        MainActivity.Etapa(
            categoria = categoria,
            pais = "X",
            circuito = circuito,
            data = "",
            dataInicio = inicio,
            dataFim = fim
        )

    private fun sessao(categoria: String, dia: Int, hora: Int, minuto: Int = 0, mes: Int = 10) =
        SessaoParaAviso(
            categoria,
            "local",
            "Treino Livre 1",
            ZonedDateTime.of(2026, mes, dia, hora, minuto, 0, 0, saoPaulo)
        )

    private val f1 = etapa("F1", "Austin", "2026-10-23", "2026-10-25")
    private val moto = etapa("MotoGP", "Phillip Island", "2026-10-23", "2026-10-25")
    private val indy = etapa("IndyCar", "Laguna", "2026-10-30", "2026-11-01")

    @Test
    fun noMesmoDia_quemTemASessaoMaisCedoVemPrimeiro() {
        val sessoes = listOf(
            sessao("F1", 23, 14, 30),
            sessao("MotoGP", 23, 1, 0)
        )

        val lista = RegrasDeFiltro.aplicar(listOf(f1, moto), FiltrosDeEtapas(), hoje, sessoes)

        assertEquals(listOf("MotoGP", "F1"), lista.map { it.categoria })
    }

    @Test
    fun semHorarios_valeAOrdemDasDatasEAListaOriginalDesempata() {
        val lista = RegrasDeFiltro.aplicar(listOf(indy, f1, moto), FiltrosDeEtapas(), hoje)

        assertEquals(listOf("F1", "MotoGP", "IndyCar"), lista.map { it.categoria })
    }

    @Test
    fun etapaSemSessoesConfirmadasEntraPeloDiaDeInicio() {
        val sessoes = listOf(sessao("F1", 23, 14, 0))

        val lista = RegrasDeFiltro.aplicar(listOf(indy, moto, f1), FiltrosDeEtapas(), hoje, sessoes)

        // Moto não tem sessão: conta como o começo do dia 23, que vem antes das 14h da F1.
        assertEquals(listOf("MotoGP", "F1", "IndyCar"), lista.map { it.categoria })
    }

    @Test
    fun primeiraSessaoEscolheAMaisCedoDaMesmaCategoriaEPeriodo() {
        val sessoes = listOf(
            sessao("F1", 24, 9),
            sessao("F1", 23, 7, 30),
            sessao("F1", 25, 11),
            sessao("MotoGP", 23, 1)
        )

        val primeiras = RegrasDeFiltro.primeirasSessoes(listOf(f1), sessoes)

        assertEquals(sessoes[1].inicio.toInstant(), primeiras[f1])
    }

    @Test
    fun sessaoDeOutraEtapaDaMesmaCategoriaNaoEntra() {
        val outraF1 = etapa("F1", "Cidade do Mexico", "2026-11-06", "2026-11-08")
        val sessoes = listOf(sessao("F1", 7, 15, mes = 11))

        val primeiras = RegrasDeFiltro.primeirasSessoes(listOf(f1, outraF1), sessoes)

        assertTrue(f1 !in primeiras)
        assertEquals(sessoes[0].inicio.toInstant(), primeiras[outraF1])
    }

    @Test
    fun categoriaComAcentoEMaiusculaCasaComASessao() {
        val formulaE = etapa("Fórmula E", "Londres", "2026-10-24", "2026-10-25")
        val sessoes = listOf(sessao("formula e", 24, 6))

        assertEquals(
            sessoes[0].inicio.toInstant(),
            RegrasDeFiltro.primeirasSessoes(listOf(formulaE), sessoes)[formulaE]
        )
    }
}
