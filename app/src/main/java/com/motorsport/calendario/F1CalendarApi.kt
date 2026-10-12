
package com.motorsport.calendario

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Monta a lista de etapas da F1 a partir da API Jolpica,
 * guarda em cache com gravação segura e nunca lança exceção
 * para a interface do aplicativo.
 */
object F1CalendarApi {

    private const val URL_API =
        "https://api.jolpi.ca/ergast/f1/current/races/?limit=100"

    private const val ARQUIVO_CACHE = "f1_races_cache.json"
    private const val ARQUIVO_TEMPORARIO = "f1_races_cache.tmp"
    private const val MINIMO_CORRIDAS_VALIDAS = 5

    private val MESES = arrayOf(
        "JAN", "FEV", "MAR", "ABR", "MAI", "JUN",
        "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"
    )

    private val CONHECIDOS = mapOf(
        "albert_park" to Pair("Austrália", "Melbourne"),
        "shanghai" to Pair("China", "Xangai"),
        "suzuka" to Pair("Japão", "Suzuka"),
        "bahrain" to Pair("Bahrein", "Sakhir"),
        "jeddah" to Pair("Arábia Saudita", "Jeddah"),
        "miami" to Pair("Estados Unidos", "Miami"),
        "villeneuve" to Pair("Canadá", "Montreal"),
        "monaco" to Pair("Mônaco", "Monte Carlo"),
        "catalunya" to Pair("Espanha", "Barcelona"),
        "red_bull_ring" to Pair("Áustria", "Spielberg"),
        "silverstone" to Pair("Reino Unido", "Silverstone"),
        "spa" to Pair("Bélgica", "Spa-Francorchamps"),
        "hungaroring" to Pair("Hungria", "Budapeste"),
        "zandvoort" to Pair("Holanda", "Zandvoort"),
        "monza" to Pair("Itália", "Monza"),
        "madring" to Pair("Espanha", "Madrid"),
        "baku" to Pair("Azerbaijão", "Baku"),
        "marina_bay" to Pair("Singapura", "Marina Bay"),
        "americas" to Pair("Estados Unidos", "Austin"),
        "rodriguez" to Pair("México", "Cidade do México"),
        "interlagos" to Pair("Brasil", "Interlagos"),
        "vegas" to Pair("Estados Unidos", "Las Vegas"),
        "losail" to Pair("Catar", "Lusail"),
        "yas_marina" to Pair("Abu Dhabi", "Yas Marina")
    )

    private val PAISES = mapOf(
        "Australia" to "Austrália",
        "China" to "China",
        "Japan" to "Japão",
        "Bahrain" to "Bahrein",
        "Saudi Arabia" to "Arábia Saudita",
        "USA" to "Estados Unidos",
        "United States" to "Estados Unidos",
        "Canada" to "Canadá",
        "Monaco" to "Mônaco",
        "Spain" to "Espanha",
        "Austria" to "Áustria",
        "UK" to "Reino Unido",
        "United Kingdom" to "Reino Unido",
        "Great Britain" to "Reino Unido",
        "Belgium" to "Bélgica",
        "Hungary" to "Hungria",
        "Netherlands" to "Holanda",
        "Italy" to "Itália",
        "Azerbaijan" to "Azerbaijão",
        "Singapore" to "Singapura",
        "Mexico" to "México",
        "Brazil" to "Brasil",
        "Qatar" to "Catar",
        "UAE" to "Abu Dhabi",
        "United Arab Emirates" to "Abu Dhabi"
    )

    private val SESSOES = listOf(
        "FirstPractice", "SecondPractice", "ThirdPractice",
        "SprintQualifying", "SprintShootout", "Sprint", "Qualifying"
    )

