package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
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
import java.time.format.DateTimeFormatter

class F1Activity : AppCompatActivity() {

    private val handler =
        Handler(Looper.getMainLooper())

    private var contadorView: TextView? = null

    private var proximoHorario:
            OffsetDateTime? = null

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

        val corridaSelecionada =
            intent.getStringExtra("RACE")

        val conteudo =
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

        // BOTÃO VOLTAR - TOPO ESQUERDO

        val voltarTopo =
            criarBotaoVoltar()

        val parametrosTopo =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosTopo.gravity =
            Gravity.LEFT

        parametrosTopo.setMargins(
            0,
            0,
            0,
            10
        )

        conteudo.addView(
            voltarTopo,
            parametrosTopo
        )

        val titulo =
            TextView(this).apply {

                text =
                    if (
                        corridaSelecionada != null
                    ) {

                        "FÓRMULA 1\n\n" +
                        "PROGRAMAÇÃO DA ETAPA"

                    } else {

                        "FÓRMULA 1\n\n" +
                        "PRÓXIMO EVENTO"
                    }

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

        conteudo.addView(
            titulo
        )

        val carregando =
            TextView(this).apply {

                text =
                    "Carregando programação..."

                textSize = 17f

                setTextColor(
                    Color.LTGRAY
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    30,
                    0,
                    30
                )
            }

        conteudo.addView(
            carregando
        )

        val scrollView =
            ScrollView(this).apply {

                fillViewport = true

                addView(
                    conteudo
                )
            }

        setContentView(
            scrollView
        )

        carregarCalendario(
            conteudo,
            carregando,
            corridaSelecionada
        )
    }

