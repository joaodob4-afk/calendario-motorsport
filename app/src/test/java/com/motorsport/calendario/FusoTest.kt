package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class FusoTest {

    @Test
    fun mesmoFusoDeBrasiliaNaoMudaNada() {
        val r = Fuso.converter("18/10/2026", "10:00", PlanoDeAvisos.BRASILIA)

        assertEquals("18/10/2026" to "10:00", r)
    }

    @Test
    fun convertePraUtc() {
        // Brasília é UTC-3.
        val r = Fuso.converter("18/10/2026", "10:00", ZoneId.of("UTC"))

        assertEquals("18/10/2026" to "13:00", r)
    }

    @Test
    fun dataMudaQuandoAHoraPassaDaMeiaNoite() {
        // 20:00 em Brasília = 23:00 UTC = 08:00 do dia seguinte no Japão (UTC+9).
        val r = Fuso.converter("18/10/2026", "20:00", ZoneId.of("Asia/Tokyo"))

        assertEquals("19/10/2026" to "08:00", r)
    }

    @Test
    fun dataVoltaQuandoOFusoEstaAtrasDeBrasilia() {
        // 01:00 em Brasília = 04:00 UTC = 18:00 do dia anterior em Honolulu (UTC-10).
        val r = Fuso.converter("18/10/2026", "01:00", ZoneId.of("Pacific/Honolulu"))

        assertEquals("17/10/2026" to "18:00", r)
    }

    @Test
    fun horarioNaoConfirmadoNaoEConvertido() {
        assertNull(Fuso.converter("18/10/2026", "A confirmar", ZoneId.of("UTC")))
        assertNull(Fuso.converter("31/02/2026", "10:00", ZoneId.of("UTC")))
    }

    @Test
    fun sessaoSemHorarioConfirmadoFicaIgual() {
        val sessao = F2Session("Corrida", "18/10/2026", "A confirmar")

        assertEquals(sessao, sessao.noFusoDoAparelho())
    }

    @Test
    fun periodoNoMesmoFusoDeBrasiliaNaoMudaNada() {
        val r = Fuso.periodo(
            "23/10/2026", "25/10/2026",
            listOf("23/10/2026" to "08:00", "25/10/2026" to "14:00"),
            PlanoDeAvisos.BRASILIA
        )

        assertEquals("23/10/2026" to "25/10/2026", r)
    }

    @Test
    fun periodoAvancaUmDiaNoJapao() {
        // 08:00 de sexta em Brasília já é 20:00 de sexta em Tóquio (+12h); 14:00 de domingo vira
        // 02:00 de segunda: a etapa termina um dia depois.
        val r = Fuso.periodo(
            "23/10/2026", "25/10/2026",
            listOf("23/10/2026" to "08:00", "25/10/2026" to "14:00"),
            ZoneId.of("Asia/Tokyo")
        )

        assertEquals("23/10/2026" to "26/10/2026", r)
    }

    @Test
    fun periodoVoltaUmDiaQuandoOFusoEstaAtrasDeBrasilia() {
        // 01:00 de sexta em Brasília ainda é quinta (22:00) em Honolulu.
        val r = Fuso.periodo(
            "23/10/2026", "25/10/2026",
            listOf("23/10/2026" to "01:00", "25/10/2026" to "14:00"),
            ZoneId.of("Pacific/Honolulu")
        )

        assertEquals("22/10/2026" to "25/10/2026", r)
    }

    @Test
    fun periodoSemHorarioConfirmadoFicaIgual() {
        val r = Fuso.periodo(
            "23/10/2026", "25/10/2026",
            listOf("23/10/2026" to "A confirmar"),
            ZoneId.of("Asia/Tokyo")
        )

        assertEquals("23/10/2026" to "25/10/2026", r)
    }

    @Test
    fun periodoComDataInvalidaFicaIgual() {
        val r = Fuso.periodo(
            "31/02/2026", "25/10/2026",
            listOf("23/10/2026" to "08:00"),
            ZoneId.of("Asia/Tokyo")
        )

        assertEquals("31/02/2026" to "25/10/2026", r)
    }

    @Test
    fun dataDaF1UsaOFusoDoAparelho() {
        // Corrida às 02:00 UTC de domingo: ainda é sábado à noite no Brasil.
        assertEquals(
            java.time.LocalDate.of(2026, 3, 14),
            Fuso.dataDaF1("2026-03-15", "02:00:00Z", PlanoDeAvisos.BRASILIA)
        )
        assertEquals(
            java.time.LocalDate.of(2026, 3, 15),
            Fuso.dataDaF1("2026-03-15", "02:00:00Z", ZoneId.of("UTC"))
        )
    }

    @Test
    fun dataDaF1SemHoraUsaAPropriaData() {
        assertEquals(
            java.time.LocalDate.of(2026, 3, 15),
            Fuso.dataDaF1("2026-03-15", "", ZoneId.of("Asia/Tokyo"))
        )
        assertNull(Fuso.dataDaF1("data-ruim", "13:00:00Z", ZoneId.of("UTC")))
    }

    @Test
    fun periodoPelasSessoesCorrigeDatasDoCircuito() {
        // Sepang: cadastrado 30/10 a 01/11 (dias do circuito), mas a 1ª sessão é 29/10 à noite
        // em Brasília. O período tem que bater com a programação.
        val sessoes = listOf("29/10/2026" to "23:45", "01/11/2026" to "04:00")

        assertEquals(
            "29/10/2026" to "01/11/2026",
            Fuso.periodoPelasSessoes("30/10/2026", "01/11/2026", sessoes, PlanoDeAvisos.BRASILIA)
        )
    }

    @Test
    fun periodoPelasSessoesAcompanhaOFusoDoAparelho() {
        val sessoes = listOf("29/10/2026" to "23:45", "01/11/2026" to "04:00")

        // Em Tóquio (+12h em relação a Brasília) a etapa começa em 30/10.
        assertEquals(
            "30/10/2026" to "01/11/2026",
            Fuso.periodoPelasSessoes("30/10/2026", "01/11/2026", sessoes, ZoneId.of("Asia/Tokyo"))
        )
    }

    @Test
    fun periodoPelasSessoesUsaADataCadastradaDaSessaoSemHorario() {
        val sessoes = listOf("23/10/2026" to "08:00", "25/10/2026" to "A confirmar")

        assertEquals(
            "23/10/2026" to "25/10/2026",
            Fuso.periodoPelasSessoes("23/10/2026", "25/10/2026", sessoes, PlanoDeAvisos.BRASILIA)
        )
    }

    @Test
    fun periodoPelasSessoesSemNenhumaConfirmadaFicaIgual() {
        assertEquals(
            "30/10/2026" to "01/11/2026",
            Fuso.periodoPelasSessoes(
                "30/10/2026", "01/11/2026",
                listOf("30/10/2026" to "A confirmar"),
                ZoneId.of("Asia/Tokyo")
            )
        )
    }
}
