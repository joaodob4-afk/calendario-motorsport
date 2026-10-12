package com.motorsport.calendario

import android.content.Context
import android.util.AtomicFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class IndyCarCachePreservationTest {

    private fun jsonValido(): String {
        return """
            {
              "temporada": 2026,
              "categoria": "IndyCar",
              "etapas": [
                {
                  "etapa": 1,
                  "nome": "Etapa de teste",
                  "circuito": "Circuito de teste",
                  "local": "Cidade de teste",
                  "pais": "Brasil",
                  "inicio": "01/05/2026",
                  "fim": "03/05/2026",
                  "sessoes": [
                    {
                      "nome": "Corrida",
                      "data": "02/05/2026",
                      "horario": "12:30"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
    }

    private fun prepararArquivos(
        context: Context
    ): Pair<File, File> {
        val arquivoCache = File(
            context.filesDir,
            "indycar_calendar_cache.json"
        )

        val arquivoBackup = File(
            arquivoCache.parentFile,
            "${arquivoCache.name}.bak"
        )

        arquivoCache.delete()
        arquivoBackup.delete()

        return Pair(arquivoCache, arquivoBackup)
    }

    @Test
    fun jsonInvalidoNaoSubstituiCacheValido() {
        val context = RuntimeEnvironment.getApplication()
        val (arquivoCache, arquivoBackup) =
            prepararArquivos(context)

        try {
            IndyCarCalendarJson.gravarCacheSeValido(
                context,
                jsonValido()
            )

            assertTrue(arquivoCache.exists())

            val conteudoOriginal = arquivoCache.readText()

            val jsonInvalido = """
                {
                  "temporada": 2026,
                  "categoria": "IndyCar",
                  "etapas": []
                }
            """.trimIndent()

            assertThrows(IllegalArgumentException::class.java) {
                IndyCarCalendarJson.gravarCacheSeValido(
                    context,
                    jsonInvalido
                )
            }

            assertEquals(
                conteudoOriginal,
                arquivoCache.readText()
            )
        } finally {
            arquivoCache.delete()
            arquivoBackup.delete()
        }
    }

    @Test
    fun falhaControladaNaGravacaoPreservaCacheAnterior() {
        val context = RuntimeEnvironment.getApplication()
        val (arquivoCache, arquivoBackup) =
            prepararArquivos(context)

        try {
            IndyCarCalendarJson.gravarCacheSeValido(
                context,
                jsonValido()
            )

            val conteudoOriginal = arquivoCache.readText()
            val atomicFile = AtomicFile(arquivoCache)

            val jsonNovoIncompleto =
                """{"temporada":2027,"etapas":["INCOMPLETO"]}"""

            assertThrows(IOException::class.java) {
                IndyCarCalendarJson.gravarAtomicamente(
                    atomicFile,
                    jsonNovoIncompleto
                ) { saida, bytes ->
                    saida.write(
                        bytes,
                        0,
                        bytes.size / 2
                    )

                    throw IOException(
                        "Falha de escrita simulada"
                    )
                }
            }

            val conteudoRecuperado = AtomicFile(arquivoCache)
                .openRead()
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            assertEquals(
                conteudoOriginal,
                conteudoRecuperado
            )

            assertEquals(
                conteudoOriginal,
                arquivoCache.readText()
            )
        } finally {
            arquivoCache.delete()
            arquivoBackup.delete()
        }
    }

    @Test
    fun recuperaCacheValidoAposGravacaoInterrompida() {
        val context = RuntimeEnvironment.getApplication()
        val (arquivoCache, arquivoBackup) =
            prepararArquivos(context)

        try {
            IndyCarCalendarJson.gravarCacheSeValido(
                context,
                jsonValido()
            )

            val conteudoOriginal = arquivoCache.readText()
            val atomicFile = AtomicFile(arquivoCache)
            val escrita = atomicFile.startWrite()

            try {
                escrita.write(
                    """{"temporada":2026,"etapas":["INCOMPLETO"]}"""
                        .toByteArray(Charsets.UTF_8)
                )
                escrita.flush()
            } finally {
                escrita.close()
            }

            val conteudoRecuperado = AtomicFile(arquivoCache)
                .openRead()
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            assertEquals(
                conteudoOriginal,
                conteudoRecuperado
            )

            assertEquals(
                conteudoOriginal,
                arquivoCache.readText()
            )
        } finally {
            arquivoCache.delete()
            arquivoBackup.delete()
        }
    }
}