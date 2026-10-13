package com.motorsport.calendario

import android.content.Context
import org.json.JSONObject
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
class F1CachePreservationTest {

    private fun jsonComCorridas(
        quantidade: Int,
        temporada: String = "2026",
        comSessoes: Boolean = false,
        omitirFirstPractice: Boolean = false,
        omitirHorarioFirstPractice: Boolean = false
    ): String {
        val corridas = (1..quantidade).joinToString(",\n") { numero ->
            val dia = numero.toString().padStart(2, '0')
            val sessoes = if (comSessoes) {
                val treino = when {
                    omitirFirstPractice -> ""
                    omitirHorarioFirstPractice -> """
                        ,"FirstPractice":{"date":"$temporada-03-$dia"}
                    """
                    else -> """
                        ,"FirstPractice":{"date":"$temporada-03-$dia","time":"10:00:00Z"}
                    """
                }
                val classificacao = """
                    ,"Qualifying":{"date":"$temporada-03-$dia","time":"14:00:00Z"}
                """
                treino + classificacao
            } else {
                ""
            }
            """
            {
              "season":"$temporada",
              "round":"$numero",
              "raceName":"Grande Prêmio de teste $numero",
              "Circuit":{
                "circuitId":"circuito_teste_$numero",
                "circuitName":"Circuito de teste $numero",
                "Location":{"locality":"Cidade","country":"Brazil"}
              },
              "date":"$temporada-03-$dia"
              $sessoes
            }
            """.trimIndent()
        }

        return """
            {
              "MRData":{
                "series":"f1",
                "RaceTable":{
                  "season":"$temporada",
                  "Races":[$corridas]
                }
              }
            }
        """.trimIndent()
    }

    private fun cacheFile(context: Context) =
        File(context.filesDir, "f1_races_cache.json")

    private fun limpar(context: Context) {
        cacheFile(context).delete()
        File(context.filesDir, "f1_races_cache.tmp").delete()
    }

    @Test
    fun calendarioMenorDaMesmaTemporadaSubstituiCacheQuandoFonteMuda() {
        val context = RuntimeEnvironment.getApplication()
        limpar(context)
        val arquivo = cacheFile(context)

        try {
            assertTrue(
                F1CalendarApi.gravarCacheSeguro(
                    context.filesDir,
                    jsonComCorridas(20, comSessoes = true)
                )
            )
            val original = arquivo.readText()

            assertTrue(
                F1CalendarApi.gravarCacheSeguro(
                    context.filesDir,
                    jsonComCorridas(10)
                )
            )

            assertFalse(original == arquivo.readText())
            val corridas = JSONObject(arquivo.readText())
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races")
            assertEquals(10, corridas.length())
            assertTrue(
                F1CalendarApi.corridaJsonDoCache(context, "circuito_teste_1")
                    ?.contains("FirstPractice") == false
            )
        } finally {
            limpar(context)
        }
    }

    @Test
    fun calendarioBasicoNaoApagaSessoesDaMesmaTemporada() {
        val context = RuntimeEnvironment.getApplication()
        limpar(context)
        val arquivo = cacheFile(context)

        try {
            assertTrue(
                F1CalendarApi.gravarCacheSeguro(
                    context.filesDir,
                    jsonComCorridas(20, comSessoes = true)
                )
            )
            val original = arquivo.readText()

            assertFalse(
                F1CalendarApi.gravarCacheSeguro(
                    context.filesDir,
                    jsonComCorridas(20, comSessoes = false)
                )
            )

            assertEquals(original, arquivo.readText())
        } finally {
            limpar(context)
        }
    }

    @Test
    fun remocaoOficialDeSessaoEhAceita() {
        val context = RuntimeEnvironment.getApplication()
        limpar(context)
        val arquivo = cacheFile(context)

        try {
            assertTrue(
                F1CalendarApi.gravarCacheSeguro(
                    context.filesDir,
                    jsonComCorridas(20, comSessoes = true)
                )
            )
            val original = arquivo.readText()

            assertTrue(
                F1CalendarApi.gravarCacheSeguro(
                    context.filesDir,
                    jsonComCorridas(
                        20,
                        comSessoes = true,
                        omitirFirstPractice = true
                    )
                )
            )
            assertFalse(original == arquivo.readText())
            val corrida = JSONObject(arquivo.readText())
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races").getJSONObject(0)
            assertFalse(corrida.has("FirstPractice"))
        } finally {
            limpar(context)
        }
    }

    @Test
    fun sessaoAindaPublicadaPreservaHorarioSeFonteTemporariamenteOmitirHora() {
        val context = RuntimeEnvironment.getApplication()
        limpar(context)
        val arquivo = cacheFile(context)

        try {
            assertTrue(
                F1CalendarApi.gravarCacheSeguro(
                    context.filesDir,
                    jsonComCorridas(20, comSessoes = true)
                )
            )
            val original = arquivo.readText()

            assertTrue(
                F1CalendarApi.gravarCacheSeguro(
                    context.filesDir,
                    jsonComCorridas(
                        20,
                        comSessoes = true,
                        omitirHorarioFirstPractice = true
                    )
                )
            )
            assertFalse(original == arquivo.readText())
            val corrida = JSONObject(arquivo.readText())
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races").getJSONObject(0)
            assertEquals("10:00:00Z", corrida.getJSONObject("FirstPractice").getString("time"))
        } finally {
            limpar(context)
        }
    }

    @Test
    fun temporadaNovaPodeTerMenosCorridas() {
        val context = RuntimeEnvironment.getApplication()
        limpar(context)
        val arquivo = cacheFile(context)

        try {
            assertTrue(
                F1CalendarApi.gravarCacheSeguro(
                    context.filesDir,
                    jsonComCorridas(20, temporada = "2026", comSessoes = true)
                )
            )
            assertTrue(
                F1CalendarApi.gravarCacheSeguro(
                    context.filesDir,
                    jsonComCorridas(10, temporada = "2027")
                )
            )

            val tabela = JSONObject(arquivo.readText())
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
            assertEquals("2027", tabela.getString("season"))
            assertEquals(10, tabela.getJSONArray("Races").length())
        } finally {
            limpar(context)
        }
    }
}
