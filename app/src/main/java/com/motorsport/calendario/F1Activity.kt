package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class F1Activity : AppCompatActivity() {

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
                    voltarParaTelaAnterior()
                }
            }
        )

        val circuitIdSelecionado =
            intent.getStringExtra(
                "CIRCUIT_ID_F1"
            )

        val selecionada =
            circuitIdSelecionado != null

        val conteudo =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    18,
                    18,
                    18,
                    24
                )

                setBackgroundColor(
                    Color.rgb(
                        7,
                        26,
                        45
                    )
                )
            }

        val voltarTopo =
            criarBotaoVoltar()

        val parametrosVoltarTopo =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosVoltarTopo.setMargins(
            0,
            0,
            0,
            8
        )

        conteudo.addView(
            voltarTopo,
            parametrosVoltarTopo
        )

        val titulo =
            TextView(this).apply {

                text = ""

                setPadding(
                    0,
                    0,
                    0,
                    0
                )
            }

        conteudo.addView(
            titulo
        )

        val carregando =
            TextView(this).apply {

                text =
                    "Carregando programação..."

                textSize = 16f

                setTextColor(
                    Color.rgb(
                        143,
                        166,
                        186
                    )
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    24,
                    0,
                    24
                )
            }

        conteudo.addView(
            carregando
        )

        val scrollView =
            ScrollView(this).apply {

                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                overScrollMode =
                    View.OVER_SCROLL_NEVER

                addView(conteudo)
            }

        setContentView(scrollView)

        carregarEvento(
            conteudo,
            carregando
        )
    }

    private fun criarBotaoVoltar(): TextView {

        return TextView(this).apply {

            text = ""

            gravity =
                Gravity.CENTER

            setPadding(
                0,
                0,
                0,
                0
            )

            background =
                getDrawable(
                    R.drawable.back_button
                )

            isClickable = true
            isFocusable = true

            val parametros =
                LinearLayout.LayoutParams(
                    48,
                    42
                )

            layoutParams =
                parametros

            setOnClickListener {

                voltarParaTelaAnterior()
            }
        }
    }

    private fun voltarParaTelaAnterior() {

        finish()

        overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }

    private fun carregarEvento(
        layout: LinearLayout,
        carregando: TextView
    ) {

        Thread {

            try {

                val circuitIdSelecionado =
                    intent.getStringExtra(
                        "CIRCUIT_ID_F1"
                    )

                val evento: JSONObject?

                if (
                    circuitIdSelecionado != null
                ) {

                    evento =
                        buscarCorridaPorCircuito(
                            circuitIdSelecionado
                        )

                } else {

                    evento =
                        buscarProximaCorrida()
                }

                if (evento == null) {

                    runOnUiThread {

                        carregando.text =
                            "Etapa não encontrada."
                    }

                    return@Thread
                }

                runOnUiThread {

                    layout.removeView(
                        carregando
                    )

                    mostrarEvento(
                        layout,
                        evento
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {

                    carregando.text =
                        "Erro ao carregar evento.\n\n" +
                        e.javaClass.simpleName
                }
            }

        }.start()
    }

    private fun buscarCorridaPorCircuito(
        circuitId: String
    ): JSONObject? {

        val url =
            URL(
                "https://api.jolpi.ca/ergast/f1/2026/circuits/" +
                circuitId +
                "/races/"
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

        val races =
            JSONObject(resposta)
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races")

        if (races.length() == 0) {
            return null
        }

        return races.getJSONObject(
            races.length() - 1
        )
    }

    private fun buscarProximaCorrida():
            JSONObject? {

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

        val races =
            JSONObject(resposta)
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races")

        val agora =
            java.time.Instant.now()

        for (
            i in 0 until races.length()
        ) {

            val race =
                races.getJSONObject(i)

            val data =
                race.getString("date")

            val hora =
                race.optString(
                    "time",
                    "00:00:00Z"
                )

            val horario =
                OffsetDateTime.parse(
                    "${data}T${hora}"
                )

            if (
                horario.toInstant()
                    .isAfter(agora)
            ) {

                return race
            }
        }

        return null
    }

    private fun mostrarEvento(
        layout: LinearLayout,
        race: JSONObject
    ) {

        val brasilia =
            ZoneId.of(
                "America/Sao_Paulo"
            )

        val formato =
            DateTimeFormatter.ofPattern(
                "EEE dd MMM • HH:mm",
                Locale("pt", "BR")
            )

        val nome =
            race.getString(
                "raceName"
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

        val cabecalho =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    0,
                    8,
                    0,
                    22
                )
            }

        val identificacao =
            TextView(this).apply {

                text =
                    "GRAND PRIX"

                textSize = 11f

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

                letterSpacing = 0.16f
            }

        cabecalho.addView(
            identificacao
        )

        val nomeEvento =
            TextView(this).apply {

                text =
                    nome

                textSize = 26f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                letterSpacing = 0.01f

                setPadding(
                    0,
                    5,
                    0,
                    0
                )
            }

        cabecalho.addView(
            nomeEvento
        )

        val divisor =
            View(this).apply {

                setBackgroundColor(
                    Color.rgb(
                        25,
                        183,
                        107
                    )
                )
            }

        val parametrosDivisor =
            LinearLayout.LayoutParams(
                42,
                3
            )

        parametrosDivisor.setMargins(
            0,
            12,
            0,
            12
        )

        cabecalho.addView(
            divisor,
            parametrosDivisor
        )

        val circuitoEvento =
            TextView(this).apply {

                text =
                    circuito

                textSize = 15f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

        cabecalho.addView(
            circuitoEvento
        )

        val localEvento =
            TextView(this).apply {

                text =
                    "$bandeira $cidade • $pais"

                textSize = 13f

                setTextColor(
                    Color.rgb(
                        143,
                        166,
                        186
                    )
                )

                setPadding(
                    0,
                    5,
                    0,
                    0
                )
            }

        cabecalho.addView(
            localEvento
        )

        layout.addView(
            cabecalho
        )

        val tituloProgramacao =
            TextView(this).apply {

                text =
                    "PROGRAMAÇÃO"

                textSize = 12f

                setTextColor(
                    Color.rgb(
                        25,
                        183,
                        107
                    )
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                letterSpacing = 0.12f

                setPadding(
                    0,
                    4,
                    0,
                    7
                )
            }

        layout.addView(
            tituloProgramacao
        )

        adicionarSessao(
            layout,
            race,
            "FirstPractice",
            "TREINO LIVRE 1",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "SecondPractice",
            "TREINO LIVRE 2",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "ThirdPractice",
            "TREINO LIVRE 3",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "SprintQualifying",
            "CLASSIFICAÇÃO SPRINT",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "Sprint",
            "SPRINT",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "Qualifying",
            "CLASSIFICAÇÃO",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "date",
            "CORRIDA",
            brasilia,
            formato
        )
    }

    private fun adicionarSessao(
        layout: LinearLayout,
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

                data =
                    race.getString("date")

                hora =
                    race.optString(
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

            val linha =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    setPadding(
                        0,
                        12,
                        0,
                        12
                    )
                }

            val superior =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL
                }

            val nomeSessao =
                TextView(this).apply {

                    text =
                        nome

                    textSize = 14f

                    setTextColor(
                        Color.WHITE
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    letterSpacing = 0.03f

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                }

            superior.addView(
                nomeSessao
            )

            val horarioSessao =
                TextView(this).apply {

                    text =
                        horarioBrasilia
                            .format(
                                formato
                            )
                            .uppercase(
                                Locale("pt", "BR")
                            )

                    textSize = 13f

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

                    gravity =
                        Gravity.END
                }

            superior.addView(
                horarioSessao
            )

            linha.addView(
                superior
            )

            val divisor =
                View(this).apply {

                    setBackgroundColor(
                        Color.rgb(
                            41,
                            74,
                            99
                        )
                    )
                }

            val parametrosDivisor =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )

            parametrosDivisor.setMargins(
                0,
                12,
                0,
                0
            )

            linha.addView(
                divisor,
                parametrosDivisor
            )

            layout.addView(
                linha
            )

        } catch (_: Exception) {
        }
    }

    private fun obterBandeira(
        pais: String
    ): String {

        return when (pais.lowercase()) {

            "australia" -> "🇦🇺"
            "bahrain" -> "🇧🇭"
            "saudi arabia" -> "🇸🇦"
            "japan" -> "🇯🇵"
            "china" -> "🇨🇳"

            "usa",
            "united states" -> "🇺🇸"

            "italy" -> "🇮🇹"
            "monaco" -> "🇲🇨"
            "spain" -> "🇪🇸"
            "canada" -> "🇨🇦"
            "austria" -> "🇦🇹"

            "united kingdom",
            "uk" -> "🇬🇧"

            "belgium" -> "🇧🇪"
            "hungary" -> "🇭🇺"
            "netherlands" -> "🇳🇱"
            "azerbaijan" -> "🇦🇿"
            "singapore" -> "🇸🇬"
            "mexico" -> "🇲🇽"
            "brazil" -> "🇧🇷"
            "qatar" -> "🇶🇦"

            "uae",
            "united arab emirates" -> "🇦🇪"

            else -> "🌐"
        }
    }
}