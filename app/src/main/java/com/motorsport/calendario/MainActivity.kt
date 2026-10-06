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

    private lateinit var raceInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        raceInfo = findViewById(R.id.raceInfo)
        raceInfo.text = "Carregando calendário da F1..."

        carregarCalendario()
    }

    private fun carregarCalendario() {

        Thread {
            try {

                val url = URL("https://api.jolpi.ca/ergast/f1/current/races/")
                val connection = url.openConnection() as HttpURLConnection

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
                    DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")

                val texto = StringBuilder()

                texto.append("🏎️ FÓRMULA 1\n\n")

                for (i in 0 until races.length()) {

                    val race = races.getJSONObject(i)

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
                    raceInfo.text = texto.toString()
                }

            } catch (e: Exception) {

                runOnUiThread {
                    raceInfo.text =
                        "Erro ao carregar calendário.\n\n" +
                        "${e.javaClass.simpleName}\n\n" +
                        "${e.message}"
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
                hora = race.optString("time", "00:00:00Z")
            } else {

                val sessao = race.getJSONObject(campo)

                data = sessao.getString("date")
                hora = sessao.getString("time")
            }

            val horarioUtc = OffsetDateTime.parse(
                "${data}T${hora}"
            )

            val horarioBrasilia =
                horarioUtc.atZoneSameInstant(brasilia)

            texto.append(nome)
                .append(": ")
                .append(horarioBrasilia.format(formato))
                .append("\n")

        } catch (_: Exception) {
            // Essa sessão não existe nesse GP.
        }
    }
}
