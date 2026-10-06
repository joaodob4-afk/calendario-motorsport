package com.motorsport.calendario

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.btnF1).setOnClickListener {
            abrirCategoria("F1")
        }

        findViewById<TextView>(R.id.btnF2).setOnClickListener {
            abrirCategoria("F2")
        }

        findViewById<TextView>(R.id.btnF3).setOnClickListener {
            abrirCategoria("F3")
        }

        findViewById<TextView>(R.id.btnIndyCar).setOnClickListener {
            abrirCategoria("IndyCar")
        }

        findViewById<TextView>(R.id.btnFormulaE).setOnClickListener {
            abrirCategoria("Fórmula E")
        }
    }

    private fun abrirCategoria(categoria: String) {

        setContentView(R.layout.activity_category)

        val titulo = findViewById<TextView>(R.id.categoryTitle)
        val info = findViewById<TextView>(R.id.categoryInfo)

        titulo.text = when (categoria) {
            "F1" -> "🏎️ FÓRMULA 1"
            "F2" -> "🏎️ FÓRMULA 2"
            "F3" -> "🏎️ FÓRMULA 3"
            "IndyCar" -> "🏁 INDYCAR"
            else -> "⚡ FÓRMULA E"
        }

        info.text = "Carregando calendário..."

        if (categoria == "F1") {
            carregarF1(info)
        } else {
            info.text = """
                Calendário automático em preparação.

                Em breve esta categoria terá:
                • Treinos
                • Classificação
                • Corrida
                • Horários de Brasília
            """.trimIndent()
        }
    }

    private fun carregarF1(info: TextView) {

        Thread {

            try {

                val url = URL(
                    "https://api.jolpi.ca/ergast/f1/current/races/"
                )

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                connection.setRequestProperty(
                    "User-Agent",
                    "CalendarioMotorsport/1.0"
                )

                val resposta = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                val json = JSONObject(resposta)

                val races = json
                    .getJSONObject("MRData")
                    .getJSONObject("RaceTable")
                    .getJSONArray("Races")

                val brasilia = ZoneId.of("America/Sao_Paulo")

                val formato =
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy - HH:mm"
                    )

                val texto = StringBuilder()

                texto.append("PRÓXIMOS EVENTOS\n\n")

                for (i in 0 until races.length()) {

                    val race =
                        races.getJSONObject(i)

                    texto.append("🏁 ")
                        .append(race.getString("raceName"))
                        .append("\n\n")

                    adicionarSessao(
                        texto,
                        race,
                        "FirstPractice",
                        "🟢 Treino Livre 1",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "SecondPractice",
                        "🟢 Treino Livre 2",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "ThirdPractice",
                        "🟢 Treino Livre 3",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "SprintQualifying",
                        "🟡 Classificação Sprint",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "Sprint",
                        "🟡 Sprint",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "Qualifying",
                        "🔵 Classificação",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "date",
                        "🔴 Corrida",
                        brasilia,
                        formato
                    )

                    texto.append("\n")
                }

                runOnUiThread {
                    info.text = texto.toString()
                }

            } catch (e: Exception) {

                runOnUiThread {
                    info.text =
                        "Erro ao carregar calendário.\n\n" +
                        e.javaClass.simpleName +
                        "\n\n" +
                        e.message
                }
            }

        }.start()
    }

    private fun adicionarSessao(
        texto: StringBuilder,
        race: JSONObject,
        campo: String,
        nome: String,
        brasilia: ZoneId,
        formato: DateTimeFormatter
    ) {

        try {

            val data: String
            val hora: String

            if (campo == "date") {

                data = race.getString("date")
                hora = race.optString(
                    "time",
                    "00:00:00Z"
                )

            } else {

                val sessao =
                    race.getJSONObject(campo)

                data = sessao.getString("date")
                hora = sessao.getString("time")
            }

            val horarioUtc =
                OffsetDateTime.parse(
                    "${data}T${hora}"
                )

            val horarioBrasilia =
                horarioUtc.atZoneSameInstant(
                    brasilia
                )

            texto.append(nome)
                .append(": ")
                .append(
                    horarioBrasilia.format(formato)
                )
                .append("\n")

        } catch (_: Exception) {
            // Sessão não disponível neste GP.
        }
    }
}
