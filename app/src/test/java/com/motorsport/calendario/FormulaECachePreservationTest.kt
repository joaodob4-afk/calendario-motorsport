package com.motorsport.calendario

import android.content.Context
import android.util.AtomicFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FormulaECachePreservationTest {

    private fun jsonComEtapas(quantidade: Int, temporada: String = "2026/27"): String {
        var rodada = 1
        val etapas = (1..quantidade).joinToString(",\n") { numero ->
            val quantidadeCorridas = if (numero <= 8 && quantidade >= 13) 2 else 1
            val sessoes = (1..quantidadeCorridas).joinToString(",\n") {
                val nome = "Corrida (Rodada ${rodada++})"
                """{"nome":"$nome","data":"18/12/2026","horario":"A confirmar"}"""
            }
            """
            {
              "etapa": $numero,
              "circuito": "Circuito $numero",
              "pais": "Brasil",
              "inicio": "18/12/2026",
              "fim": "19/12/2026",
              "sessoes": [$sessoes]
            }
            """.trimIndent()
        }

        return """
            {
              "temporada": "$temporada",
              "categoria": "Formula E",
              "etapas": [$etapas]
            }
        """.trimIndent()
    }

    private fun cacheFile(context: Context): File =
        File(context.filesDir, "fe_calendar_cache.json")

    private fun limpar(context: Context) {
        val arquivo = cacheFile(context)
        arquivo.delete()
        File(arquivo.parentFile, "${arquivo.name}.bak").delete()
        File(arquivo.parentFile, "${arquivo.name}.new").delete()
    }

    @Test
    fun calendarioMenorDaMesmaTemporadaNaoSubstituiCache() {
        val context = RuntimeEnvironment.getApplication()
        limpar(context)
        val arquivo = cacheFile(context)

        try {
            assertTrue(FormulaECalendarJson.gravarCacheSeValido(context, jsonComEtapas(15)))
            val original = arquivo.readText()

            assertFalse(FormulaECalendarJson.gravarCacheSeValido(context, jsonComEtapas(13)))
            assertEquals(original, AtomicFile(arquivo).openRead().bufferedReader().use { it.readText() })
            assertEquals(15, FormulaECalendarJson.carregar(context).size)
        } finally {
            limpar(context)
        }
    }

    @Test
    fun temporadaNovaPodeTerQuantidadeDiferenteDeEtapas() {
        val context = RuntimeEnvironment.getApplication()
        limpar(context)
        val arquivo = cacheFile(context)

        try {
            assertTrue(FormulaECalendarJson.gravarCacheSeValido(context, jsonComEtapas(15, "2026/27")))
            assertTrue(FormulaECalendarJson.gravarCacheSeValido(context, jsonComEtapas(13, "2027/28")))
            assertEquals(13, FormulaECalendarJson.carregar(context).size)
        } finally {
            limpar(context)
        }
    }
}
