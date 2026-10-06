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
        mostrarMenu()
    }

    private fun mostrarMenu() {
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
            abrirCategoria("Formula E")
        }
    }

    private fun abrirCategoria(categoria: String) {
        setContentView(R.layout.activity_category)

        val titulo = findViewById<TextView>(R.id.categoryTitle)
        val evento = findViewById<TextView>(R.id.nextEvent)
        val sessao = findViewById<TextView>(R.id.nextSession)
        val calendario = findViewById<TextView>(R.id.fullCalendarButton)
        val voltar = findViewById<TextView>(R.id.backButton)

        titulo.text = when (categoria) {
            "F1" -> "🏎️ FÓRMULA 1"
            "F2" -> "🏎️ FÓRMULA 2"
            "F3" -> "🏎️ FÓRMULA 3"
            "IndyCar" -> "🏁 INDYCAR"
            else -> "⚡ FÓRMULA E"
        }

        voltar.setOnClickListener {
            mostrarMenu()
        }

        calendario.setOnClickListener {
            evento.text = "Programação completa"
            sessao.text = "Em breve mostraremos todas as sessões."
        }

        if (categoria == "F1") {
            carregarF1(evento, sessao)
        } else {
            evento.text = "Calendário automático"
            sessao.text =
                "Esta categoria será adicionada em breve.\n\n" +
                "• Treinos\n" +
                "• Classificação\n" +
                "• Corrida\n" +
                "• Horários de Brasília"
        }
    }

    private fun carregarF1(
        evento: TextView,
        sessao: TextView
    ) {
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

                val resposta =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                connection.disconnect()

                val races = JSONObject(resposta)
                    .getJSONObject("MRData")
                    .getJSONObject("RaceTable")
                    .getJSONArray("Races")

                val agora = java.time.Instant.now()
                val brasilia = ZoneId.of("America/Sao_Paulo")

                var proximaCorrida: JSONObject? = null
                var proximaData: OffsetDateTime? = null

                for (i in 0 until races.length()) {
                    val race = races.getJSONObject(i)

                    val data = race.getString("date")

                    val hora = race.optString(
                        "time",
                        "00:00:00Z"
                    )

                    val horario = OffsetDateTime.parse(
                        "${data}T${hora}"
                    )

                    if (horario.toInstant().isAfter(agora)) {
                        proximaCorrida = race
                        proximaData = horario
                        break
                    }
                }

                if (proximaCorrida == null) {
                    runOnUiThread {
                        evento.text =
                            "Nenhuma corrida futura encontrada."

                        sessao.text = ""
                    }

                    return@Thread
                }

                val formato =
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy - HH:mm"
                    )

                val horarioBrasilia =
                    proximaData!!
                        .atZoneSameInstant(brasilia)

                val nome =
                    proximaCorrida!!
                        .getString("raceName")

                val dataCorrida =
                    horarioBrasilia.format(formato)

                runOnUiThread {
                    evento.text =
                        "🏁 " + nome + "\n\n" +
                        "🔴 Corrida\n" +
                        "📅 " + dataCorrida

                    sessao.text =
                        "Carregando próxima sessão..."
                }

            } catch (e: Exception) {
                runOnUiThread {
                    evento.text =
                        "Erro ao carregar calendário."

                    sessao.text =
                        "Erro: " +
                        e.javaClass.simpleName
                }
            }
        }.start()
    }
}
