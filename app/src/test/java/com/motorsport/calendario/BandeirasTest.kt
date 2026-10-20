package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BandeirasTest {
    @Test
    fun aceitaPortuguesIngles_semDiferenciarCaixaNemEspacos() {
        assertEquals("flag_br", Bandeiras.arquivo("Brasil"))
        assertEquals("flag_br", Bandeiras.arquivo("  brazil "))
        assertEquals("flag_ae", Bandeiras.arquivo("Emirados Árabes Unidos"))
        assertEquals("flag_ae", Bandeiras.arquivo("Abu Dhabi"))
        assertEquals("flag_gb", Bandeiras.arquivo("UK"))
    }

    @Test
    fun paisDesconhecido_naoTemBandeira() {
        assertNull(Bandeiras.arquivo("Atlântida"))
        assertEquals("🌐", Bandeiras.emoji("Atlântida", "🌐"))
    }

    @Test
    fun emojiVemDoCodigoDoPais() {
        assertEquals("\uD83C\uDDE7\uD83C\uDDF7", Bandeiras.emoji("Brasil", "?"))
        assertEquals("\uD83C\uDDEF\uD83C\uDDF5", Bandeiras.emoji("japan", "?"))
    }
}
