package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Test

class DataEmInglesTest {

    @Test
    fun convertePadraoBrasileiro() {
        assertEquals("Oct 23, 2026", Formatacao.dataEmIngles("23/10/2026"))
        assertEquals("Jan 5, 2027", Formatacao.dataEmIngles("05/01/2027"))
    }

    @Test
    fun convertePadraoIso() {
        assertEquals("Oct 23, 2026", Formatacao.dataEmIngles("2026-10-23"))
        assertEquals("Oct 23, 2026", Formatacao.dataEmIngles("2026-10-23T14:00:00"))
    }

    @Test
    fun textoQueNaoEDataFicaIgual() {
        assertEquals("A confirmar", Formatacao.dataEmIngles("A confirmar"))
        assertEquals("", Formatacao.dataEmIngles(""))
        assertEquals("31/02/2026", Formatacao.dataEmIngles("31/02/2026"))
    }
}
