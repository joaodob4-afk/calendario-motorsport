
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
import java.time.format.TextStyle
import java.util.Locale

class F1Activity : AppCompatActivity() {

    private val preto = Color.rgb(13, 13, 13)
    private val verde = Color.rgb(25, 183, 107)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(143, 143, 143)
    private val linhaCinza = Color.rgb(48, 48, 48)

    private val localeBR = Locale("pt", "BR")
    private val brasilia = ZoneId.of("America/Sao_Paulo")

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

        val circuitId = intent.getStringExtra("CIRCUIT_ID_F1")

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
            setBackgroundColor(preto)
        }

        conteudo.addView(
            criarBotaoVoltar(),
            LinearLayout.LayoutParams(dp(48), dp(42)).apply {
                setMargins(0, 0, 0, dp(18))
            }
        )

        val carregando = TextView(this).apply {
            text = "Carregando programação..."
            textSize = 16f
            setTextColor(cinza)
            gravity = Gravity.CENTER
            setPadding(0, dp(30), 0, dp(30))
        }

        conteudo.addView(carregando)

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(preto)
            addView(conteudo)
        }

        setContentView(scrollView)

        carregarEvento(conteudo, carregando, circuitId)
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()

    private fun criarBotaoVoltar(): ImageView {
        return ImageView(this).apply {
            setImageResource(R.drawable.back_button)
            scaleType = ImageView.ScaleType.CENTER
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
                val evento = if (circuitId != null) {
                    buscarCorridaPorCircuito(circuitId)
                } else {
                    buscarProximaCorrida()
                }

                if (evento == null) {
                    runOnUiThread {
                        carregando.text = "Etapa não encontrada."
                    }
                    return@Thread
                }

                runOnUiThread {
                    layout.removeView(carregando)
                    mostrarEvento(layout, evento)
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

    private fun requisitarJson(endereco: String): String {
        val connection =
            URL(endereco).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty(
                "User-Agent",
                "CalendarioMotorsport/1.0"
            )

            return connection.inputStream.bufferedReader().use {
                it.readText()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun buscarCorridaPorCircuito(
        circuitId: String
    ): JSONObject? {
        val resposta = requisitarJson(
            "https://api.jolpi.ca/ergast/f1/2026/circuits/" +
                circuitId + "/races/"
        )

        val races = JSONObject(resposta)
            .getJSONObject("MRData")
            .getJSONObject("RaceTable")
            .getJSONArray("Races")

        if (races.length() == 0) return null

        return races.getJSONObject(races.length() - 1)
    }

    private fun buscarProximaCorrida(): JSONObject? {
        val resposta = requisitarJson(
            "https://api.jolpi.ca/ergast/f1/current/races/"
        )

        val races = JSONObject(resposta)
            .getJSONObject("MRData")
            .getJSONObject("RaceTable")
            .getJSONArray("Races")

        val agora = java.time.Instant.now()

        for (i in 0 until races.length()) {
            val race = races.getJSONObject(i)
            val data = race.getString("date")
            val hora = race.optString("time", "00:00:00Z")
            val horario = OffsetDateTime.parse("${data}T${hora}")

            if (horario.toInstant().isAfter(agora)) {
                return race
            }
        }

        return null
    }

    private fun mostrarEvento(
        layout: LinearLayout,
        race: JSONObject
    ) {
        val nome = race.getString("raceName")
        val circuito = race.optJSONObject("Circuit")
            ?.optString("circuitName", "Circuito não informado")
            ?: "Circuito não informado"

        val localizacao = race.optJSONObject("Circuit")
            ?.optJSONObject("Location")

        val cidade = localizacao?.optString("local", "") ?: ""
        val pais = localizacao?.optString("country", "") ?: ""

        // Cabeçalho: linha verde e categoria.
        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, dp(4), 0, dp(18))
        }

        val faixaVerde = View(this).apply {
            setBackgroundColor(verde)
        }

        cabecalho.addView(
            faixaVerde,
            LinearLayout.LayoutParams(dp(4), dp(26)).apply {
                setMargins(0, dp(1), dp(12), 0)
            }
        )

        val categoria = TextView(this).apply {
            text = "FORMULA 1"
            textSize = 12f
            setTextColor(verde)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.16f
        }

        cabecalho.addView(categoria)
        layout.addView(cabecalho)

        // Título do Grand Prix.
        val nomeGrandPrix = nome
            .replace("Grand Prix", "", ignoreCase = true)
            .replace("Grand Prix", "", ignoreCase = true)
            .replace(Regex("\\s+"), " ")
            .trim()
            .uppercase(localeBR)

        val nomeEvento = TextView(this).apply {
            text = "GRAND PRIX DO $nomeGrandPrix"
            textSize = 24f
            setTextColor(branco)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }

        layout.addView(nomeEvento)

        // Nome do autódromo.
        val circuitoEvento = TextView(this).apply {
            text = circuito.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(localeBR) else it.toString()
            }
            textSize = 14f
            setTextColor(cinza)
        }

        layout.addView(circuitoEvento)

        // Bandeira e localização, alinhadas verticalmente.
        val localEvento = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(18))
        }

        val recursoBandeira = obterRecursoBandeira(pais)

        if (recursoBandeira != 0) {
            val imagemBandeira = ImageView(this).apply {
                setImageResource(recursoBandeira)
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = "Bandeira de $pais"
            }

            localEvento.addView(
                imagemBandeira,
                LinearLayout.LayoutParams(dp(44), dp(29)).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setMargins(0, 0, dp(8), 0)
                }
            )
        } else {
            val bandeiraEmoji = TextView(this).apply {
                text = obterBandeira(pais)
                textSize = 18f
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, dp(8), 0)
            }

            localEvento.addView(bandeiraEmoji)
        }

        val textoLocal = TextView(this).apply {
            text = "$cidade • $pais"
            textSize = 12f
            setTextColor(cinza)
            gravity = Gravity.CENTER_VERTICAL
        }

        localEvento.addView(
            textoLocal,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        layout.addView(localEvento)

        // Data da etapa.
        adicionarDataEtapa(layout, race)

        // Separador com margens laterais.
        adicionarSeparador(layout)

        val tituloProgramacao = TextView(this).apply {
            text = "PROGRAMAÇÃO"
            textSize = 12f
            setTextColor(verde)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.16f
            setPadding(0, dp(18), 0, dp(10))
        }

        layout.addView(tituloProgramacao)

        adicionarGrupoDia(
            layout,
            race,
            "SEXTA",
            listOf(
                "FirstPractice" to "TREINO LIVRE 1",
                "SecondPractice" to "TREINO LIVRE 2"
            )
        )

        adicionarGrupoDia(
            layout,
            race,
            "SÁBADO",
            listOf(
                "ThirdPractice" to "TREINO LIVRE 3",
                "SprintQualifying" to "CLASSIFICAÇÃO SPRINT",
                "Qualifying" to "CLASSIFICAÇÃO"
            )
        )

        adicionarGrupoDia(
            layout,
            race,
            "DOMINGO",
            listOf(
                "Sprint" to "SPRINT",
                "date" to "CORRIDA"
            )
        )
    }

    private fun adicionarDataEtapa(
        layout: LinearLayout,
        race: JSONObject
    ) {
        try {
            val datas = mutableListOf<java.time.ZonedDateTime>()

            val campos = listOf(
                "FirstPractice",
                "SecondPractice",
                "ThirdPractice",
                "SprintQualifying",
                "Sprint",
                "Qualifying"
            )

            for (campo in campos) {
                if (race.has(campo)) {
                    try {
                        val sessao = race.getJSONObject(campo)
                        datas.add(
                            OffsetDateTime.parse(
                                sessao.getString("date") +
                                    "T" + sessao.getString("time")
                            ).atZoneSameInstant(brasilia)
                        )
                    } catch (_: Exception) {
                    }
                }
            }

            val dataCorrida = OffsetDateTime.parse(
                race.getString("date") +
                    "T" + race.optString("time", "00:00:00Z")
            ).atZoneSameInstant(brasilia)

            datas.add(dataCorrida)

            val primeira = datas.minByOrNull { it.toLocalDate() }!!
                .toLocalDate()
            val ultima = datas.maxByOrNull { it.toLocalDate() }!!
                .toLocalDate()

            val mes = ultima.month.getDisplayName(
                TextStyle.SHORT,
                localeBR
            ).uppercase(localeBR).replace(".", "")

            val dataFormatada = if (primeira.month == ultima.month) {
                String.format(
                    localeBR,
                    "%02d-%02d %s %d",
                    primeira.dayOfMonth,
                    ultima.dayOfMonth,
                    mes,
                    ultima.year
                )
            } else {
                val mesInicial = primeira.month.getDisplayName(
                    TextStyle.SHORT,
                    localeBR
                ).uppercase(localeBR).replace(".", "")

                String.format(
                    localeBR,
                    "%02d %s - %02d %s %d",
                    primeira.dayOfMonth,
                    mesInicial,
                    ultima.dayOfMonth,
                    mes,
                    ultima.year
                )
            }

            val data = TextView(this).apply {
                text = dataFormatada
                textSize = 23f
                setTextColor(branco)
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(2), 0, dp(18))
            }

            layout.addView(data)
        } catch (_: Exception) {
        }
    }

    private fun adicionarSeparador(layout: LinearLayout) {
        val separador = View(this).apply {
            setBackgroundColor(linhaCinza)
        }

        layout.addView(
            separador,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                setMargins(dp(2), 0, dp(2), 0)
            }
        )
    }

    private fun adicionarGrupoDia(
        layout: LinearLayout,
        race: JSONObject,
        titulo: String,
        sessoes: List<Pair<String, String>>
    ) {
        val existentes = sessoes.filter { race.has(it.first) }
        if (existentes.isEmpty()) return

        val grupo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, dp(12), 0, dp(12))
        }

        val tituloDia = TextView(this).apply {
            text = titulo
            textSize = 11f
            setTextColor(branco)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.04f
        }

        grupo.addView(
            tituloDia,
            LinearLayout.LayoutParams(dp(66), LinearLayout.LayoutParams.WRAP_CONTENT)
        )

        val divisorVertical = View(this).apply {
            setBackgroundColor(linhaCinza)
        }

        grupo.addView(
            divisorVertical,
            LinearLayout.LayoutParams(dp(1), dp(1)).apply {
                height = dp(1)
                setMargins(0, dp(2), dp(10), 0)
            }
        )

        val colunaSessoes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        for ((indice, sessao) in existentes.withIndex()) {
            adicionarSessao(
                colunaSessoes,
                race,
                sessao.first,
                sessao.second
            )

            if (indice < existentes.lastIndex) {
                val divisor = View(this).apply {
                    setBackgroundColor(linhaCinza)
                }

                colunaSessoes.addView(
                    divisor,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                    ).apply {
                        setMargins(0, dp(8), 0, dp(8))
                    }
                )
            }
        }

        grupo.addView(
            colunaSessoes,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        layout.addView(grupo)
        adicionarSeparador(layout)
    }

    private fun adicionarSessao(
        layout: LinearLayout,
        race: JSONObject,
        campo: String,
        nome: String
    ) {
        try {
            val data: String
            val hora: String

            if (campo == "date") {
                data = race.getString("date")
                hora = race.optString("time", "00:00:00Z")
            } else {
                val sessao = race.getJSONObject(campo)
                data = sessao.getString("date")
                hora = sessao.getString("time")
            }

            val horario = OffsetDateTime.parse(
                "${data}T${hora}"
            ).atZoneSameInstant(brasilia)

            val corrida = nome == "CORRIDA"

            val bloco = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(1), 0, dp(1))
            }

            val nomeSessao = TextView(this).apply {
                text = nome
                textSize = 12f
                setTextColor(if (corrida) verde else branco)
                setTypeface(null, Typeface.BOLD)
            }

            bloco.addView(nomeSessao)

            val horarioSessao = TextView(this).apply {
                text = horario.format(
                    DateTimeFormatter.ofPattern("HH:mm", localeBR)
                ) + " - " + horario.plusMinutes(
                    if (corrida) 120 else 90
                ).format(DateTimeFormatter.ofPattern("HH:mm", localeBR))

                textSize = 11f
                setTextColor(if (corrida) verde else cinza)
                setPadding(0, dp(3), 0, 0)
            }

            bloco.addView(horarioSessao)
            layout.addView(bloco)
        } catch (_: Exception) {
        }
    }

    private fun obterRecursoBandeira(pais: String): Int {
        return when (pais.trim().lowercase(Locale.ROOT)) {
            "australia" -> R.drawable.flag_au
            "bahrain" -> R.drawable.flag_bh
            "saudi arabia" -> R.drawable.flag_sa
            "japan" -> R.drawable.flag_jp
            "china" -> R.drawable.flag_cn
            "usa", "united states",
            "united states of america" -> R.drawable.flag_us
            "italy" -> R.drawable.flag_it
            "monaco" -> R.drawable.flag_mc
            "spain" -> R.drawable.flag_es
            "canada" -> R.drawable.flag_ca
            "austria" -> R.drawable.flag_at
            "united kingdom", "uk", "great britain" -> R.drawable.flag_gb
            "belgium" -> R.drawable.flag_be
            "hungary" -> R.drawable.flag_hu
            "netherlands", "the netherlands" -> R.drawable.flag_nl
            "azerbaijan" -> R.drawable.flag_az
            "singapore" -> R.drawable.flag_sg
            "mexico" -> R.drawable.flag_mx
            "brazil" -> R.drawable.flag_br
            "qatar" -> R.drawable.flag_qa
            "uae", "united arab emirates" -> R.drawable.flag_ae
            "malaysia" -> R.drawable.flag_my
            else -> 0
        }
    }

    private fun obterBandeira(pais: String): String {
        return when (pais.trim().lowercase(Locale.ROOT)) {
            "australia" -> "🇦🇺"
            "bahrain" -> "🇧🇭"
            "saudi arabia" -> "🇸🇦"
            "japan" -> "🇯🇵"
            "china" -> "🇨🇳"
            "usa", "united states",
            "united states of america" -> "🇺🇸"
            "italy" -> "🇮🇹"
            "monaco" -> "🇲🇨"
            "spain" -> "🇪🇸"
            "canada" -> "🇨🇦"
            "austria" -> "🇦🇹"
            "united kingdom", "uk", "great britain" -> "🇬🇧"
            "belgium" -> "🇧🇪"
            "hungary" -> "🇭🇺"
            "netherlands", "the netherlands" -> "🇳🇱"
            "azerbaijan" -> "🇦🇿"
            "singapore" -> "🇸🇬"
            "mexico" -> "🇲🇽"
            "brazil" -> "🇧🇷"
            "qatar" -> "🇶🇦"
            "uae", "united arab emirates" -> "🇦🇪"
            "malaysia" -> "🇲🇾"
            else -> "🌐"
        }
    }
}
