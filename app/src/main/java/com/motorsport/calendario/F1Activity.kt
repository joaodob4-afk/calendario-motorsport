
package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class F1Activity : AppCompatActivity() {

    private val preto = Color.BLACK
    private val verde = Color.rgb(57, 255, 20)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(165, 165, 165)
    private val bordaCard = Color.rgb(20, 48, 15)

    private val localeBR = Locale("pt", "BR")
    private val brasilia = ZoneId.of("America/Sao_Paulo")

    private data class Sessao(
        val nome: String,
        val data: LocalDate,
        val horario: java.time.ZonedDateTime?,
        val corrida: Boolean = false
    )

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
            contentDescription = "Voltar"

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
                val evento = if (!circuitId.isNullOrBlank()) {
                    buscarCorridaPorCircuito(circuitId)
                } else {
                    buscarProximaCorrida()
                }

                if (evento == null) {
                    runOnUiThread {
                        carregando.text =
                            "Etapa não encontrada ou ainda indisponível."
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
                        (e.message ?: e.javaClass.simpleName)
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
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val codigo = connection.responseCode
            val stream = if (codigo in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val resposta = stream?.bufferedReader()?.use {
                it.readText()
            }.orEmpty()

            if (codigo !in 200..299) {
                throw IllegalStateException(
                    "A API respondeu com HTTP $codigo."
                )
            }

            return resposta
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

        val agora = Instant.now()
        val hoje = LocalDate.now(brasilia)

        for (i in 0 until races.length()) {
            val race = races.getJSONObject(i)
            val data = race.optString("date", "")

            val dataCorrida = try {
                LocalDate.parse(data)
            } catch (_: Exception) {
                continue
            }

            val hora = race.optString("time", "")

            if (hora.isBlank()) {
                if (!dataCorrida.isBefore(hoje)) {
                    return race
                }
                continue
            }

            val instante = try {
                OffsetDateTime.parse("${data}T${hora}")
                    .toInstant()
            } catch (_: Exception) {
                continue
            }

            if (instante.isAfter(agora)) {
                return race
            }
        }

        return null
    }

    private fun mostrarEvento(
        layout: LinearLayout,
        race: JSONObject
    ) {
        val nome = race.optString("raceName", "Grand Prix")
        val tituloGrandPrix = obterTituloGrandPrix(nome)

        val circuito = race.optJSONObject("Circuit")
            ?.optString("circuitName", "Circuito não informado")
            ?: "Circuito não informado"

        val localizacao = race.optJSONObject("Circuit")
            ?.optJSONObject("Location")

        val cidade = localizacao?.optString("locality", "") ?: ""
        val pais = localizacao?.optString("country", "") ?: ""

        // Cabeçalho da categoria.
        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(18))
        }

        val faixaVerde = View(this).apply {
            setBackgroundColor(verde)
        }

        cabecalho.addView(
            faixaVerde,
            LinearLayout.LayoutParams(dp(4), dp(25)).apply {
                setMargins(0, 0, dp(12), 0)
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

        // Nome principal da etapa.
        val nomeEvento = TextView(this).apply {
            text = tituloGrandPrix
            textSize = 24f
            setTextColor(branco)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }

        layout.addView(nomeEvento)

        // Nome do circuito.
        val circuitoEvento = TextView(this).apply {
            text = circuito.replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(localeBR)
                } else {
                    it.toString()
                }
            }
            textSize = 14f
            setTextColor(cinza)
        }

        layout.addView(circuitoEvento)

        // Bandeira e localização.
        val localEvento = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            translationX = -dp(5).toFloat()
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
                    setMargins(-dp(2), 0, dp(4), 0)
                }
            )
        } else {
            val bandeiraEmoji = TextView(this).apply {
                text = obterBandeira(pais)
                textSize = 18f
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, dp(4), 0)
            }

            localEvento.addView(bandeiraEmoji)
        }

        val textoLocal = TextView(this).apply {
            text = listOf(cidade, pais)
                .filter { it.isNotBlank() }
                .joinToString(", ")
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

        val sessoes = obterSessoes(race)

        adicionarDataEtapa(layout, sessoes)
        adicionarSeparador(layout)

        val tituloProgramacao = TextView(this).apply {
            text = "PROGRAMAÇÃO"
            textSize = 12f
            setTextColor(verde)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.16f
            setPadding(0, dp(14), 0, dp(10))
        }

        layout.addView(tituloProgramacao)

        if (sessoes.isEmpty()) {
            val semProgramacao = TextView(this).apply {
                text = "Horários ainda não disponíveis."
                textSize = 13f
                setTextColor(cinza)
                setPadding(0, dp(8), 0, dp(8))
            }

            layout.addView(semProgramacao)
            return
        }

        val grupos = sessoes
            .groupBy { it.data }
            .toSortedMap()

        for ((data, sessoesDoDia) in grupos) {
            val diaCurto = data.dayOfWeek
                .getDisplayName(TextStyle.SHORT, localeBR)
                .uppercase(localeBR)
                .replace(".", "")

            val dataCurta = data.format(
                DateTimeFormatter.ofPattern("dd MMM", localeBR)
            ).uppercase(localeBR).replace(".", "")

            adicionarGrupoDia(
                layout,
                "$diaCurto $dataCurta",
                sessoesDoDia.sortedWith(
                    compareBy<Sessao> { it.horario == null }
                        .thenBy { it.horario?.toInstant() }
                )
            )
        }
    }

    private fun obterTituloGrandPrix(nome: String): String {
        val chave = nome.trim().lowercase(Locale.ROOT)

        val titulos = mapOf(
            "australian grand prix" to "GRAND PRIX DA\nAUSTRÁLIA",
            "bahrain grand prix" to "GRAND PRIX DO BAHREIN",
            "saudi arabian grand prix" to "GRAND PRIX DA ARÁBIA SAUDITA",
            "japanese grand prix" to "GRAND PRIX DO JAPÃO",
            "chinese grand prix" to "GRAND PRIX DA CHINA",
            "miami grand prix" to "GRAND PRIX DE MIAMI",
            "emilia romagna grand prix" to "GRAND PRIX DA EMÍLIA-ROMANHA",
            "monaco grand prix" to "GRAND PRIX DE MÔNACO",
            "spanish grand prix" to "GRAND PRIX DA ESPANHA",
            "canadian grand prix" to "GRAND PRIX DO CANADÁ",
            "austrian grand prix" to "GRAND PRIX DA ÁUSTRIA",
            "british grand prix" to "GRAND PRIX DA GRÃ-BRETANHA",
            "belgian grand prix" to "GRAND PRIX DA BÉLGICA",
            "hungarian grand prix" to "GRAND PRIX DA HUNGRIA",
            "dutch grand prix" to "GRAND PRIX DOS PAÍSES BAIXOS",
            "italian grand prix" to "GRAND PRIX DA ITÁLIA",
            "azerbaijan grand prix" to "GRAND PRIX DO AZERBAIJÃO",
            "singapore grand prix" to "GRAND PRIX DE SINGAPURA",
            "united states grand prix" to "GRAND PRIX DOS ESTADOS UNIDOS",
            "mexico city grand prix" to "GRAND PRIX DA CIDADE DO MÉXICO",
            "mexican grand prix" to "GRAND PRIX DO MÉXICO",
            "são paulo grand prix" to "GRAND PRIX DE SÃO PAULO",
            "brazilian grand prix" to "GRAND PRIX DO BRASIL",
            "qatar grand prix" to "GRAND PRIX DO CATAR",
            "abu dhabi grand prix" to "GRAND PRIX DE ABU DHABI",
            "las vegas grand prix" to "GRAND PRIX DE LAS VEGAS",
            "portuguese grand prix" to "GRAND PRIX DE PORTUGAL"
        )

        titulos[chave]?.let { return it }

        val nomeBase = nome
            .replace(Regex("(?i)\\s*grand prix\\s*"), "")
            .trim()

        return if (nomeBase.isNotEmpty()) {
            "GRAND PRIX DE ${nomeBase.uppercase(localeBR)}"
        } else {
            "GRAND PRIX"
        }
    }

    private fun obterSessoes(race: JSONObject): List<Sessao> {
        val resultado = mutableListOf<Sessao>()

        val campos = listOf(
            "FirstPractice" to "TREINO LIVRE 1",
            "SecondPractice" to "TREINO LIVRE 2",
            "ThirdPractice" to "TREINO LIVRE 3",
            "SprintQualifying" to "CLASSIFICAÇÃO SPRINT",
            "SprintShootout" to "CLASSIFICAÇÃO SPRINT",
            "Sprint" to "SPRINT",
            "Qualifying" to "CLASSIFICAÇÃO"
        )

        val nomesAdicionados = mutableSetOf<String>()

        for ((campo, nome) in campos) {
            if (!race.has(campo) || !nomesAdicionados.add(nome)) {
                continue
            }

            val sessao = race.optJSONObject(campo) ?: continue
            val dataTexto = sessao.optString("date", "")

            val data = try {
                LocalDate.parse(dataTexto)
            } catch (_: Exception) {
                continue
            }

            val horaTexto = sessao.optString("time", "")
            val horario = converterHorario(dataTexto, horaTexto)

            resultado.add(
                Sessao(
                    nome = nome,
                    data = horario?.toLocalDate() ?: data,
                    horario = horario
                )
            )
        }

        val dataCorridaTexto = race.optString("date", "")
        val dataCorrida = try {
            LocalDate.parse(dataCorridaTexto)
        } catch (_: Exception) {
            null
        }

        if (dataCorrida != null) {
            val horarioCorrida = converterHorario(
                dataCorridaTexto,
                race.optString("time", "")
            )

            resultado.add(
                Sessao(
                    nome = "CORRIDA",
                    data = horarioCorrida?.toLocalDate() ?: dataCorrida,
                    horario = horarioCorrida,
                    corrida = true
                )
            )
        }

        return resultado.sortedWith(
            compareBy<Sessao> { it.data }
                .thenBy { it.horario?.toInstant() }
        )
    }

    private fun converterHorario(
        data: String,
        hora: String
    ): java.time.ZonedDateTime? {
        if (data.isBlank() || hora.isBlank()) return null

        return try {
            OffsetDateTime.parse("${data}T${hora}")
                .atZoneSameInstant(brasilia)
        } catch (_: Exception) {
            null
        }
    }

    // Data da etapa no estilo da imagem: 06–08 MAR.
    private fun adicionarDataEtapa(
        layout: LinearLayout,
        sessoes: List<Sessao>
    ) {
        if (sessoes.isEmpty()) return

        val primeira = sessoes.minOf { it.data }
        val ultima = sessoes.maxOf { it.data }

        val mesInicial = primeira.month
            .getDisplayName(TextStyle.SHORT, localeBR)
            .uppercase(localeBR)
            .replace(".", "")

        val mesFinal = ultima.month
            .getDisplayName(TextStyle.SHORT, localeBR)
            .uppercase(localeBR)
            .replace(".", "")

        val dataFormatada = when {
            primeira.month == ultima.month &&
                primeira.year == ultima.year -> {
                String.format(
                    localeBR,
                    "%02d–%02d %s",
                    primeira.dayOfMonth,
                    ultima.dayOfMonth,
                    mesFinal
                )
            }

            primeira.year == ultima.year -> {
                String.format(
                    localeBR,
                    "%02d %s – %02d %s",
                    primeira.dayOfMonth,
                    mesInicial,
                    ultima.dayOfMonth,
                    mesFinal
                )
            }

            else -> String.format(
                localeBR,
                "%02d %s %d – %02d %s %d",
                primeira.dayOfMonth,
                mesInicial,
                primeira.year,
                ultima.dayOfMonth,
                mesFinal,
                ultima.year
            )
        }

        val data = TextView(this).apply {
            text = dataFormatada
            textSize = 20f
            setTextColor(branco)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(2), 0, dp(18))
        }

        layout.addView(data)
    }

    private fun adicionarSeparador(layout: LinearLayout) {
        val separador = View(this).apply {
            setBackgroundColor(Color.rgb(65, 65, 65))
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

    // Título do dia em verde e sessões em cards horizontais.
    private fun adicionarGrupoDia(
        layout: LinearLayout,
        titulo: String,
        sessoes: List<Sessao>
    ) {
        val grupo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(14), 0, dp(4))
        }

        val tituloDia = TextView(this).apply {
            text = titulo
            textSize = 16f
            setTextColor(verde)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.04f
            setPadding(0, 0, 0, dp(9))
        }

        grupo.addView(tituloDia)

        for (sessao in sessoes) {
            adicionarSessao(grupo, sessao)
        }

        layout.addView(grupo)
    }

    // Card de cada sessão, seguindo a referência enviada.
    private fun adicionarSessao(
        layout: LinearLayout,
        sessao: Sessao
    ) {
        val corBorda = if (sessao.corrida) {
            Color.rgb(57, 180, 30)
        } else {
            bordaCard
        }

        val fundoCard = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(preto)
            setStroke(dp(1), corBorda)
            cornerRadius = dp(11).toFloat()
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background = fundoCard
        }

        val nomeSessao = TextView(this).apply {
            text = sessao.nome
            textSize = 14f
            setTextColor(if (sessao.corrida) branco else Color.LTGRAY)
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
        }

        card.addView(
            nomeSessao,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(0, 0, dp(8), 0)
            }
        )

        val horarioSessao = TextView(this).apply {
            text = sessao.horario?.format(
                DateTimeFormatter.ofPattern("HH:mm", localeBR)
            ) ?: "A definir"

            textSize = 14f
            setTextColor(verde)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        card.addView(
            horarioSessao,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        layout.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }
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
            "united kingdom", "uk",
            "great britain" -> R.drawable.flag_gb
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
            "united kingdom", "uk",
            "great britain" -> "🇬🇧"
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
