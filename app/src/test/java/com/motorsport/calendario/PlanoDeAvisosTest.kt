package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZonedDateTime

class PlanoDeAvisosTest {

    private val brasilia = PlanoDeAvisos.BRASILIA

    // Sábado, 17/10/2026, 12:00 em Brasília.
    private val agora: Instant = ZonedDateTime.of(2026, 10, 17, 12, 0, 0, 0, brasilia).toInstant()

    private fun sessao(
        nome: String,
        dia: Int,
        hora: Int,
        minuto: Int = 0,
        categoria: String = "F2"
    ) = SessaoParaAviso(
        categoria,
        "Montreal",
        nome,
        ZonedDateTime.of(2026, 10, dia, hora, minuto, 0, 0, brasilia)
    )

    @Test
    fun desativadoNaoGeraAvisos() {
        val avisos = PlanoDeAvisos.calcular(listOf(sessao("Corrida", 18, 10)), agora, 0)
        assertTrue(avisos.isEmpty())
    }

    @Test
    fun avisoDisparaOTempoEscolhidoAntesDaSessao() {
        val corrida = sessao("Corrida", 18, 10)

        val avisos = PlanoDeAvisos.calcular(listOf(corrida), agora, 30)

        assertEquals(1, avisos.size)
        assertEquals(corrida.inicio.toInstant().minusSeconds(30 * 60L), avisos[0].dispararEm)
    }

    @Test
    fun todasAsOpcoesDaPreferenciaFuncionam() {
        val corrida = sessao("Corrida", 18, 10)

        PreferenciasDeAviso.opcoes.forEach { minutos ->
            val avisos = PlanoDeAvisos.calcular(listOf(corrida), agora, minutos)
            assertEquals(
                corrida.inicio.toInstant().minusSeconds(minutos * 60L),
                avisos.single().dispararEm
            )
        }
    }

    @Test
    fun ignoraAvisoQueJaPassou() {
        // Começa às 12:20; o aviso de 30 min seria às 11:50, antes de "agora".
        val avisos = PlanoDeAvisos.calcular(listOf(sessao("Treino", 17, 12, 20)), agora, 30)
        assertTrue(avisos.isEmpty())
    }

    @Test
    fun ignoraSessaoQueJaAconteceu() {
        val avisos = PlanoDeAvisos.calcular(listOf(sessao("Treino", 16, 10)), agora, 15)
        assertTrue(avisos.isEmpty())
    }

    @Test
    fun sessaoDistanteTambemRecebeAviso() {
        // Depois de uma pausa longa no calendário, o aviso da volta continua agendado.
        val longe = SessaoParaAviso(
            "F1",
            "Interlagos",
            "Corrida",
            ZonedDateTime.of(2027, 3, 14, 10, 0, 0, 0, brasilia)
        )

        assertEquals(1, PlanoDeAvisos.calcular(listOf(longe), agora, 30).size)
    }

    @Test
    fun ordenaPorHorarioDoAviso() {
        val tarde = sessao("Corrida", 18, 14)
        val cedo = sessao("Treino", 17, 18)

        val avisos = PlanoDeAvisos.calcular(listOf(tarde, cedo), agora, 15)

        assertEquals(listOf("Treino", "Corrida"), avisos.map { it.sessao.nome })
    }

    @Test
    fun respeitaOLimiteDeAlarmes() {
        val muitas = (1..10).map { sessao("Sessão $it", 18, it) }

        val avisos = PlanoDeAvisos.calcular(muitas, agora, 15, limite = 3)

        assertEquals(3, avisos.size)
        assertEquals(listOf("Sessão 1", "Sessão 2", "Sessão 3"), avisos.map { it.sessao.nome })
    }

    @Test
    fun sessoesRepetidasGeramUmUnicoAviso() {
        val corrida = sessao("Corrida", 18, 10)

        val avisos = PlanoDeAvisos.calcular(listOf(corrida, corrida.copy()), agora, 30)

        assertEquals(1, avisos.size)
    }

    @Test
    fun horarioNovoMudaOCodigoDaSessao() {
        val antes = sessao("Corrida", 18, 10)
        val depois = sessao("Corrida", 18, 11)

        assertFalse(antes.chave == depois.chave)
        assertFalse(antes.codigo == depois.codigo)
    }

    @Test
    fun inicioDaSessaoLeDataEHoraEmBrasilia() {
        val inicio = PlanoDeAvisos.inicioDaSessao("07/03/2026", "21:25")

        assertNotNull(inicio)
        assertEquals(ZonedDateTime.of(2026, 3, 7, 21, 25, 0, 0, brasilia), inicio)
    }

