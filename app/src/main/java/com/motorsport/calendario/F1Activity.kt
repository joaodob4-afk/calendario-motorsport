package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
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

        val conteudo =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    18,
                    18,
                    18,
                    28
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
                48,
                42
            )

        parametrosVoltarTopo.setMargins(
            0,
            0,
            0,
            14
        )

        conteudo.addView(
            voltarTopo,
            parametrosVoltarTopo
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

                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                overScrollMode =
                    View.OVER_SCROLL_NEVER

                addView(
                    conteudo
                )
            }

        setContentView(
            scrollView
        )

        carregarEvento(
            conteudo,
            carregando,
            circuitIdSelecionado
        )
    }

    private fun criarBotaoVoltar(): ImageView {

        return ImageView(this).apply {

            setImageResource(
                R.drawable.back_button
            )

            scaleType =
                ImageView.ScaleType.CENTER

            setPadding(
                0,
                0,
                0,
                0
            )

            background = null

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
        carregando: TextView,
        circuitId: String?
    ) {

        Thread {

            try {

                val evento: JSONObject?

                if (circuitId != null) {

                    evento =
                        buscarCorridaPorCircuito(
                            circuitId
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
                "EEE • dd MMM • HH:mm",
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
                    4,
                    0,
                    18
                )
            }

        val nomeEvento =
            TextView(this).apply {

                text =
                    nome

                textSize = 28f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                letterSpacing =
                    0.01f

                setPadding(
                    0,
                    0,
                    0,
                    8
                )
            }

        cabecalho.addView(
            nomeEvento
        )

        val circuitoEvento =
            TextView(this).apply {

                text =
                    circuito

                textSize = 16f

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
                46,
                3
            )

        parametrosDivisor.setMargins(
            0,
            16,
            0,
            0
        )

        cabecalho.addView(
            divisor,
            parametrosDivisor
        )

        layout.addView(
            cabecalho
        )

        adicionarDataEtapa(
            layout,
            race,
            brasilia
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

                letterSpacing =
                    0.14f

                setPadding(
                    0,
                    24,
                    0,
                    12
                )
            }

        layout.addView(
            tituloProgramacao
        )

        adicionarGrupoDia(
            layout,
            race,
            "SEXTA",
            listOf(
                Pair(
                    "FirstPractice",
                    "TREINO LIVRE 1"
                ),
                Pair(
                    "SecondPractice",
                    "TREINO LIVRE 2"
                )
            ),
            brasilia,
            formato
        )

        adicionarGrupoDia(
            layout,
            race,
            "SÁBADO",
            listOf(
                Pair(
                    "ThirdPractice",
                    "TREINO LIVRE 3"
                ),
                Pair(
                    "SprintQualifying",
                    "CLASSIFICAÇÃO SPRINT"
                ),
                Pair(
                    "Qualifying",
                    "CLASSIFICAÇÃO"
                )
            ),
            brasilia,
            formato
        )

        adicionarGrupoDia(
            layout,
            race,
            "DOMINGO",
            listOf(
                Pair(
                    "Sprint",
                    "SPRINT"
                ),
                Pair(
                    "date",
                    "CORRIDA"
                )
            ),
            brasilia,
            formato
        )
    }

    private fun adicionarDataEtapa(
        layout: LinearLayout,
        race: JSONObject,
        brasilia: ZoneId
    ) {

        try {

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

            val dataBrasilia =
                horario.atZoneSameInstant(
                    brasilia
                )

            val dia =
                dataBrasilia.dayOfMonth

            val mes =
                dataBrasilia.month
                    .getDisplayName(
                        java.time.format.TextStyle.SHORT,
                        Locale("pt", "BR")
                    )
                    .uppercase(
                        Locale("pt", "BR")
                    )

            val faixa =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        0,
                        14,
                        0,
                        14
                    )
                }

            val numero =
                TextView(this).apply {

                    text =
                        String.format(
                            Locale("pt", "BR"),
                            "%02d",
                            dia
                        )

                    textSize =
                        30f

                    setTextColor(
                        Color.WHITE
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )
                }

            faixa.addView(
                numero
            )

            val mesTexto =
                TextView(this).apply {

                    text =
                        mes

                    textSize =
                        12f

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

                    setPadding(
                        8,
                        0,
                        0,
                        0
                    )
                }

            faixa.addView(
                mesTexto
            )

            val linha =
                View(this).apply {

                    setBackgroundColor(
                        Color.rgb(
                            41,
                            74,
                            99
                        )
                    )
                }

            val parametrosLinha =
                LinearLayout.LayoutParams(
                    0,
                    1,
                    1f
                )

            parametrosLinha.setMargins(
                16,
                0,
                0,
                0
            )

            faixa.addView(
                linha,
                parametrosLinha
            )

            layout.addView(
                faixa
            )

        } catch (_: Exception) {
        }
    }

    private fun adicionarGrupoDia(
        layout: LinearLayout,
        race: JSONObject,
        titulo: String,
        sessoes: List<Pair<String, String>>,
        brasilia: ZoneId,
        formato: DateTimeFormatter
    ) {

        val sessoesExistentes =
            sessoes.filter {

                race.has(
                    it.first
                )
            }

        if (
            sessoesExistentes.isEmpty()
        ) {
            return
        }

        val tituloDia =
            TextView(this).apply {

                text =
                    titulo

                textSize =
                    11f

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

                letterSpacing =
                    0.14f

                setPadding(
                    0,
                    18,
                    0,
                    4
                )
            }

        layout.addView(
            tituloDia
        )

        for (
            sessao in sessoesExistentes
        ) {

            adicionarSessaoModerna(
                layout,
                race,
                sessao.first,
                sessao.second,
                brasilia,
                formato
            )
        }
    }

    private fun adicionarSessaoModerna(
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

            val linha =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        0,
                        13,
                        0,
                        13
                    )
                }

            val marcador =
                View(this).apply {

                    setBackgroundColor(
                        if (
                            nome == "CORRIDA"
                        ) {
                            Color.rgb(
                                25,
                                183,
                                107
                            )
                        } else {
                            Color.rgb(
                                41,
                                74,
                                99
                            )
                        }
                    )
                }

            val parametrosMarcador =
                LinearLayout.LayoutParams(
                    3,
                    24
                )

            parametrosMarcador.setMargins(
                0,
                0,
                12,
                0
            )

            linha.addView(
                marcador,
                parametrosMarcador
            )

            val nomeSessao =
                TextView(this).apply {

                    text =
                        nome

                    textSize =
                        if (
                            nome == "CORRIDA"
                        ) {
                            15f
                        } else {
                            14f
                        }

                    setTextColor(
                        Color.WHITE
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                }

            linha.addView(
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
                                Locale(
                                    "pt",
                                    "BR"
                                )
                            )

                    textSize =
                        12f

                    setTextColor(
                        if (
                            nome == "CORRIDA"
                        ) {
                            Color.WHITE
                        } else {
                            Color.rgb(
                                143,
                                166,
                                186
                            )
                        }
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    gravity =
                        Gravity.END
                }

            linha.addView(
                horarioSessao
            )

            layout.addView(
                linha
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

            layout.addView(
                divisor,
                parametrosDivisor
            )

        } catch (_: Exception) {
        }
    }

    private fun obterBandeira(
        pais: String
    ): String {

        return when (
            pais.lowercase()
        ) {

            "australia" ->
                "🇦🇺"

            "bahrain" ->
                "🇧🇭"

            "saudi arabia" ->
                "🇸🇦"

            "japan" ->
                "🇯🇵"

            "china" ->
                "🇨🇳"

            "usa",
            "united states" ->
                "🇺🇸"

            "italy" ->
                "🇮🇹"

            "monaco" ->
                "🇲🇨"

            "spain" ->
                "🇪🇸"

            "canada" ->
                "🇨🇦"

            "austria" ->
                "🇦🇹"

            "united kingdom",
            "uk" ->
                "🇬🇧"

            "belgium" ->
                "🇧🇪"

            "hungary" ->
                "🇭🇺"

            "netherlands" ->
                "🇳🇱"

            "azerbaijan" ->
                "🇦🇿"

            "singapore" ->
                "🇸🇬"

            "mexico" ->
                "🇲🇽"

            "brazil" ->
                "🇧🇷"

            "qatar" ->
                "🇶🇦"

            "uae",
            "united arab emirates" ->
                "🇦🇪"

            else ->
                "🌐"
        }
    }
}