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
}
