package com.motorsport.calendario

import android.content.Context
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** Garante que guardar o resultado da leitura não impede de enxergar um calendário novo. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FonteDeCalendarioMemoriaTest {

    private val arquivoCache = "teste_memoria_cache.json"
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private var chamadas = 0

    private fun interpretar(texto: String): List<String> {
        chamadas++
        val etapas = JSONObject(texto).getJSONArray("etapas")
        require(etapas.length() > 0) { "calendário sem etapas" }
        return (0 until etapas.length()).map { etapas.get(it).toString() }
    }

    private fun json(temporada: Int, etapas: Int): String =
        """{"temporada": $temporada, "etapas": [${(1..etapas).joinToString(",")}]}"""

    private fun fonte() =
        FonteDeCalendario("Teste", "http://localhost/nao-usado", "nao_existe.json", arquivoCache) {
            interpretar(it)
        }

    @Before
    fun limpar() {
        chamadas = 0
        File(context.filesDir, arquivoCache).delete()
        File(context.filesDir, "$arquivoCache.bak").delete()
    }

    @Test
    fun calendarioNovoSubstituiOAnteriorMesmoDepoisDeLer() {
        val fonte = fonte()

        assertEquals(true, fonte.gravarCache(context, json(2026, 3)))
        assertEquals(3, fonte.carregar(context).size)

        assertEquals(true, fonte.gravarCache(context, json(2026, 5)))
        assertEquals(5, fonte.carregar(context).size)
    }

    @Test
    fun mesmoTextoEInterpretadoUmaSoVez() {
        val fonte = fonte()

        fonte.gravarCache(context, json(2026, 3))
        fonte.carregar(context)
        fonte.carregar(context)
        fonte.carregar(context)

        assertEquals(1, chamadas)
    }

    @Test
    fun textoInvalidoContinuaInvalidoNaSegundaTentativa() {
        val fonte = fonte()
        val invalido = """{"temporada": 2026, "etapas": []}"""

        assertFalse(fonte.gravarCache(context, invalido))
        assertFalse(fonte.gravarCache(context, invalido))
        assertEquals(0, fonte.carregar(context).size)
    }
}
