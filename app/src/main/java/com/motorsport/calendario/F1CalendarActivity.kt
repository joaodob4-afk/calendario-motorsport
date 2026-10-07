package com.motorsport.calendario

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class F1CalendarActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    finish()

                    overridePendingTransition(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                }
            }
        )

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    20,
                    20,
                    20,
                    20
                )

                setBackgroundColor(
                    Color.rgb(
                        7,
                        26,
                        45
                    )
                )
            }

        val titulo =
            TextView(this).apply {

                text =
                    "FÓRMULA 1\n\n" +
                    "CALENDÁRIO 2026"

                textSize = 27f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                gravity =
                    Gravity.CENTER

                letterSpacing = 0.04f

                setPadding(
                    0,
                    10,
                    0,
                    28
                )
            }

        layout.addView(titulo)

        val carregando =
            TextView(this).apply {

                text =
                    "Carregando calendário..."

                textSize = 17f

                setTextColor(
                    Color.LTGRAY
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    20,
                    0,
                    20
                )
            }

        layout.addView(carregando)

        val voltar =
            TextView(this).apply {

                text =
                    "‹  VOLTAR"

                textSize = 15f

                setTextColor(
                    Color.rgb(
                        143,
                        166,
                        186
                    )
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setGravity(
                    Gravity.CENTER
                )

                setPadding(
                    0,
                    0,
                    0,
                    20
                )

                setOnClickListener {

                    finish()

                    overridePendingTransition(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                }
            }

        layout.addView(voltar)

        val scroll =
            ScrollView(this).apply {
                addView(layout)
            }

        setContentView(scroll)

        carregarCalendario(
            layout,
            carregando
        )
    }

    private fun carregarCalendario(
        layout: LinearLayout,
        carregando: TextView
    ) {

        Thread {

            try {

                val corridas =
                    buscarCorridasF1()

                runOnUiThread {

                    layout.removeView(
                        carregando
                    )

                    for (
                        i in 0 until corridas.length()
                    ) {

                        val race =
                            corridas.getJSONObject(
                                i
                            )

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
                        "Erro: " +
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
            race.optString(
                "raceName",
                "Grande Prêmio"
            )

        val circuito =
            race
                .optJSONObject("Circuit")
                ?.optString(
                    "circuitName",
                    "Circuito não informado"
                )
                ?: "Circuito não informado"

        val localizacao =
            race
                .optJSONObject("Circuit")
                ?.optJSONObject("Location")

        val cidade =
            localizacao
                ?.optString(
                    "local",
                    ""
                )
                ?: ""

        val pais =
            localizacao
                ?.optString(
                    "country",
                    ""
                )
                ?: ""

        val bandeira =
            obterBandeira(pais)

        val data =
            race.optString(
                "date",
                ""
            )

        val hora =
            race.optString(
                "time",
                ""
            )

        val dataHoraBrasil =
            converterParaBrasilia(
                data,
                hora
            )

        val card =
            TextView(this).apply {

                text =
                    "🏁 $nome\n\n" +
                    "📍 $circuito\n" +
                    "$bandeira $cidade • $pais\n\n" +
                    "📅 $dataHoraBrasil"

                textSize = 17f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    20,
                    20,
                    20,
                    20
                )

                background =
                    getDrawable(
                        R.drawable.rounded_card
                    )

                isClickable = true
                isFocusable = true

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
            card,
            parametros
        )
    }

    private fun converterParaBrasilia(
        data: String,
        hora: String
    ): String {

        return try {

            val horarioUtc =
                OffsetDateTime.parse(
                    "${data}T$hora"
                )

            val brasilia =
                ZoneId.of(
                    "America/Sao_Paulo"
                )

            val horarioBrasil =
                horarioUtc.atZoneSameInstant(
                    brasilia
                )

            val formato =
                DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy - HH:mm"
                )

            horarioBrasil.format(
                formato
            )

        } catch (_: Exception) {

            if (data.isNotEmpty()) {

                try {

                    val dataFormatada =
                        java.time.LocalDate
                            .parse(data)
                            .format(
                                DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy"
                                )
                            )

                    dataFormatada

                } catch (_: Exception) {

                    data
                }

            } else {

                "Data não informada"
            }
        }
    }

    private fun obterBandeira(
        pais: String
    ): String {

        return when (
            pais.lowercase()
        ) {

            "australia" -> "🇦🇺"

            "japan" -> "🇯🇵"

            "bahrain" -> "🇧🇭"

            "saudi arabia" -> "🇸🇦"

            "usa" -> "🇺🇸"

            "united states" -> "🇺🇸"

            "italy" -> "🇮🇹"

            "monaco" -> "🇲🇨"

            "spain" -> "🇪🇸"

            "canada" -> "🇨🇦"

            "austria" -> "🇦🇹"

            "uk" -> "🇬🇧"

            "united kingdom" -> "🇬🇧"

            "belgium" -> "🇧🇪"

            "hungary" -> "🇭🇺"

            "netherlands" -> "🇳🇱"

            "azerbaijan" -> "🇦🇿"

            "singapore" -> "🇸🇬"

            "mexico" -> "🇲🇽"

            "brazil" -> "🇧🇷"

            "qatar" -> "🇶🇦"

            "uae" -> "🇦🇪"

            "united arab emirates" -> "🇦🇪"

            else -> "🏳️"
        }
    }

    private fun abrirEtapa(
        race: JSONObject
    ) {

        val intent =
            Intent(
                this,
                F1Activity::class.java
            )

        intent.putExtra(
            "RACE",
            race.toString()
        )

        startActivity(
            intent
        )

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun buscarCorridasF1():
        JSONArray {

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