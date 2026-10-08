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
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class F1Activity : AppCompatActivity() {

    private val handler =
        Handler(Looper.getMainLooper())

    private var contadorView: TextView? = null

    private var proximoHorario:
            ZonedDateTime? = null

    private val atualizarContador =
        object : Runnable {

            override fun run() {

                atualizarContadorTela()

                handler.postDelayed(
                    this,
                    60_000
                )
            }
        }

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

                text =
                    if (selecionada) {

                        "FÓRMULA 1\n" +
                        "PROGRAMAÇÃO DA ETAPA"

                    } else {

                        "FÓRMULA 1\n" +
                        "PRÓXIMO EVENTO"
                    }

                textSize = 25f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                gravity =
                    Gravity.START

                letterSpacing = 0.04f

                setPadding(
                    2,
                    14,
                    2,
                    22
                )
            }

        conteudo.addView(titulo)

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

        conteudo.addView(carregando)

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

            text =
                "‹  VOLTAR"

            textSize = 14f

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
                Gravity.START

            setPadding(
                2,
                10,
                2,
                10
            )

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

                    handler.post(
                        atualizarContador
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
                "dd/MM/yyyy - HH:mm"
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
            TextView(this).apply {

                text =
                    "$nome\n\n" +
                    circuito + "\n" +
                    "$bandeira $cidade • $pais"

                textSize = 19f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                gravity =
                    Gravity.START

                setPadding(
                    20,
                    22,
                    20,
                    22
                )

                background =
                    getDrawable(
                        R.drawable.rounded_card
                    )
            }

        val parametrosCabecalho =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosCabecalho.setMargins(
            0,
            0,
            0,
            18
        )

        layout.addView(
            cabecalho,
            parametrosCabecalho
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

            val agora =
                ZonedDateTime.now(
                    brasilia
                )

            val card =
                TextView(this).apply {

                    text =
                        "$nome\n\n" +
                        horarioBrasilia.format(
                            formato
                        )

                    textSize = 16f

                    setTextColor(
                        Color.WHITE
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    gravity =
                        Gravity.START

                    setPadding(
                        18,
                        17,
                        18,
                        17
                    )

                    background =
                        getDrawable(
                            R.drawable.session_card
                        )
                }

            val parametros =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            parametros.setMargins(
                0,
                5,
                0,
                5
            )

            layout.addView(
                card,
                parametros
            )

            if (
                proximoHorario == null ||
                (
                    horarioBrasilia.isAfter(agora) &&
                    horarioBrasilia.isBefore(
                        proximoHorario
                    )
                )
            ) {

                if (
                    horarioBrasilia.isAfter(
                        agora
                    )
                ) {

                    proximoHorario =
                        horarioBrasilia

                    contadorView =
                        TextView(this).apply {

                            textSize = 15f

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
                                18,
                                15,
                                18,
                                15
                            )

                            background =
                                getDrawable(
                                    R.drawable.countdown_card
                                )
                        }

                    val parametrosContador =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                    parametrosContador.setMargins(
                        0,
                        2,
                        0,
                        6
                    )

                    layout.addView(
                        contadorView,
                        parametrosContador
                    )
                }
            }

        } catch (_: Exception) {
        }
    }

    private fun atualizarContadorTela() {

        val view =
            contadorView ?: return

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
            duracao.isZero ||
            duracao.isNegative
        ) {

            view.text =
                "A próxima sessão está começando."

            return
        }

        val totalMinutos =
            duracao.toMinutes()

        val dias =
            totalMinutos / 1440

        val horas =
            (totalMinutos % 1440) / 60

        val minutos =
            totalMinutos % 60

        view.text =
            if (dias > 0) {

                "PRÓXIMA SESSÃO  •  " +
                "${dias}d ${horas}h ${minutos}min"

            } else if (horas > 0) {

                "PRÓXIMA SESSÃO  •  " +
                "${horas}h ${minutos}min"

            } else {

                "PRÓXIMA SESSÃO  •  " +
                "${minutos}min"
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

    override fun onDestroy() {

        handler.removeCallbacks(
            atualizarContador
        )

        super.onDestroy()
    }
}