    private fun carregarCalendario(
        layout: LinearLayout,
        carregando: TextView,
        corridaSelecionada: String?
    ) {

        Thread {

            try {

                val races =
                    buscarCorridasF1()

                val brasilia =
                    ZoneId.of(
                        "America/Sao_Paulo"
                    )

                val formato =
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy - HH:mm"
                    )

                var corridaEncontrada:
                        JSONObject? = null

                var horarioCorrida:
                        OffsetDateTime? = null

                val agora =
                    java.time.Instant.now()

                for (
                    i in 0 until races.length()
                ) {

                    val race =
                        races.getJSONObject(i)

                    val nome =
                        race.getString(
                            "raceName"
                        )

                    if (
                        corridaSelecionada != null &&
                        nome != corridaSelecionada
                    ) {
                        continue
                    }

                    val data =
                        race.getString(
                            "date"
                        )

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
                        corridaSelecionada != null ||
                        horario.toInstant()
                            .isAfter(agora)
                    ) {

                        corridaEncontrada =
                            race

                        horarioCorrida =
                            horario

                        break
                    }
                }

                if (
                    corridaEncontrada == null
                ) {

                    runOnUiThread {

                        carregando.text =
                            "Nenhuma corrida encontrada."

                        adicionarBotaoVoltarFundo(
                            layout
                        )
                    }

                    return@Thread
                }

                val race =
                    corridaEncontrada

                val nome =
                    race!!.getString(
                        "raceName"
                    )

                val circuito =
                    race
                        .getJSONObject(
                            "Circuit"
                        )

                val nomeCircuito =
                    circuito.getString(
                        "circuitName"
                    )

                val local =
                    circuito
                        .getJSONObject(
                            "Location"
                        )

                val cidade =
                    local.getString(
                        "locality"
                    )

                val pais =
                    local.getString(
                        "country"
                    )

                val bandeira =
                    obterBandeira(
                        pais
                    )

                val horarioCorridaBrasilia =
                    horarioCorrida!!
                        .atZoneSameInstant(
                            brasilia
                        )

                runOnUiThread {

                    layout.removeView(
                        carregando
                    )

                    val cabecalho =
                        TextView(this).apply {

                            text =
                                "🏁 $nome\n\n" +
                                "🏎️ $nomeCircuito\n" +
                                "$bandeira $cidade, $pais"

                            textSize = 19f

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
                                16,
                                22,
                                16,
                                26
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
                        20
                    )

                    layout.addView(
                        cabecalho,
                        parametrosCabecalho
                    )

                    adicionarSessao(
                        layout,
                        "🔴  CORRIDA",
                        horarioCorridaBrasilia
                    )

                    val qualificacao =
                        race.optJSONObject(
                            "Qualifying"
                        )

                    if (
                        qualificacao != null
                    ) {

                        val data =
                            qualificacao.getString(
                                "date"
                            )

                        val hora =
                            qualificacao.getString(
                                "time"
                            )

                        val horario =
                            OffsetDateTime.parse(
                                "${data}T${hora}"
                            )

                        adicionarSessao(
                            layout,
                            "🔵  CLASSIFICAÇÃO",
                            horario.atZoneSameInstant(
                                brasilia
                            ).toOffsetDateTime()
                        )
                    }

                    val sprint =
                        race.optJSONObject(
                            "Sprint"
                        )

                    if (
                        sprint != null
                    ) {

                        val data =
                            sprint.getString(
                                "date"
                            )

                        val hora =
                            sprint.getString(
                                "time"
                            )

                        val horario =
                            OffsetDateTime.parse(
                                "${data}T${hora}"
                            )

                        adicionarSessao(
                            layout,
                            "🟡  SPRINT",
                            horario.atZoneSameInstant(
                                brasilia
                            ).toOffsetDateTime()
                        )
                    }

                    val sprintQualifying =
                        race.optJSONObject(
                            "SprintQualifying"
                        )

                    if (
                        sprintQualifying != null
                    ) {

                        val data =
                            sprintQualifying.getString(
                                "date"
                            )

                        val hora =
                            sprintQualifying.getString(
                                "time"
                            )

                        val horario =
                            OffsetDateTime.parse(
                                "${data}T${hora}"
                            )

                        adicionarSessao(
                            layout,
                            "🟡  CLASSIFICAÇÃO SPRINT",
                            horario.atZoneSameInstant(
                                brasilia
                            ).toOffsetDateTime()
                        )
                    }

                    iniciarContador()

                    // =================================================
                    // BOTÃO VOLTAR - VERDADEIRAMENTE NO FINAL
                    // =================================================

                    adicionarBotaoVoltarFundo(
                        layout
                    )
                }

            } catch (
                e: Exception
            ) {

                runOnUiThread {

                    layout.removeView(
                        carregando
                    )

                    val erro =
                        TextView(this).apply {

                            text =
                                "Erro ao carregar programação.\n\n" +
                                e.javaClass.simpleName

                            textSize = 17f

                            setTextColor(
                                Color.LTGRAY
                            )

                            gravity =
                                Gravity.CENTER

                            setPadding(
                                0,
                                30,
                                0,
                                30
                            )
                        }

                    layout.addView(
                        erro
                    )

                    adicionarBotaoVoltarFundo(
                        layout
                    )
                }
            }

        }.start()
    }

    private fun adicionarSessao(
        layout: LinearLayout,
        nome: String,
        horario: OffsetDateTime
    ) {

        val brasilia =
            ZoneId.of(
                "America/Sao_Paulo"
            )

        val horarioBrasilia =
            horario.atZoneSameInstant(
                brasilia
            )

        val formato =
            DateTimeFormatter.ofPattern(
                "dd/MM/yyyy - HH:mm"
            )

        val card =
            TextView(this).apply {

                text =
                    "$nome\n\n" +
                    "📅 " +
                    horarioBrasilia.format(
                        formato
                    )

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

        if (
            horario.toInstant()
                .isAfter(
                    java.time.Instant.now()
                ) &&
            (
                proximoHorario == null ||
                horario.isBefore(
                    proximoHorario
                )
            )
        ) {

            proximoHorario =
                horario

            contadorView =
                TextView(this).apply {

                    textSize = 16f

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
                        16,
                        12,
                        16,
                        16
                    )

                    background =
                        getDrawable(
                            R.drawable.rounded_card
                        )
                }

            val parametrosContador =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            parametrosContador.setMargins(
                0,
                0,
                0,
                6
            )

            layout.addView(
                contadorView,
                parametrosContador
            )
        }
    }

    private fun adicionarBotaoVoltarFundo(
        layout: LinearLayout
    ) {

        val voltarFundo =
            criarBotaoVoltar()

        val parametros =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametros.gravity =
            Gravity.LEFT

        parametros.setMargins(
            0,
            24,
            0,
            20
        )

        layout.addView(
            voltarFundo,
            parametros
        )
    }

    private fun criarBotaoVoltar():
            TextView {

        return TextView(this).apply {

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

            gravity =
                Gravity.LEFT

            setPadding(
                0,
                8,
                0,
                8
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

    private fun iniciarContador() {

        if (
            proximoHorario != null
        ) {

            handler.post(
                atualizarContador
            )
        }
    }

    private fun atualizarContadorTela() {

        val view =
            contadorView ?: return

        val horario =
            proximoHorario ?: return

        val agora =
            OffsetDateTime.now(
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
                "🏁  A próxima sessão está começando!"

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

                "⏳  PRÓXIMA SESSÃO EM  " +
                "${dias}d ${horas}h ${minutos}min"

            } else if (horas > 0) {

                "⏳  PRÓXIMA SESSÃO EM  " +
                "${horas}h ${minutos}min"

            } else {

                "⏳  PRÓXIMA SESSÃO EM  " +
                "${minutos}min"
            }
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

        return JSONObject(
            resposta
        )
            .getJSONObject(
                "MRData"
            )
            .getJSONObject(
                "RaceTable"
            )
            .getJSONArray(
                "Races"
            )
    }

    private fun obterBandeira(
        pais: String
    ): String {

        return when (
            pais.lowercase().trim()
        ) {

            "austrália",
            "australia" -> "🇦🇺"

            "mônaco",
            "monaco" -> "🇲🇨"

            "reino unido",
            "united kingdom",
            "uk" -> "🇬🇧"

            "hungria",
            "hungary" -> "🇭🇺"

            "bélgica",
            "belgica",
            "belgium" -> "🇧🇪"

            "itália",
            "italia",
            "italy" -> "🇮🇹"

            "áustria",
            "austria" -> "🇦🇹"

            "países baixos",
            "paises baixos",
            "netherlands" -> "🇳🇱"

            "azerbaijão",
            "azerbaijan" -> "🇦🇿"

            "catar",
            "qatar" -> "🇶🇦"

            "emirados árabes unidos",
            "united arab emirates",
            "uae" -> "🇦🇪"

            "bahrein",
            "bahrain" -> "🇧🇭"

            "arábia saudita",
            "arabia saudita",
            "saudi arabia" -> "🇸🇦"

            "japão",
            "japao",
            "japan" -> "🇯🇵"

            "espanha",
            "spain" -> "🇪🇸"

            "canadá",
            "canada" -> "🇨🇦"

            "estados unidos",
            "united states",
            "usa" -> "🇺🇸"

            "singapura",
            "singapore" -> "🇸🇬"

            "méxico",
            "mexico" -> "🇲🇽"

            "brasil",
            "brazil" -> "🇧🇷"

            else -> "🏳️"
        }
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            atualizarContador
        )

        super.onDestroy()
    }
}

Cole o arquivo inteiro, salve e rode o build. Se aparecer Verde, aí fazemos exatamente a mesma correção no "F2Activity.kt".