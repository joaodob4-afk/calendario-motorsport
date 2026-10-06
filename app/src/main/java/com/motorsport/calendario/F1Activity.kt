package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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

    private var contadorView: TextView? = null

    private var proximoHorario:
            ZonedDateTime? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val conteudo =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    24,
                    24,
                    24
                )

                setBackgroundColor(
                    Color.BLACK
                )
            }

        val selecionada =
            intent.getStringExtra("RACE") != null

        val titulo =
            TextView(this).apply {

                text =
                    if (selecionada) {
                        "🏎️ FÓRMULA 1\n\n" +
                        "PROGRAMAÇÃO DA ETAPA"
                    } else {
                        "🏎️ FÓRMULA 1\n\n" +
                        "PRÓXIMO EVENTO"
                    }

                textSize = 26f

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
                    0,
                    0,
                    0,
                    24
                )
            }

        conteudo.addView(titulo)

        val carregando =
            TextView(this).apply {

                text =
                    "Carregando programação..."

                textSize = 18f

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

        conteudo.addView(carregando)

        val voltar =
            Button(this).apply {

                text = "VOLTAR"
                textSize = 16f

                setOnClickListener {
                    finish()
                }
            }

        val parametrosVoltar =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosVoltar.setMargins(
            0,
            24,
            0,
            24
        )

        conteudo.addView(
            voltar,
            parametrosVoltar
        )

        val scrollView =
            ScrollView(this).apply {
                addView(conteudo)
            }

        setContentView(scrollView)

        carregarEvento(
            conteudo,
            carregando
        )
    }

    private fun carregarEvento(
        layout: LinearLayout,
        carregando: TextView
    ) {

        Thread {

            try {

                val raceExtra =
                    intent.getStringExtra("RACE")

                val evento: JSONObject

                if (raceExtra != null) {

                    evento =
                        JSONObject(raceExtra)

                } else {

                    val races =
                        buscarCorridasF1()

                    val agora =
                        java.time.Instant.now()

                    var proximo:
                            JSONObject? = null

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

                            proximo =
                                race

                            break
                        }
                    }

                    if (proximo == null) {

                        runOnUiThread {

                            carregando.text =
                                "Nenhum evento futuro encontrado."
                        }

                        return@Thread
                    }

                    evento =
                        proximo
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
                        "Erro: " +
                        e.javaClass.simpleName
                }
            }

        }.start()
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

        val cabecalho =
            TextView(this).apply {

                text =
                    "🏁 $nome\n\n" +
                    "🏟️ $circuito\n" +
                    "📍 $cidade - $pais"

                textSize = 22f

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
                    10,
                    20,
                    10,
                    24
                )
            }

        layout.addView(
            cabecalho
        )

        adicionarSessao(
            layout,
            race,
            "FirstPractice",
            "🟢 TREINO LIVRE 1",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "SecondPractice",
            "🟢 TREINO LIVRE 2",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "ThirdPractice",
            "🟢 TREINO LIVRE 3",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "SprintQualifying",
            "🟡 CLASSIFICAÇÃO SPRINT",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "Sprint",
            "🟡 SPRINT",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "Qualifying",
            "🔵 CLASSIFICAÇÃO",
            brasilia,
            formato
        )

        adicionarSessao(
            layout,
            race,
            "date",
            "🔴 CORRIDA",
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
                    race.getString(
                        "date"
                    )

                hora =
                    race.optString(
                        "time",
                        "00:00:00Z"
                    )

            } else {

                val sessao =
                    race.getJSONObject(
                        campo
                    )

                data =
                    sessao.getString(
                        "date"
                    )

                hora =
                    sessao.getString(
                        "time"
                    )
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
                        "📅 ${
                            horarioBrasilia.format(
                                formato
                            )
                        }"

                    textSize = 18f

                    setTextColor(
                        Color.WHITE
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    setPadding(
                        20,
                        20,
                        20,
                        20
                    )

                    setBackgroundColor(
                        Color.DKGRAY
                    )
                }

            val parametros =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            parametros.setMargins(
                0,
                8,
                0,
                8
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

                            textSize = 16f

                            setTextColor(
                                Color.WHITE
                            )

                            setTypeface(
                                null,
                                Typeface.BOLD
                            )

                            setPadding(
                                20,
                                0,
                                20,
                                20
                            )
                        }

                    layout.addView(
                        contadorView
                    )
                }
            }

        } catch (_: Exception) {
            // Sessão inexistente não será exibida.
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
                "🏁 A próxima sessão está começando!"

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

                "⏳ Próxima sessão começa em " +
                "${dias}d " +
                "${horas}h " +
                "${minutos}min"

            } else if (horas > 0) {

                "⏳ Próxima sessão começa em " +
                "${horas}h " +
                "${minutos}min"

            } else {

                "⏳ Próxima sessão começa em " +
                "${minutos}min"
            }
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            atualizarContador
        )

        super.onDestroy()
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