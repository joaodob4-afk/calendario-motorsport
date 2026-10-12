package com.motorsport.calendario

import android.content.Context
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
class F2F3CachePreservationTest {

    private fun calendarJson(category: String, season: Int, count: Int): String {
        val stages = (1..count).joinToString(",\n") { number ->
            """
            {
              "etapa": $number,
              "circuito": "Circuito de teste $number",
              "pais": "Brasil",
              "inicio": "01/05/$season",
              "fim": "03/05/$season",
              "sessoes": [
                {
                  "nome": "Corrida",
                  "data": "02/05/$season",
                  "horario": "12:30"
                }
              ]
            }
            """.trimIndent()
        }

        return """
            {
              "temporada": $season,
              "categoria": "$category",
              "etapas": [
                $stages
              ]
            }
        """.trimIndent()
    }

    private fun prepareCache(context: Context, fileName: String): Pair<File, File> {
        val cache = File(context.filesDir, fileName)
        val backup = File(cache.parentFile, "${cache.name}.bak")
        cache.delete()
        backup.delete()
        return cache to backup
    }

    @Test
    fun f2CalendarioMenorDaMesmaTemporadaNaoSubstituiCache() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f2_calendar_cache.json")

        try {
            val completo = calendarJson("F2", 2026, 14)
            assertTrue(F2CalendarJson.gravarCacheSeguro(context, completo))
            val original = cache.readText()

            assertFalse(
                F2CalendarJson.gravarCacheSeguro(
                    context,
                    calendarJson("F2", 2026, 10)
                )
            )
            assertEquals(original, cache.readText())
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f2CompletaEtapasAusentesComFallbackDaMesmaTemporada() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f2_calendar_cache.json")
        try {
            assertTrue(F2CalendarJson.gravarCacheSeguro(context, calendarJson("F2", 2026, 10)))
            assertEquals(14, F2CalendarJson.carregar(context).size)
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f2NaoMisturaFallbackAntigoComTemporadaNova() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f2_calendar_cache.json")
        try {
            assertTrue(F2CalendarJson.gravarCacheSeguro(context, calendarJson("F2", 2027, 10)))
            assertEquals(10, F2CalendarJson.carregar(context).size)
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f3CompletaEtapasAusentesComFallbackDaMesmaTemporada() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f3_calendar_cache.json")
        try {
            assertTrue(F3CalendarJson.gravarCacheSeguro(context, calendarJson("F3", 2026, 8)))
            assertEquals(9, F3CalendarJson.carregar(context).size)
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f3NaoMisturaFallbackAntigoComTemporadaNova() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f3_calendar_cache.json")
        try {
            assertTrue(F3CalendarJson.gravarCacheSeguro(context, calendarJson("F3", 2027, 5)))
            assertEquals(5, F3CalendarJson.carregar(context).size)
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f2PermiteQuantidadeDiferenteNaNovaTemporada() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f2_calendar_cache.json")

        try {
            assertTrue(
                F2CalendarJson.gravarCacheSeguro(
                    context,
                    calendarJson("F2", 2026, 14)
                )
            )

            assertTrue(
                F2CalendarJson.gravarCacheSeguro(
                    context,
                    calendarJson("F2", 2027, 10)
                )
            )
            assertEquals(2027, org.json.JSONObject(cache.readText()).getInt("temporada"))
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f3CalendarioMenorDaMesmaTemporadaNaoSubstituiCache() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f3_calendar_cache.json")

        try {
            val completo = calendarJson("F3", 2026, 9)
            assertTrue(F3CalendarJson.gravarCacheSeguro(context, completo))
            val original = cache.readText()

            assertFalse(
                F3CalendarJson.gravarCacheSeguro(
                    context,
                    calendarJson("F3", 2026, 5)
                )
            )
            assertEquals(original, cache.readText())
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f2AtualizacaoQueRemoveSessaoNaoSubstituiCache() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f2_calendar_cache.json")

        try {
            val completo = calendarJson("F2", 2026, 14)
            assertTrue(F2CalendarJson.gravarCacheSeguro(context, completo))
            val original = cache.readText()

            val semSessaoExistente = calendarJson("F2", 2026, 14)
                .replace("\"nome\": \"Corrida\"", "\"nome\": \"Treino\"")

            assertFalse(F2CalendarJson.gravarCacheSeguro(context, semSessaoExistente))
            assertEquals(original, cache.readText())
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f3HorarioConhecidoNaoPodeVirarAConfirmar() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f3_calendar_cache.json")

        try {
            val completo = calendarJson("F3", 2026, 9)
            assertTrue(F3CalendarJson.gravarCacheSeguro(context, completo))
            val original = cache.readText()

            val semHorario = calendarJson("F3", 2026, 9)
                .replace("\"horario\": \"12:30\"", "\"horario\": \"A confirmar\"")

            assertFalse(F3CalendarJson.gravarCacheSeguro(context, semHorario))
            assertEquals(original, cache.readText())
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f3PermiteQuantidadeDiferenteNaNovaTemporada() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f3_calendar_cache.json")

        try {
            assertTrue(
                F3CalendarJson.gravarCacheSeguro(
                    context,
                    calendarJson("F3", 2026, 9)
                )
            )

            assertTrue(
                F3CalendarJson.gravarCacheSeguro(
                    context,
                    calendarJson("F3", 2027, 5)
                )
            )
            assertEquals(2027, org.json.JSONObject(cache.readText()).getInt("temporada"))
        } finally {
            cache.delete()
            backup.delete()
        }
    }
}
