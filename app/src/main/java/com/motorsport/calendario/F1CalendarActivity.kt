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

class F1CalendarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.BLACK)
        }

        val titulo = TextView(this).apply {
            text = "🏎️ FÓRMULA 1\n\nCALENDÁRIO 2026"
            textSize = 26f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        layout.addView(titulo)

        val carregando = TextView(this).apply {
            text = "Carregando etapas..."
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

        carregarEtapas(
            layout,
            carregando
        )
    }

    private fun carregarEtapas(
        layout: LinearLayout,
        carregando: TextView
    ) {

        Thread {

            try {

                val races =
                    buscarCorridasF1()

                runOnUiThread {

                    layout.removeView(carregando)

                    for (i in 0 until races.length()) {

                        val race =
                            races.getJSONObject(i)

                        adicionarEtapa(
                            layout,
                            race
                        )
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    carregando.text =
                        "Erro ao carregar calendário.\n\n" +
                        e.javaClass.simpleName
                }
            }
        }.start()
    }

    private fun adicionarEtapa(
        layout: LinearLayout,
        race: JSONObject
    ) {

        val nome =
            race.getString("raceName")

        val circuito =
            race
                .optJSONObject("Circuit")
                ?.optString(
                    "circuitName",
                    ""
                )
                ?: ""

        val localizacao =
            race
                .optJSONObject("Circuit")
                ?.optJSONObject("Location")

        val pais =
            localizacao
                ?.optString(
                    "country",
                    ""
                )
                ?: ""

        val botao = Button(this).apply {

            text =
                "🏁 $nome\n" +
                "📍 $pais"

            textSize = 17f

            setTextColor(Color.WHITE)

            setTypeface(
                null,
                Typeface.BOLD
            )

            setPadding(
                16,
                16,
                16,
                16
            )

            setOnClickListener {

                abrirEtapa(
                    race
                )
            }
        }

        val parametros =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametros.setMargins(
            0,
            6,
            0,
            6
        )

        layout.addView(
            botao,
            parametros
        )
    }

    private fun abrirEtapa(
        race: JSONObject
    ) {

        val intent =
            android.content.Intent(
                this,
                F1Activity::class.java
            )

        intent.putExtra(
            "RACE",
            race.toString()
        )

        startActivity(intent)
    }

    private fun buscarCorridasF1():
        org.json.JSONArray {

        val url =
            URL(
                "https://api.jolpi.ca/ergast/f1/current/races/"
            )

        val connection =
            url.openConnection()
                    as HttpURLConnection

        connection.requestMethod =
            "GET"

        connection.connectTimeout =
            15000

        connection.readTimeout =
            15000

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
}
