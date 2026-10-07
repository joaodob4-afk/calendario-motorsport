package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class F1Activity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var proximoHorario: ZonedDateTime? = null

    private val formatoData =
        DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")

    override fun onCreate(savedInstanceState: Bundle?) {
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

        val layoutPrincipal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(7, 26, 45))
            setPadding(20, 20, 20, 20)
        }

        layoutPrincipal.addView(criarBotaoVoltar())

        val titulo = TextView(this).apply {
            text = "FÓRMULA 1"
            setTextColor(Color.WHITE)
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 28)
        }

        layoutPrincipal.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val scrollView = ScrollView(this).apply {

            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            scrollBarSize = 0

            isScrollbarFadingEnabled = false

            setVerticalFadingEdgeEnabled(false)
            setHorizontalFadingEdgeEnabled(false)

            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(
            conteudo,
            ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
            )
        )

        layoutPrincipal.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(layoutPrincipal)

        val carregando = TextView(this).apply {
            text = "Carregando..."
            setTextColor(Color.WHITE)
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }

        conteudo.addView(carregando)

        carregarEvento(conteudo, carregando)
    }

    private fun criarBotaoVoltar(): TextView {
        return TextView(this).apply {
            text = "‹  VOLTAR"
            setTextColor(Color.rgb(143, 166, 186))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
            setPadding(0, 0, 0, 18)

            isClickable = true
            isFocusable = true

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
        try {
            val raceJson = intent.getStringExtra("RACE")

            if (!raceJson.isNullOrEmpty()) {
                val evento = JSONObject(raceJson)

                layout.removeView(carregando)
                mostrarEvento(layout, evento)

                atualizarContadorTela()
                return
            }

            Thread {
                try {
                    val url =
                        java.net.URL("https://api.jolpi.ca/ergast/f1/current/races/")

                    val conexao =
                        url.openConnection() as java.net.HttpURLConnection

                    conexao.requestMethod = "GET"
                    conexao.connectTimeout = 10000
                    conexao.readTimeout = 10000

                    val resposta =
                        conexao.inputStream.bufferedReader().use {
                            it.readText()
                        }

                    conexao.disconnect()

                    val raiz = JSONObject(resposta)
                    val races = raiz
                        .getJSONObject("MRData")
                        .getJSONObject("RaceTable")
                        .getJSONArray("Races")

                    val agora =
                        ZonedDateTime.now(
                            ZoneId.of("America/Sao_Paulo")
                        )

                    var proximaCorrida: JSONObject? = null
                    var horarioMaisProximo: ZonedDateTime? = null

                    for (i in 0 until races.length()) {
                        val race = races.getJSONObject(i)
                        val date = race.getString("date")
                        val time = race.optString(
                            "time",
                            "00:00:00Z"
                        )

                        val horario =
                            OffsetDateTime.parse(
                                "${date}T${time}"
                            ).atZoneSameInstant(
                                ZoneId.of("America/Sao_Paulo")
                            )

                        if (horario.isAfter(agora)) {
                            proximaCorrida = race
                            horarioMaisProximo = horario
                            break
                        }
                    }

                    runOnUiThread {
                        if (proximaCorrida != null) {
                            proximoHorario = horarioMaisProximo

                            layout.removeView(carregando)

                            mostrarEvento(
                                layout,
                                proximaCorrida!!
                            )

                            atualizarContadorTela()
                        } else {
                            carregando.text =
                                "Nenhuma corrida futura encontrada."
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        carregando.text =
                            "Não foi possível carregar o evento."
                    }
                }
            }.start()

        } catch (e: Exception) {
            carregando.text =
                "Não foi possível carregar o evento."
        }
    }

    private fun mostrarEvento(
        layout: LinearLayout,
        evento: JSONObject
    ) {
        val nome =
            evento.optString(
                "raceName",
                "Grande Prêmio"
            )

        val circuito =
            evento.optJSONObject("Circuit")
                ?.optString(
                    "circuitName",
                    ""
                )
                ?: ""

        val localizacao =
            evento.optJSONObject("Circuit")
                ?.optJSONObject("Location")

        val pais =
            localizacao?.optString(
                "country",
                ""
            ) ?: ""

        val bandeira =
            obterBandeira(pais)

        val cabecalho =
            TextView(this).apply {

                text =
                    "🏁 $nome\n\n" +
                    "📍 $circuito\n" +
                    "$bandeira $pais"

                setTextColor(Color.WHITE)
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER

                setPadding(
                    20,
                    22,
                    20,
                    22
                )

                setBackgroundResource(
                    R.drawable.rounded_card
                )
            }

        layout.addView(
            cabecalho,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        )

        adicionarSessao(
            layout,
            evento,
            "FirstPractice",
            "🟢 TREINO LIVRE 1"
        )

        adicionarSessao(
            layout,
            evento,
            "SecondPractice",
            "🟢 TREINO LIVRE 2"
        )

        adicionarSessao(
            layout,
            evento,
            "ThirdPractice",
            "🟢 TREINO LIVRE 3"
        )

        adicionarSessao(
            layout,
            evento,
            "SprintQualifying",
            "🟡 CLASSIFICAÇÃO SPRINT"
        )

        adicionarSessao(
            layout,
            evento,
            "Sprint",
            "🟡 SPRINT"
        )

        adicionarSessao(
            layout,
            evento,
            "Qualifying",
            "🔵 CLASSIFICAÇÃO"
        )

        adicionarSessao(
            layout,
            evento,
            "date",
            "🔴 CORRIDA"
        )
    }

    private fun adicionarSessao(
        layout: LinearLayout,
        evento: JSONObject,
        chave: String,
        titulo: String
    ) {
        try {
            val data: String
            val hora: String

            if (chave == "date") {
                data = evento.optString("date", "")
                hora = evento.optString(
                    "time",
                    "00:00:00Z"
                )
            } else {
                val sessao =
                    evento.optJSONObject(chave)
                        ?: return

                data =
                    sessao.optString(
                        "date",
                        ""
                    )

                hora =
                    sessao.optString(
                        "time",
                        "00:00:00Z"
                    )
            }

            if (data.isEmpty()) return

            val horario =
                OffsetDateTime.parse(
                    "${data}T${hora}"
                ).atZoneSameInstant(
                    ZoneId.of(
                        "America/Sao_Paulo"
                    )
                )

            if (
                proximoHorario == null ||
                horario.isBefore(proximoHorario)
            ) {
                if (
                    horario.isAfter(
                        ZonedDateTime.now(
                            ZoneId.of(
                                "America/Sao_Paulo"
                            )
                        )
                    )
                ) {
                    proximoHorario = horario
                }
            }

            val card =
                TextView(this).apply {

                    text =
                        "$titulo\n\n" +
                        horario.format(
                            formatoData
                        )

                    setTextColor(Color.WHITE)
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER

                    setPadding(
                        20,
                        20,
                        20,
                        20
                    )

                    setBackgroundResource(
                        R.drawable.rounded_card
                    )
                }

            layout.addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            )

        } catch (_: Exception) {
        }
    }

    private fun atualizarContadorTela() {
        val horario =
            proximoHorario ?: return

        val agora =
            ZonedDateTime.now(
                ZoneId.of(
                    "America/Sao_Paulo"
                )
            )

        val duracao =
            Duration.between(
                agora,
                horario
            )

        if (
            duracao.isNegative ||
            duracao.isZero
        ) {
            handler.postDelayed(
                { atualizarContadorTela() },
                60000
            )
            return
        }

        handler.postDelayed(
            { atualizarContadorTela() },
            60000
        )
    }

    private fun obterBandeira(
        pais: String
    ): String {
        return when (pais.lowercase()) {
            "bahrain" -> "🇧🇭"
            "saudi arabia" -> "🇸🇦"
            "australia" -> "🇦🇺"
            "japan" -> "🇯🇵"
            "china" -> "🇨🇳"
            "usa" -> "🇺🇸"
            "united states" -> "🇺🇸"
            "italy" -> "🇮🇹"
            "monaco" -> "🇲🇨"
            "spain" -> "🇪🇸"
            "canada" -> "🇨🇦"
            "austria" -> "🇦🇹"
            "uk" -> "🇬🇧"
            "united kingdom" -> "🇬🇧"
            "hungary" -> "🇭🇺"
            "belgium" -> "🇧🇪"
            "netherlands" -> "🇳🇱"
            "azerbaijan" -> "🇦🇿"
            "singapore" -> "🇸🇬"
            "mexico" -> "🇲🇽"
            "brazil" -> "🇧🇷"
            "qatar" -> "🇶🇦"
            "uae" -> "🇦🇪"
            "united arab emirates" -> "🇦🇪"
            else -> "🌎"
        }
    }

    private fun buscarCorridasF1(): String {
        return ""
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}