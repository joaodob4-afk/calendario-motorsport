package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Test

class HoraTest {

    @Test
    fun em24Horas_mantemOFormatoDosCalendarios() {
        assertEquals("14:30", Hora.deTexto("14:30", true))
        assertEquals("09:05", Hora.deTexto("09:05", true))
        assertEquals("00:00", Hora.deTexto("0:00", true))
    }

    @Test
    fun em12Horas_usaAMePM() {
        assertEquals("2:30 PM", Hora.deTexto("14:30", false))
        assertEquals("9:05 AM", Hora.deTexto("09:05", false))
        assertEquals("11:59 PM", Hora.deTexto("23:59", false))
    }

    @Test
    fun meiaNoiteEMeioDia_em12Horas() {
        assertEquals("12:00 AM", Hora.deTexto("00:00", false))
        assertEquals("12:05 AM", Hora.deTexto("00:05", false))
        assertEquals("12:00 PM", Hora.deTexto("12:00", false))
        assertEquals("12:45 PM", Hora.deTexto("12:45", false))
    }

    @Test
    fun textoQueNaoEHoraVoltaIgual() {
        assertEquals("a confirmar", Hora.deTexto("a confirmar", false))
        assertEquals("25:00", Hora.deTexto("25:00", false))
        assertEquals("10:75", Hora.deTexto("10:75", true))
        assertEquals("", Hora.deTexto("", false))
    }

    @Test
    fun formatarDireto() {
        assertEquals("7:08 AM", Hora.formatar(7, 8, false))
        assertEquals("07:08", Hora.formatar(7, 8, true))
        assertEquals("1:00 PM", Hora.formatar(13, 0, false))
    }
}
