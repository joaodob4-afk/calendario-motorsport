
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

    private val preto = Color.rgb(5, 5, 5)
    private val verde = Color.rgb(25, 183, 107)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(143, 143, 143)
    private val linhaCinza = Color.rgb(48, 48, 48)

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

        val circuitIdSelecionado =
            intent.getStringExtra("CIRCUIT_ID_F1")

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
            setBackgroundColor(preto)
        }

        val voltarTopo = criarBotaoVoltar()

        conteudo.addView(
            voltarTopo,
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

        carregarEvento(
            conteudo,
            carregando,
            circuitIdSelecionado
        )
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
        val brasilia = ZoneId.of("America/Sao_Paulo")

        val formato = DateTimeFormatter.ofPattern(
            "EEE • dd MMM • HH:mm",
            Locale("pt", "BR")
        )

        val nome = race.getString("raceName")
        val circuito = race.optJSONObject("Circuit")
            ?.optString("circuitName", "Circuito não informado")
            ?: "Circuito não informado"

        val localizacao = race.optJSONObject("Circuit")
            ?.optJSONObject("Location")

        val cidade = localizacao?.optString("local", "") ?: ""
        val pais = localizacao?.optString("country", "") ?: ""

        // Cabeçalho de estilo automobilístico.
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
            LinearLayout.LayoutParams(dp(4), dp(104)).apply {
                setMargins(0, dp(2), dp(13), 0)
            }
        )

        val colunaTitulo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val categoria = TextView(this).apply {
            text = "FORMULA 1"
            textSize = 11f
            setTextColor(verde)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.18f
        }

        colunaTitulo.addView(categoria)

        val nomeEvento = TextView(this).apply {
            text = nome
            textSize = 26f
            setTextColor(branco)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(5), 0, dp(8))
        }

        colunaTitulo.addView(nomeEvento)

        val circuitoEvento = TextView(this).apply {
            text = circuito
            textSize = 14f
            setTextColor(branco)
            setTypeface(null, Typeface.BOLD)
        }

        colunaTitulo.addView(circuitoEvento)

        val localEvento = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
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
                    setMargins(0, 0, dp(7), 0)
                }
            )
        } else {
            val bandeiraEmoji = TextView(this).apply {
                text = obterBandeira(pais)
                textSize = 14f
                setPadding(0, 0, dp(7), 0)
            }
            localEvento.addView(bandeiraEmoji)
        }

        val textoLocal = TextView(this).apply {
            text = "$cidade • $pais"
            textSize = 12f
            setTextColor(cinza)
        }

        localEvento.addView(textoLocal)
        colunaTitulo.addView(localEvento)

        cabecalho.addView(
            colunaTitulo,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        layout.addView(cabecalho)

        val divisorPrincipal = View(this).apply {
            setBackgroundColor(verde)
        }

        layout.addView(
            divisorPrincipal,
            LinearLayout.LayoutParams(dp(46), dp(3)).apply {
                setMargins(0, 0, 0, dp(14))
            }
        )

        adicionarDataEtapa(layout, race, brasilia)

        val tituloProgramacao = TextView(this).apply {
            text = "PROGRAMAÇÃO"
            textSize = 12f
            setTextColor(verde)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.16f
            setPadding(0, dp(20), 0, dp(8))
        }

        layout.addView(tituloProgramacao)

        adicionarGrupoDia(
            layout, race, "SEXTA",
            listOf(
                Pair("FirstPractice", "TREINO LIVRE 1"),
                Pair("SecondPractice", "TREINO LIVRE 2")
            ),
            brasilia, formato
        )

        adicionarGrupoDia(
            layout, race, "SÁBADO",
            listOf(
                Pair("ThirdPractice", "TREINO LIVRE 3"),
                Pair("SprintQualifying", "CLASSIFICAÇÃO SPRINT"),
                Pair("Qualifying", "CLASSIFICAÇÃO")
            ),
            brasilia, formato
        )

        adicionarGrupoDia(
            layout, race, "DOMINGO",
            listOf(
                Pair("Sprint", "SPRINT"),
                Pair("date", "CORRIDA")
            ),
            brasilia, formato
        )
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

    private fun adicionarDataEtapa(
        layout: LinearLayout,
        race: JSONObject,
        brasilia: ZoneId
    ) {
        try {
            val corrida = OffsetDateTime.parse(
                race.getString("date") +
                    "T" +
                    race.optString("time", "00:00:00Z")
            ).atZoneSameInstant(brasilia)

            val sessoes = mutableListOf<java.time.ZonedDateTime>()

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
                        sessoes.add(
                            OffsetDateTime.parse(
                                sessao.getString("date") +
                                    "T" + sessao.getString("time")
                            ).atZoneSameInstant(brasilia)
                        )
                    } catch (_: Exception) {
                    }
                }
            }

            sessoes.add(corrida)

            val primeira = sessoes.minByOrNull {
                it.toLocalDate()
            }!!.toLocalDate()

            val ultima = sessoes.maxByOrNull {
                it.toLocalDate()
            }!!.toLocalDate()

            val textoData = if (primeira == ultima) {
                String.format(
                    Locale("pt", "BR"),
                    "%02d %s",
                    primeira.dayOfMonth,
                    primeira.month.getDisplayName(
                        java.time.format.TextStyle.SHORT,
                        Locale("pt", "BR")
                    ).uppercase(Locale("pt", "BR"))
                )
            } else {
                String.format(
                    Locale("pt", "BR"),
                    "%02d–%02d %s",
                    primeira.dayOfMonth,
                    ultima.dayOfMonth,
                    ultima.month.getDisplayName(
                        java.time.format.TextStyle.SHORT,
                        Locale("pt", "BR")
                    ).uppercase(Locale("pt", "BR"))
                )
            }

            val bloco = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(8), 0, dp(14))
            }

            val data = TextView(this).apply {
                text = textoData
                textSize = 28f
                setTextColor(branco)
                setTypeface(null, Typeface.BOLD)
            }

            bloco.addView(data)

            val ano = TextView(this).apply {
                text = ultima.year.toString()
                textSize = 11f
                setTextColor(cinza)
                setTypeface(null, Typeface.BOLD)
                letterSpacing = 0.12f
            }

            bloco.addView(ano)
            layout.addView(bloco)

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
        val existentes = sessoes.filter { race.has(it.first) }
        if (existentes.isEmpty()) return

        val cabecalhoDia = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, dp(8))
        }

        val tituloDia = TextView(this).apply {
            text = titulo
            textSize = 12f
            setTextColor(branco)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.16f
        }

        cabecalhoDia.addView(tituloDia)

        val linha = View(this).apply {
            setBackgroundColor(linhaCinza)
        }

        cabecalhoDia.addView(
            linha,
            LinearLayout.LayoutParams(
                0, dp(1), 1f
            ).apply {
                setMargins(dp(12), 0, 0, 0)
            }
        )

        layout.addView(cabecalhoDia)

        for (sessao in existentes) {
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

            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    0,
                    dp(if (corrida) 17 else 12),
                    0,
                    dp(if (corrida) 17 else 12)
                )
            }

            if (corrida) {
                val marcador = View(this).apply {
                    setBackgroundColor(verde)
                }

                linha.addView(
                    marcador,
                    LinearLayout.LayoutParams(dp(4), dp(30)).apply {
                        setMargins(0, 0, dp(12), 0)
                    }
                )
            }

            val nomeSessao = TextView(this).apply {
                text = nome
                textSize = if (corrida) 16f else 13f
                setTextColor(branco)
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            linha.addView(nomeSessao)

            val horarioSessao = TextView(this).apply {
                text = horario.format(formato)
                    .uppercase(Locale("pt", "BR"))
                textSize = if (corrida) 13f else 11f
                setTextColor(if (corrida) verde else cinza)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.END
            }

            linha.addView(horarioSessao)
            layout.addView(linha)

            if (corrida) {
                val divisor = View(this).apply {
                    setBackgroundColor(verde)
                }

                layout.addView(
                    divisor,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(2)
                    )
                )
            } else {
                val divisor = View(this).apply {
                    setBackgroundColor(linhaCinza)
                }

                layout.addView(
                    divisor,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                    )
                )
            }

        } catch (_: Exception) {
        }
    }

    private fun obterBandeira(pais: String): String {
        return when (pais.lowercase(Locale.ROOT)) {
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
