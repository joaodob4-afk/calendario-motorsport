package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SessaoDoCartaoTest {
    private val zona = ZoneId.of("America/Sao_Paulo")

    private fun sessao(
        nome: String,
        dia: Int,
        hora: Int,
        categoria: String = "F1",
        evento: String = ""
    ) = SessaoParaAviso(categoria, "Cidade", nome, ZonedDateTime.of(2026, 10, dia, hora, 0, 0, 0, zona), evento)

    private fun agora(dia: Int, hora: Int, minuto: Int = 0): Instant =
        ZonedDateTime.of(2026, 10, dia, hora, minuto, 0, 0, zona).toInstant()

    // Fim de semana: sexta 23, sábado 24, domingo 25.
    private val fimDeSemana = listOf(
        sessao("Treino Livre 1", 23, 11),
        sessao("Classificação", 24, 14),
        sessao("Corrida", 25, 13)
    )

    @Test
    fun duracaoDependeDoTipoDaSessao() {
        assertEquals(Duration.ofMinutes(60), SessaoDoCartao.duracaoEstimada(sessao("Treino Livre 1", 23, 11)))
        assertEquals(Duration.ofMinutes(60), SessaoDoCartao.duracaoEstimada(sessao("Classificação Sprint", 23, 11)))
        assertEquals(Duration.ofMinutes(45), SessaoDoCartao.duracaoEstimada(sessao("Corrida Sprint", 24, 11)))
        assertEquals(Duration.ofMinutes(20), SessaoDoCartao.duracaoEstimada(sessao("Warm-up", 25, 9)))
        assertEquals(Duration.ofHours(2), SessaoDoCartao.duracaoEstimada(sessao("Corrida", 25, 13)))
        assertEquals(Duration.ofMinutes(60), SessaoDoCartao.duracaoEstimada(sessao("Corrida", 25, 13, "MotoGP")))
    }

    @Test
    fun corridaDoWecUsaAsHorasDoNomeDoEvento() {
        fun corrida(evento: String) = SessaoDoCartao.duracaoEstimada(sessao("Corrida", 25, 13, "WEC", evento))
        assertEquals(Duration.ofHours(6), corrida("6 Horas de Imola"))
        assertEquals(Duration.ofHours(24), corrida("24 Horas de Le Mans"))
        assertEquals(Duration.ofHours(8), corrida("8 Horas do Bahrein"))
        assertEquals(Duration.ofHours(10), corrida("1812 km do Catar"))
        assertEquals(Duration.ofHours(6), corrida("Lone Star Le Mans"))
    }

    @Test
    fun duracaoCurtaFicaCompactaEmQualquerEscala() {
        fun texto(minutos: Long, segundos: Long = 0) =
            SessaoDoCartao.duracaoCurta(Duration.ofMinutes(minutos).plusSeconds(segundos))
        assertEquals("1 min", texto(0, 30))
        assertEquals("29 min", texto(29))
        assertEquals("3h", texto(180))
        assertEquals("14h 29min", texto(14 * 60 + 29))
        assertEquals("2d", texto(48 * 60))
        assertEquals("2d 5h", texto(53 * 60 + 40))
    }

    @Test
    fun mostraASessaoEmAndamentoEDepoisAProxima() {
        fun nome(agora: Instant) = SessaoDoCartao.atual(fimDeSemana, agora)?.nome

        assertEquals("Treino Livre 1", nome(agora(23, 10)))      // antes de começar
        assertEquals("Treino Livre 1", nome(agora(23, 11, 30)))  // em andamento
        assertEquals("Classificação", nome(agora(23, 12, 1)))    // TL1 acabou
        assertEquals("Corrida", nome(agora(24, 15, 1)))          // classificação acabou
        assertEquals("Corrida", nome(agora(25, 14, 59)))         // corrida ainda dentro das 2 h
        assertNull(nome(agora(25, 15, 1)))                       // tudo terminou
    }

    @Test
    fun eventoSaiDoCardQuandoAUltimaSessaoTermina() {
        val fim = LocalDate.of(2026, 10, 25)
        assertFalse(SessaoDoCartao.encerrado(fimDeSemana, fim, agora(25, 14), zona))
        assertTrue(SessaoDoCartao.encerrado(fimDeSemana, fim, agora(25, 15, 1), zona))
    }

    @Test
    fun comHorariosPorConfirmarOEventoContinuaAteADataDeFim() {
        // Só a sexta tem horário confirmado; o evento vai até domingo.
        val soSexta = listOf(sessao("Treino Livre 1", 23, 11))
        val fim = LocalDate.of(2026, 10, 25)
        assertFalse(SessaoDoCartao.encerrado(soSexta, fim, agora(23, 20), zona))
        assertFalse(SessaoDoCartao.encerrado(emptyList(), fim, agora(25, 20), zona))
    }

    @Test
    fun separaAsSessoesDaEtapaPorCategoriaEData() {
        val todas = fimDeSemana + listOf(
            sessao("Treino Livre 1", 23, 12, "F2"),
            sessao("Corrida", 30, 13),            // outro fim de semana
            sessao("Treino Livre 1", 16, 11)      // fim de semana anterior
        )
        val doEvento = SessaoDoCartao.sessoesDaEtapa(
            todas, "F1", LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 25), zona
        )
        assertEquals(listOf("Treino Livre 1", "Classificação", "Corrida"), doEvento.map { it.nome })
    }
}