    fun carregarCache(context: Context): List<MainActivity.Etapa>? {
        return try {
            val arquivo = File(
                context.applicationContext.filesDir,
                ARQUIVO_CACHE
            )

            if (!arquivo.exists() || !arquivo.isFile) {
                return null
            }

            val lista = interpretar(arquivo.readText(Charsets.UTF_8))

            if (lista.size < MINIMO_CORRIDAS_VALIDAS) {
                null
            } else {
                lista
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retorna o JSON de uma corrida específica usando o cache local.
     * Assim a tela de detalhes pode funcionar sem conexão.
     */
    fun corridaJsonDoCache(context: Context, circuitId: String): String? {
        if (circuitId.isBlank()) return null

        return try {
            val arquivo = File(
                context.applicationContext.filesDir,
                ARQUIVO_CACHE
            )
            if (!arquivo.isFile) return null

            val corridas = JSONObject(arquivo.readText(Charsets.UTF_8))
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races")

            for (i in 0 until corridas.length()) {
                val corrida = corridas.getJSONObject(i)
                val id = corrida.optJSONObject("Circuit")
                    ?.optString("circuitId", "") ?: ""
                if (id.equals(circuitId, ignoreCase = true)) {
                    return corrida.toString()
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun atualizar(
        context: Context,
        aoTerminar: (() -> Unit)? = null
    ) {
        val app = context.applicationContext

        Thread {
            var gravou = false
            var conexao: HttpURLConnection? = null

            try {
                conexao = URL(URL_API)
                    .openConnection() as HttpURLConnection

                conexao.connectTimeout = 10000
                conexao.readTimeout = 10000
                conexao.requestMethod = "GET"
                conexao.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                if (conexao.responseCode == HttpURLConnection.HTTP_OK) {
                    val texto = conexao.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }

                    val etapas = interpretar(texto)

                    if (etapas.size >= MINIMO_CORRIDAS_VALIDAS) {
                        gravou = gravarCacheSeguro(
                            app.filesDir,
                            texto
                        )
                    }
                }
            } catch (_: Exception) {
                // Sem internet ou resposta inválida:
                // preserva o cache anterior.
            } finally {
                try {
                    conexao?.disconnect()
                } catch (_: Exception) {
                    // Evita que uma falha ao fechar afete o app.
                }
            }

            if (gravou && aoTerminar != null) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        aoTerminar()
                    } catch (_: Exception) {
                        // A atualização do cache já foi concluída.
                    }
                }
            }
        }.start()
    }

    /**
     * Salva o JSON completo do calendário recebido pela tela, preservando
     * os campos de todas as sessões para consulta offline.
     */
    fun salvarCalendarioCompleto(context: Context, json: String): Boolean {
        return try {
            val corridas = JSONObject(json)
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races")
            if (corridas.length() < MINIMO_CORRIDAS_VALIDAS) {
                return false
            }
            gravarCacheSeguro(context.applicationContext.filesDir, json)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Escreve primeiro em um arquivo temporário no mesmo diretório.
     * Só substitui o cache quando a gravação temporária está completa.
     * Se a substituição falhar, preserva o cache anterior.
     */
    private fun gravarCacheSeguro(
        diretorio: File,
        conteudo: String
    ): Boolean {
        val destino = File(diretorio, ARQUIVO_CACHE)
        val temporario = File(diretorio, ARQUIVO_TEMPORARIO)

        return try {
            if (!diretorio.exists() && !diretorio.mkdirs()) {
                return false
            }

            if (!diretorio.isDirectory) {
                return false
            }

            FileOutputStream(temporario, false).use { saida ->
                saida.write(conteudo.toByteArray(Charsets.UTF_8))
                saida.flush()
                saida.fd.sync()
            }

            // Confere novamente o arquivo escrito antes da troca.
            val textoGravado = temporario.readText(Charsets.UTF_8)
            val etapasGravadas = interpretar(textoGravado)

            if (etapasGravadas.size < MINIMO_CORRIDAS_VALIDAS) {
                temporario.delete()
                return false
            }

            // O temporário está no mesmo diretório do destino.
            // Não apagamos o cache antigo se a troca falhar.
            val substituiu = temporario.renameTo(destino)

            if (!substituiu) {
                temporario.delete()
            }

            substituiu
        } catch (_: Exception) {
            try {
                temporario.delete()
            } catch (_: Exception) {
                // A limpeza não deve afetar o aplicativo.
            }

            false
        }
    }

    private fun interpretar(json: String): List<MainActivity.Etapa> {
        return try {
            val corridas = JSONObject(json)
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races")

            val lista = mutableListOf<MainActivity.Etapa>()

            for (i in 0 until corridas.length()) {
                val corrida = corridas.getJSONObject(i)
                val circuito = corrida.getJSONObject("Circuit")
                val circuitId = circuito.getString("circuitId")
                val local = circuito.optJSONObject("Location")
                val localidade = local?.optString("locality", "") ?: ""
                val paisIngles = local?.optString("country", "") ?: ""

                val fim = try {
                    LocalDate.parse(corrida.getString("date"))
                } catch (_: Exception) {
                    continue
                }

                var inicio = fim.minusDays(2)
                var menor: LocalDate? = null

                for (chave in SESSOES) {
                    val texto = corrida.optJSONObject(chave)
                        ?.optString("date", "") ?: ""

                    if (texto.isBlank()) continue

                    val data = try {
                        LocalDate.parse(texto)
                    } catch (_: Exception) {
                        continue
                    }

                    if (menor == null || data.isBefore(menor)) {
                        menor = data
                    }
                }

                if (
                    menor != null &&
                    !menor.isAfter(fim) &&
                    !menor.isBefore(fim.minusDays(4))
                ) {
                    inicio = menor
                }

                val conhecido = CONHECIDOS[circuitId]
                val pais = conhecido?.first
                    ?: PAISES[paisIngles]
                    ?: paisIngles
                val cidade = conhecido?.second ?: localidade

                lista.add(
                    MainActivity.Etapa(
                        categoria = "F1",
                        pais = pais,
                        circuito = cidade,
                        data = formatarPeriodo(inicio, fim),
                        dataInicio = inicio.toString(),
                        circuitId = circuitId,
                        dataFim = fim.toString()
                    )
                )
            }

            lista.sortedBy { it.dataInicio }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun formatarPeriodo(
        inicio: LocalDate,
        fim: LocalDate
    ): String {
        fun dia(d: LocalDate) =
            d.dayOfMonth.toString().padStart(2, '0')

        fun mes(d: LocalDate) =
            MESES[d.monthValue - 1]

        return when {
            inicio == fim ->
                "${dia(fim)} ${mes(fim)}"

            inicio.monthValue == fim.monthValue ->
                "${dia(inicio)}–${dia(fim)} ${mes(fim)}"

            else ->
                "${dia(inicio)} ${mes(inicio)}–${dia(fim)} ${mes(fim)}"
        }
    }
}