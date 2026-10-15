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
    fun f2AceitaReducaoDeEtapasNaMesmaTemporada() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f2_calendar_cache.json")

        try {
            val completo = calendarJson("F2", 2026, 14)
            assertTrue(F2CalendarJson.gravarCacheSeguro(context, completo))
            assertTrue(
                F2CalendarJson.gravarCacheSeguro(
                    context,
                    calendarJson("F2", 2026, 10)
                )
            )
            assertEquals(10, F2CalendarJson.carregar(context).size)
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f2NaoRessuscitaEtapasAusentesNoCalendarioOficial() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f2_calendar_cache.json")
        try {
            assertTrue(F2CalendarJson.gravarCacheSeguro(context, calendarJson("F2", 2026, 10)))
            assertEquals(10, F2CalendarJson.carregar(context).size)
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f2UsaCacheDaTemporadaMaisNova() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f2_calendar_cache.json")
        try {
            // Na virada de temporada o calendário baixado vale mais que o JSON do APK antigo.
            assertTrue(F2CalendarJson.gravarCacheSeguro(context, calendarJson("F2", 2027, 10)))
            assertEquals(10, F2CalendarJson.carregar(context).size)
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f3NaoRessuscitaEtapasAusentesNoCalendarioOficial() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f3_calendar_cache.json")
        try {
            assertTrue(F3CalendarJson.gravarCacheSeguro(context, calendarJson("F3", 2026, 8)))
            assertEquals(8, F3CalendarJson.carregar(context).size)
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f3UsaCacheDaTemporadaMaisNova() {
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
    fun f3AceitaReducaoDeEtapasNaMesmaTemporada() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f3_calendar_cache.json")

        try {
            val completo = calendarJson("F3", 2026, 9)
            assertTrue(F3CalendarJson.gravarCacheSeguro(context, completo))
            assertTrue(
                F3CalendarJson.gravarCacheSeguro(
                    context,
                    calendarJson("F3", 2026, 5)
                )
            )
            assertEquals(5, F3CalendarJson.carregar(context).size)
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f2AceitaRemocaoOuRenomeacaoOficialDeSessao() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f2_calendar_cache.json")

        try {
            val completo = calendarJson("F2", 2026, 14)
            assertTrue(F2CalendarJson.gravarCacheSeguro(context, completo))
            val semSessaoExistente = calendarJson("F2", 2026, 14)
                .replace("\"nome\": \"Corrida\"", "\"nome\": \"Treino\"")

            assertTrue(F2CalendarJson.gravarCacheSeguro(context, semSessaoExistente))
            assertTrue(cache.readText().contains("\"nome\": \"Treino\""))
            assertFalse(cache.readText().contains("\"nome\": \"Corrida\""))
        } finally {
            cache.delete()
            backup.delete()
        }
    }

    @Test
    fun f3RefleteHorarioAtualDaFonteMesmoQuandoViraAConfirmar() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f3_calendar_cache.json")

        try {
            val completo = calendarJson("F3", 2026, 9)
            assertTrue(F3CalendarJson.gravarCacheSeguro(context, completo))
            val semHorario = calendarJson("F3", 2026, 9)
                .replace("\"horario\": \"12:30\"", "\"horario\": \"A confirmar\"")

            assertTrue(F3CalendarJson.gravarCacheSeguro(context, semHorario))
            assertTrue(cache.readText().contains("\"horario\": \"A confirmar\""))
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
    @Test
    fun f2RejeitaJsonDeOutraCategoriaSemAlterarCache() {
        val context = RuntimeEnvironment.getApplication()
        val (cache, backup) = prepareCache(context, "f2_calendar_cache.json")

        try {
            val original = calendarJson("F2", 2026, 14)
            assertTrue(F2CalendarJson.gravarCacheSeguro(context, original))
            val conteudoOriginal = cache.readText()

            assertFalse(
                F2CalendarJson.gravarCacheSeguro(
                    context,
                    calendarJson("F3", 2026, 9)
                )
            )

            assertEquals(conteudoOriginal, cache.readText())
            assertEquals(14, F2CalendarJson.carregar(context).size)
        } finally {
            cache.delete()
            backup.delete()
        }
    }

}
