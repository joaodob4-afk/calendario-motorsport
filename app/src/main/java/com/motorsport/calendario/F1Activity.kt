package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class F1Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.BLACK)
        }

        val titulo = TextView(this).apply {
            text = "🏎️ FÓRMULA 1\n\nPROGRAMAÇÃO 2026"
            textSize = 26f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        layout.addView(titulo)

        val carregando = TextView(this).apply {
            text = "Carregando programação..."
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        layout.addView(carregando)

        val voltar = Button(this).apply {
            text = "VOLTAR"
            textSize = 16f

            setOnClickListener {
                finish()
            }
        }

        val parametrosVoltar = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        parametrosVoltar.setMargins(0, 24, 0, 24)

        layout.addView(
            voltar,
            parametrosVoltar
        )

        val scrollView = ScrollView(this).apply {
            addView(layout)
        }

        setContentView(scrollView)

        carregarCalendario(layout, carregando)
    }

    private fun carregarCalendario(
        layout: LinearLayout,
        carregando: TextView
    ) {

        Thread {

            try {

                val races = buscarCorridasF1()

                runOnUiThread {

                    layout.removeView(carregando)

                    for (i in 0 until races.length()) {

                        val race = races.getJSONObject(i)

                        adicionarEtapa(
                            layout,
                            race
                        )
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    carregando.text =
                        "Erro ao carregar programação.\n\n" +
                        e.javaClass.simpleName
                }
            }
        }.start()
    }

    private fun buscarCorridasF1(): org.json.JSONArray {

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
                .use {
                    it.readText()
                }

        connection.disconnect()

        return JSONObject(resposta)
            .getJSONObject("MRData")
            .getJSONObject("RaceTable")
            .getJSONArray("Races")
    }

    private fun adicionarEtapa(
        layout: LinearLayout,
        race: JSONObject
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.DKGRAY)
        }

        val nome = TextView(this).apply {
            text = "🏁 ${race.getString("raceName")}"
            textSize = 21f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        card.addView(nome)

        val brasilia =
            ZoneId.of("America/Sao_Paulo")

        val formato =
            DateTimeFormatter.ofPattern(
                "dd/MM/yyyy - HH:mm"
            )

        adicionarSessao(
            card,
            race,
            "FirstPractice",
            "🟢 TREINO LIVRE 1",
            brasilia,
            formato
        )

        adicionarSessao(
            card,
            race,
            "SecondPractice",
            "🟢 TREINO LIVRE 2",
            brasilia,
            formato
        )

        adicionarSessao(
            card,
            race,
            "ThirdPractice",
            "🟢 TREINO LIVRE 3",
            brasilia,
            formato
        )

        adicionarSessao(
            card,
            race,
            "SprintQualifying",
            "🟡 CLASSIFICAÇÃO SPRINT",
            brasilia,
            formato
        )

        adicionarSessao(
            card,
            race,
            "Sprint",
            "🟡 SPRINT",
            brasilia,
            formato
        )

        adicionarSessao(
            card,
            race,
            "Qualifying",
            "🔵 CLASSIFICAÇÃO",
            brasilia,
            formato
        )

        adicionarSessao(
            card,
            race,
            "date",
            "🔴 CORRIDA",
            brasilia,
            formato
        )

        val parametros = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        parametros.setMargins(0, 0, 0, 18)

        layout.addView(card, parametros)
    }

    private fun adicionarSessao(
        card: LinearLayout,
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

                data =
                    sessao.getString("date")

                hora =
                    sessao.getString("time")
            }

            val horarioUtc =
                OffsetDateTime.parse(
                    "${data}T${hora}"
                )

            val horarioBrasilia =
                horarioUtc.atZoneSameInstant(
                    brasilia
                )

            val texto = TextView(this).apply {

                text =
                    "$nome\n" +
                    "📅 ${horarioBrasilia.format(formato)}"

                textSize = 17f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setPadding(10, 12, 10, 12)
            }

            card.addView(texto)

        } catch (_: Exception) {
        }
    }
}
