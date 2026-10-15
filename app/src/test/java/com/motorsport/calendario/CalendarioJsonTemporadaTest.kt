package com.motorsport.calendario

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarioJsonTemporadaTest {
    private fun json(temporada: String) =
        """{"temporada":"$temporada","etapas":[]}"""

    @Test
    fun usaAssetQuandoCacheEDeTemporadaAnterior() {
        assertEquals(json("2026"), CalendarioJsonTemporada.selecionar(json("2025"), json("2026")))
    }

    @Test
    fun preservaCacheQuandoEleEDeTemporadaMaisNova() {
        assertEquals(json("2027"), CalendarioJsonTemporada.selecionar(json("2027"), json("2026")))
    }

    @Test
    fun cacheDeTemporadaMaisNovaVenceOAssetEmTodasAsCategorias() {
        // Vale também para F2 e F3: na virada de temporada o app não fica preso ao APK antigo.
        assertEquals(json("2028"), CalendarioJsonTemporada.selecionar(json("2028"), json("2026")))
    }

    @Test
    fun preservaCacheDaMesmaTemporada() {
        assertEquals(json("2026/27"), CalendarioJsonTemporada.selecionar(json("2026/27"), json("2026/27")))
    }

    @Test
    fun usaAssetQuandoNaoExisteCache() {
        assertEquals(json("2026"), CalendarioJsonTemporada.selecionar(null, json("2026")))
    }

    @Test
    fun usaCacheQuandoAssetNaoExiste() {
        assertEquals(json("2026"), CalendarioJsonTemporada.selecionar(json("2026"), null))
    }
}
