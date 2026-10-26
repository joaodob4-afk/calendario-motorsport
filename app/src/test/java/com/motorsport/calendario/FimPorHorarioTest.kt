package com.motorsport.calendario

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FimPorHorarioTest {

    private val fuso = ZoneId.systemDefault()
    private val hoje = LocalDate.of(2026, 10, 25)

    private fun etapa(categoria: String) = MainActivity.Etapa(
        categoria = categoria,
        pais = "Austrália",
        circuito = "Phillip Island",
        data = "",
        dataInicio = "2026-10-22",
        dataFim = "2026-10-25"
    )

    private fun sessao(categoria: String, nome: String, dia: Int, hora: Int, minuto: Int = 0) =
        SessaoParaAviso(
            categoria = categoria,
            local = "Phillip Island",
            nome = nome,
            inicio = ZonedDateTime.of(LocalDate.of(2026, 10, dia), LocalTime.of(hora, minuto), fuso)
        )

    @Test
    fun terminaDepoisDaUltimaSessaoDoUltimoDia() {
        val corrida = sessao("MotoGP", "Corrida", 25, 2, 30)
        val lista = RegrasDeFiltro.comFimPorHorario(
            listOf(etapa("MotoGP")),
            listOf(sessao("MotoGP", "Treino", 22, 10), corrida)
        )
        val etapa = lista.single()

        val fimEsperado = corrida.inicio.toInstant()
            .plusSeconds(RegrasDeFiltro.HORAS_APOS_ULTIMA_SESSAO * 3600)
        assertEquals(fimEsperado.toEpochMilli(), etapa.terminaEmMs)

        // Logo depois da corrida ainda conta como em andamento; passada a margem, finalizou.
        assertFalse(RegrasDeFiltro.finalizada(etapa, hoje, corrida.inicio.toInstant().plusSeconds(3600)))
        assertTrue(RegrasDeFiltro.finalizada(etapa, hoje, fimEsperado.plusSeconds(60)))
    }

    @Test
    fun ultimaSessaoConhecidaAntesDoUltimoDia_naoDefineOFim() {
        // Falta a corrida (A confirmar): a última sessão conhecida é de quinta, não de domingo.
        val lista = RegrasDeFiltro.comFimPorHorario(
            listOf(etapa("MotoGP")),
            listOf(sessao("MotoGP", "Treino", 22, 10), sessao("MotoGP", "Classificação", 23, 14))
        )

        assertEquals(0L, lista.single().terminaEmMs)
    }

    @Test
    fun resistenciaFicaSoComODia() {
        val lista = RegrasDeFiltro.comFimPorHorario(
            listOf(etapa("WEC")),
            listOf(sessao("WEC", "Corrida", 25, 2, 30))
        )

        assertEquals(0L, lista.single().terminaEmMs)
    }

    @Test
    fun semSessoes_devolveAsMesmasEtapas() {
        val etapas = listOf(etapa("MotoGP"))

        assertEquals(etapas, RegrasDeFiltro.comFimPorHorario(etapas, emptyList()))
    }
}