    @Test
    fun horarioNaoConfirmadoNaoGeraAviso() {
        assertNull(PlanoDeAvisos.inicioDaSessao("18/12/2026", "A confirmar"))
        assertNull(PlanoDeAvisos.inicioDaSessao("18/12/2026", "TBA"))
        assertNull(PlanoDeAvisos.inicioDaSessao("18/12/2026", ""))
    }

    @Test
    fun dataOuHoraInvalidasSaoIgnoradas() {
        assertNull(PlanoDeAvisos.inicioDaSessao("31/02/2026", "10:00"))
        assertNull(PlanoDeAvisos.inicioDaSessao("18/12/2026", "25:00"))
        assertNull(PlanoDeAvisos.inicioDaSessao("2026-12-18", "10:00"))
    }

    @Test
    fun avisoMuitoAtrasadoEDescartado() {
        assertTrue(PlanoDeAvisos.aindaVale(30 * 60_000L))
        assertTrue(PlanoDeAvisos.aindaVale(-4 * 60_000L))
        assertFalse(PlanoDeAvisos.aindaVale(-6 * 60_000L))
    }

    @Test
    fun textoDeAntecedencia() {
        assertEquals("Começando agora", PlanoDeAvisos.textoDeAntecedencia(0))
        assertEquals("Começa em 15 min", PlanoDeAvisos.textoDeAntecedencia(15))
        assertEquals("Começa em 1 h", PlanoDeAvisos.textoDeAntecedencia(60))
        assertEquals("Começa em 1 h 15 min", PlanoDeAvisos.textoDeAntecedencia(75))
    }

    @Test
    fun reconheceCorridasDeTodasAsCategorias() {
        listOf(
            "Corrida", "Corrida 1", "Corrida Sprint", "Corrida Feature 2",
            "Corrida (Rodada 7)", "CORRIDA"
        ).forEach { assertTrue(it, PlanoDeAvisos.ehCorrida(it)) }
    }

    @Test
    fun treinosEClassificacoesNaoSaoCorridas() {
        listOf(
            "Treino Livre 1", "Classificação", "Classificação Sprint", "Warm-up",
            "Pre-Race Show", "Competição de Pit Stop", "Fast Friday"
        ).forEach { assertFalse(it, PlanoDeAvisos.ehCorrida(it)) }
    }

    @Test
    fun modoCorridasDescartaTreinosEClassificacoes() {
        val sessoes = listOf(
            sessao("Treino Livre", 17, 18),
            sessao("Classificação", 18, 10),
            sessao("Corrida Sprint", 18, 12),
            sessao("Corrida Feature", 19, 10)
        )

        val avisos = PlanoDeAvisos.calcular(
            sessoes, agora, 15, modoDaCategoria = { ModoDeAviso.CORRIDAS }
        )

        assertEquals(listOf("Corrida Sprint", "Corrida Feature"), avisos.map { it.sessao.nome })
    }

    @Test
    fun cadaCategoriaTemOSeuProprioModo() {
        val sessoes = listOf(
            sessao("Treino Livre", 17, 18, categoria = "F1"),
            sessao("Corrida", 18, 10, categoria = "F1"),
            sessao("Treino Livre", 17, 19, categoria = "F2"),
            sessao("Corrida Feature", 18, 11, categoria = "F2"),
            sessao("Corrida", 18, 12, categoria = "IndyCar")
        )
        val modos = mapOf(
            "F1" to ModoDeAviso.TODAS,
            "F2" to ModoDeAviso.CORRIDAS,
            "IndyCar" to ModoDeAviso.NENHUMA
        )

        val avisos = PlanoDeAvisos.calcular(
            sessoes, agora, 15, modoDaCategoria = { modos.getValue(it) }
        )

        assertEquals(
            listOf("F1:Treino Livre", "F1:Corrida", "F2:Corrida Feature"),
            avisos.map { "${it.sessao.categoria}:${it.sessao.nome}" }
        )
    }

    @Test
    fun modoNenhumaSilenciaACategoriaInteira() {
        val sessoes = listOf(sessao("Treino Livre", 17, 18), sessao("Corrida", 18, 10))

        val avisos = PlanoDeAvisos.calcular(
            sessoes, agora, 15, modoDaCategoria = { ModoDeAviso.NENHUMA }
        )

        assertTrue(avisos.isEmpty())
    }

    @Test
    fun porPadraoTodasAsSessoesRecebemAviso() {
        val sessoes = listOf(sessao("Treino Livre", 17, 18), sessao("Corrida", 18, 10))

        assertEquals(2, PlanoDeAvisos.calcular(sessoes, agora, 15).size)
    }
}