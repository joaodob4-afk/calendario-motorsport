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

private fun jsonComEtapas(
    quantidade: Int,
    temporada: Int = 2026,
    horario: String = "12:30",
    incluirTreino: Boolean = false
): String {
    val etapas = (1..quantidade).joinToString(",\n") { numero ->
        val treino = if (incluirTreino) """
            {
              "nome": "Treino",
              "data": "01/05/$temporada",
              "horario": "11:30"
            },
        """.trimIndent() + "\n" else ""
        """
        {
          "etapa": $numero,
          "nome": "Etapa de teste $numero",
          "circuito": "Circuito de teste $numero",
          "local": "Cidade de teste",
          "pais": "Brasil",
          "inicio": "01/05/$temporada",
          "fim": "03/05/$temporada",
          "sessoes": [
            $treino
            {
              "nome": "Corrida",
              "data": "02/05/$temporada",
              "horario": "$horario"
            }
          ]
        }
        """.trimIndent()
    }

    return """
        {
          "temporada": $temporada,
          "categoria": "IndyCar",
          "etapas": [
            $etapas
          ]
        }
    """.trimIndent()
}

private fun jsonValido(): String = jsonComEtapas(10)

private fun jsonComDuasCorridasMilwaukee(
    horarioCorrida1: String,
    horarioCorrida2: String
): String = """
    {
      "temporada": 2026,
      "categoria": "IndyCar",
      "etapas": [
        {
          "etapa": 16,
          "nome": "Snap-on Makers and Fixers 250",
          "circuito": "Milwaukee Mile",
          "local": "West Allis, Wisconsin",
          "pais": "Estados Unidos",
          "inicio": "28/08/2026",
          "fim": "30/08/2026",
          "sessoes": [
            {"nome": "Classificação", "data": "29/08/2026", "horario": "12:00"},
            {"nome": "Corrida 1", "data": "30/08/2026", "horario": "$horarioCorrida1"}
          ]
        },
        {
          "etapa": 17,
          "nome": "Snap-on Milwaukee Mile 250",
          "circuito": "Milwaukee Mile",
          "local": "West Allis, Wisconsin",
          "pais": "Estados Unidos",
          "inicio": "29/08/2026",
          "fim": "30/08/2026",
          "sessoes": [
            {"nome": "Classificação", "data": "29/08/2026", "horario": "12:00"},
            {"nome": "Corrida 2", "data": "30/08/2026", "horario": "$horarioCorrida2"}
          ]
        }
      ]
    }
""".trimIndent()


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

@Test
fun sessaoRemovidaNaMesmaTemporadaPermiteAtualizacaoOficial() {
    val context = RuntimeEnvironment.getApplication()
    val (arquivoCache, arquivoBackup) = prepararArquivos(context)

    try {
        IndyCarCalendarJson.gravarCacheSeValido(
            context,
            jsonComEtapas(18, incluirTreino = true)
        )

        IndyCarCalendarJson.gravarCacheSeValido(
            context,
            jsonComEtapas(18)
        )

        val eventos = IndyCarCalendarJson.carregar(context)
        assertEquals(18, eventos.size)
        assertTrue(eventos.all { evento -> evento.sessoes.none { it.nome == "Treino" } })
    } finally {
        arquivoCache.delete()
        arquivoBackup.delete()
    }
}

@Test
fun refleteHorarioAtualQuandoFonteRetornaAConfirmar() {
    val context = RuntimeEnvironment.getApplication()
    val (arquivoCache, arquivoBackup) = prepararArquivos(context)

    try {
        IndyCarCalendarJson.gravarCacheSeValido(
            context,
            jsonComEtapas(18)
        )

        IndyCarCalendarJson.gravarCacheSeValido(
            context,
            jsonComEtapas(18, horario = "A confirmar")
        )

        val eventos = IndyCarCalendarJson.carregar(context)
        assertEquals("A confirmar", eventos.first().sessoes.first { it.nome == "Corrida" }.horario)
    } finally {
        arquivoCache.delete()
        arquivoBackup.delete()
    }
}

@Test
fun temporadaNovaPodeTerQuantidadeDiferenteDeEtapas() {
    val context = RuntimeEnvironment.getApplication()
    val (arquivoCache, arquivoBackup) = prepararArquivos(context)

    try {
        IndyCarCalendarJson.gravarCacheSeValido(context, jsonComEtapas(18, temporada = 2026))
        IndyCarCalendarJson.gravarCacheSeValido(context, jsonComEtapas(10, temporada = 2027))

        assertEquals(10, IndyCarCalendarJson.carregar(context).size)
    } finally {
        arquivoCache.delete()
        arquivoBackup.delete()
    }
}

@Test
fun calendarioMenorNaMesmaTemporadaPermiteRemocaoOficialDeEtapas() {
    val context = RuntimeEnvironment.getApplication()
    val (arquivoCache, arquivoBackup) = prepararArquivos(context)

    try {
        IndyCarCalendarJson.gravarCacheSeValido(
            context,
            jsonComEtapas(18)
        )

        // Não exige uma quantidade fixa de etapas: a redução pode ser oficial.
        IndyCarCalendarJson.gravarCacheSeValido(
            context,
            jsonComEtapas(9)
        )

        assertEquals(9, IndyCarCalendarJson.carregar(context).size)
    } finally {
        arquivoCache.delete()
        arquivoBackup.delete()
    }
}

@Test
fun refleteHorariosPublicadosNasDuasCorridasDeMilwaukee() {
    val context = RuntimeEnvironment.getApplication()
    val (arquivoCache, arquivoBackup) = prepararArquivos(context)

    try {
        IndyCarCalendarJson.gravarCacheSeValido(
            context,
            jsonComDuasCorridasMilwaukee("19:00", "14:00")
        )

        IndyCarCalendarJson.gravarCacheSeValido(
            context,
            jsonComDuasCorridasMilwaukee("A confirmar", "A confirmar")
        )

        val eventos = IndyCarCalendarJson.carregar(context)
        assertEquals(2, eventos.size)
        assertEquals(
            "A confirmar",
            eventos.first { it.nome == "Snap-on Makers and Fixers 250" }
                .sessoes.first { it.nome == "Corrida 1" }.horario
        )
        assertEquals(
            "A confirmar",
            eventos.first { it.nome == "Snap-on Milwaukee Mile 250" }
                .sessoes.first { it.nome == "Corrida 2" }.horario
        )
    } finally {
        arquivoCache.delete()
        arquivoBackup.delete()
    }
}

}
