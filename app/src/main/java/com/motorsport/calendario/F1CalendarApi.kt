package com.motorsport.calendario

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Monta a lista de etapas da F1 a partir da API Jolpica (a mesma que a
 * F1Activity já usa), guarda em cache e nunca lança exceção.
 */
object F1CalendarApi {

    private const val URL_API =
        "https://api.jolpi.ca/ergast/f1/current/races/?limit=100"

    private const val ARQUIVO_CACHE = "f1_races_cache.json"

    private val MESES = arrayOf(
        "JAN", "FEV", "MAR", "ABR", "MAI", "JUN",
        "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"
    )

    // circuitId -> (país, cidade) nos nomes usados pelo app.
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

    // País em inglês (como vem da API) -> nome usado pelo app.
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

    /** Lê o último calendário baixado. Null se não houver cache válido. */
    fun carregarCache(context: Context): List<MainActivity.Etapa>? {
        return try {
            val arquivo = File(context.filesDir, ARQUIVO_CACHE)
            if (!arquivo.exists()) return null

            val lista = interpretar(arquivo.readText())
            if (lista.isEmpty()) null else lista
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Baixa o calendário em segundo plano. Se der certo, guarda no cache
     * e chama aoTerminar na thread principal.
     */
    fun atualizar(context: Context, aoTerminar: (() -> Unit)? = null) {
        val app = context.applicationContext

        Thread {
            var gravou = false

            try {
                val conexao = URL(URL_API).openConnection() as HttpURLConnection
                conexao.connectTimeout = 10000
                conexao.readTimeout = 10000
                conexao.setRequestProperty("Accept", "application/json")

                if (conexao.responseCode == 200) {
                    val texto = conexao.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                    if (interpretar(texto).isNotEmpty()) {
                        File(app.filesDir, ARQUIVO_CACHE).writeText(texto)
                        gravou = true
                    }
                }

                conexao.disconnect()
            } catch (_: Exception) {
                // Sem internet ou resposta inválida: mantém o que já existe.
            }

            if (gravou && aoTerminar != null) {
                Handler(Looper.getMainLooper()).post { aoTerminar() }
            }
        }.start()
    }

    private fun interpretar(json: String): List<MainActivity.Etapa> {
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

            val fim = LocalDate.parse(corrida.getString("date"))

            // Início do fim de semana = primeira sessão (treino/sprint/
            // classificação). Sem dados, assume sexta (2 dias antes).
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

            if (menor != null &&
                !menor.isAfter(fim) &&
                !menor.isBefore(fim.minusDays(4))
            ) {
                inicio = menor
            }

            val conhecido = CONHECIDOS[circuitId]
            val pais = conhecido?.first ?: PAISES[paisIngles] ?: paisIngles
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

        return lista.sortedBy { it.dataInicio }
    }

    fun formatarPeriodo(inicio: LocalDate, fim: LocalDate): String {
        fun dia(d: LocalDate) = d.dayOfMonth.toString().padStart(2, '0')
        fun mes(d: LocalDate) = MESES[d.monthValue - 1]

        return when {
            inicio == fim -> "${dia(fim)} ${mes(fim)}"
            inicio.monthValue == fim.monthValue ->
                "${dia(inicio)}–${dia(fim)} ${mes(fim)}"
            else ->
                "${dia(inicio)} ${mes(inicio)}–${dia(fim)} ${mes(fim)}"
        }
    }
}