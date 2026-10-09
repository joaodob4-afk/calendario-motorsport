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
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class F1Activity : AppCompatActivity() {

    private val preto = Color.BLACK
    private val verde = Color.rgb(57, 255, 20)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(175, 175, 175)
    private val linhaCinza = Color.rgb(55, 55, 55)
    private val fundoCard = Color.rgb(15, 20, 15)
    private val verdeEscuro = Color.rgb(20, 75, 20)

    private val brasilia = ZoneId.of("America/Sao_Paulo")
    private val localePt = Locale("pt", "BR")

    private lateinit var conteudo: LinearLayout
    private lateinit var scrollView: ScrollView

    data class Sessao(
        val nome: String,
        val data: LocalDate,
        val horario: ZonedDateTime?,
        val corrida: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        montarTela()

        adicionarTexto(
            "CARREGANDO PROGRAMAÇÃO...",
            14f,
            verde,
            true
        )

        Thread {
            try {
                val corrida = buscarCorridaSelecionada()

                runOnUiThread {
                    if (corrida != null) {
                        mostrarEvento(corrida)
                    } else {
                        mostrarErro(
                            "Não foi encontrada uma próxima etapa."
                        )
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    mostrarErro(
                        "Não foi possível carregar a programação. Verifique sua conexão."
                    )
                }
            }
        }.start()
    }

    private fun montarTela() {
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(preto)
            setPadding(dp(18), dp(12), dp(18), dp(20))
        }

        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val voltar = ImageView(this).apply {
            val recurso = resources.getIdentifier(
                "back_button",
                "drawable",
                packageName
            )

            if (recurso != 0) {
                setImageResource(recurso)
            } else {
                setImageResource(
                    android.R.drawable.ic_media_previous
                )
                setColorFilter(verde)
            }

            contentDescription = "Voltar"
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(4), dp(4), dp(12), dp(4))

            setOnClickListener {
                finish()
                overridePendingTransition(
                    android.R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
            }
        }

        cabecalho.addView(
            voltar,
            LinearLayout.LayoutParams(dp(42), dp(42))
        )

        val tituloTela = TextView(this).apply {
            text = "FÓRMULA 1"
            textSize = 23f
            setTextColor(branco)
            typeface = Typeface.create(
                "sans-serif-black",
                Typeface.NORMAL
            )
            letterSpacing = 0.06f
        }

        cabecalho.addView(
            tituloTela,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        raiz.addView(cabecalho)

        val detalhe = View(this).apply {
            setBackgroundColor(verde)
        }

        raiz.addView(
            detalhe,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(2)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(14)
            }
        )

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false

            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            isVerticalFadingEdgeEnabled = false
        }

        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, dp(20))
        }

        scrollView.addView(conteudo)

        raiz.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(raiz)
    }

    // false quando o usuário abriu uma corrida específica (não a próxima).
    private var corridaEhProxima = true

    /**
     * Usa a corrida escolhida pelo usuário (extra "RACE" ou
     * "CIRCUIT_ID_F1"). Sem escolha, ou se não achar, usa a próxima.
     */
    private fun buscarCorridaSelecionada(): JSONObject? {
        val corridaTexto = intent.getStringExtra("RACE")
        val circuitId = intent.getStringExtra("CIRCUIT_ID_F1")

        var selecionada: JSONObject? = null

        if (!corridaTexto.isNullOrBlank()) {
            selecionada = try {
                JSONObject(corridaTexto)
            } catch (_: Exception) {
                null
            }
        }

        val proxima = try {
            buscarProximaCorrida()
        } catch (e: Exception) {
            if (selecionada != null) null else throw e
        }

        if (selecionada == null && !circuitId.isNullOrBlank()) {
            selecionada = buscarCorridaPorCircuito(circuitId)
        }

        if (selecionada == null) {
            corridaEhProxima = true
            return proxima
        }

        corridaEhProxima =
            proxima != null &&
            selecionada.optString("round") == proxima.optString("round")

        return selecionada
    }

    private fun buscarCorridaPorCircuito(circuitId: String): JSONObject? {
        val resposta = requisitarJson(
            "https://api.jolpi.ca/ergast/f1/current/races/?limit=100"
        )

        val races = JSONObject(resposta)
            .getJSONObject("MRData")
            .getJSONObject("RaceTable")
            .getJSONArray("Races")

        for (i in 0 until races.length()) {
            val race = races.getJSONObject(i)
            val id = race.optJSONObject("Circuit")
                ?.optString("circuitId", "") ?: ""

            if (id.equals(circuitId, ignoreCase = true)) {
                return race
            }
        }

        return null
    }

    private fun buscarProximaCorrida(): JSONObject? {
        val resposta = requisitarJson(
            "https://api.jolpi.ca/ergast/f1/current/races/?limit=100"
        )

        val races = JSONObject(resposta)
            .getJSONObject("MRData")
            .getJSONObject("RaceTable")
            .getJSONArray("Races")

        val agora = Instant.now()
        val hoje = LocalDate.now(brasilia)

        var corridaSemHorarioHoje: JSONObject? = null

        for (i in 0 until races.length()) {
            val race = races.getJSONObject(i)

            val dataTexto = race.optString("date", "")

            val dataCorrida = try {
                LocalDate.parse(dataTexto)
            } catch (_: Exception) {
                continue
            }

            if (dataCorrida.isBefore(hoje)) continue

            val hora = race.optString("time", "")

            if (hora.isBlank()) {
                if (dataCorrida.isAfter(hoje)) {
                    return race
                }

                if (
                    dataCorrida == hoje &&
                    corridaSemHorarioHoje == null
                ) {
                    corridaSemHorarioHoje = race
                }

                continue
            }

            val instante = try {
                OffsetDateTime.parse("${dataTexto}T${hora}")
                    .toInstant()
            } catch (_: Exception) {
                continue
            }

            if (instante.isAfter(agora)) {
                return race
            }
        }

        return corridaSemHorarioHoje
    }

    private fun requisitarJson(endereco: String): String {
        val conexao = URL(endereco).openConnection()
            as HttpURLConnection

        try {
            conexao.requestMethod = "GET"
            conexao.connectTimeout = 15000
            conexao.readTimeout = 15000

            conexao.setRequestProperty(
                "User-Agent",
                "CalendarioMotorsport/1.1"
            )

            conexao.setRequestProperty(
                "Accept",
                "application/json"
            )

            val codigo = conexao.responseCode

            val fluxo = if (codigo in 200..299) {
                conexao.inputStream
            } else {
                conexao.errorStream
            }

            val resposta = fluxo?.bufferedReader()?.use {
                it.readText()
            } ?: ""

            if (codigo !in 200..299) {
                throw Exception("Erro HTTP $codigo")
            }

            return resposta
        } finally {
            conexao.disconnect()
        }
    }

    private fun mostrarEvento(race: JSONObject) {
        conteudo.removeAllViews()

        val nomeGP = obterTituloGrandPrix(
            race.optString("raceName", "Grande Prêmio")
        )

        val circuito = race.optJSONObject("Circuit")
        val nomeCircuito =
            circuito?.optString("circuitName", "") ?: ""

        val localizacao = circuito?.optJSONObject("Location")
        val cidade = localizacao?.optString("locality", "") ?: ""
        val pais = localizacao?.optString("country", "") ?: ""

        val dataCorrida = try {
            LocalDate.parse(race.optString("date", ""))
        } catch (_: Exception) {
            LocalDate.now(brasilia)
        }

        adicionarTexto(
            if (corridaEhProxima) "PRÓXIMA ETAPA" else "ETAPA",
            12f,
            verde,
            true
        ).letterSpacing = 0.16f

        adicionarTexto(
            nomeGP.uppercase(localePt),
            28f,
            branco,
            true
        ).setPadding(0, dp(5), 0, dp(5))

        if (nomeCircuito.isNotBlank()) {
            adicionarTexto(
                nomeCircuito,
                12f,
                cinza,
                false
            ).setPadding(0, 0, 0, dp(12))
        }

        val localLinha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val bandeira = ImageView(this).apply {
            val id = obterRecursoBandeira(pais)

            if (id != 0) {
                setImageResource(id)
                scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                visibility = View.GONE
            }
        }

        localLinha.addView(
            bandeira,
            LinearLayout.LayoutParams(dp(34), dp(23)).apply {
                rightMargin = dp(9)
            }
        )

        val localTexto = TextView(this).apply {
            text = listOf(cidade, pais)
                .filter { it.isNotBlank() }
                .joinToString(", ")

            textSize = 13f
            setTextColor(cinza)
        }

        localLinha.addView(
            localTexto,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        conteudo.addView(
            localLinha,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        )

        adicionarFimDeSemana(race, dataCorrida)
        adicionarSeparador()

        adicionarTexto(
            "PROGRAMAÇÃO",
            15f,
            verde,
            true
        ).apply {
            letterSpacing = 0.1f
            setPadding(0, dp(3), 0, dp(12))
        }

        val sessoes = obterSessoes(race)

        if (sessoes.isEmpty()) {
            adicionarTexto(
                "Os horários desta etapa ainda não estão disponíveis.",
                14f,
                cinza,
                false
            )
            return
        }

        val grupos = sessoes.groupBy { it.data }.toSortedMap()

        for ((data, sessoesDoDia) in grupos) {
            adicionarGrupoDia(data)

            for (sessao in sessoesDoDia.sortedBy {
                it.horario?.toInstant() ?: Instant.MAX
            }) {
                adicionarSessao(sessao)
            }
        }
    }

    private fun obterTituloGrandPrix(nome: String): String {
        val titulo = nome.trim()

        if (titulo.isBlank()) {
            return "GRANDE PRÊMIO"
        }

        return if (titulo.endsWith("Grand Prix", ignoreCase = true)) {
            titulo
        } else {
            "$titulo Grand Prix"
        }
    }

    private fun adicionarFimDeSemana(
        race: JSONObject,
        dataCorrida: LocalDate
    ) {
        val campos = listOf(
            "FirstPractice",
            "SecondPractice",
            "ThirdPractice",
            "SprintQualifying",
            "SprintShootout",
            "Sprint",
            "Qualifying"
        )

        val datas = mutableListOf<LocalDate>()

        for (campo in campos) {
            val sessao = race.optJSONObject(campo) ?: continue
            val dataTexto = sessao.optString("date", "")

            try {
                datas.add(LocalDate.parse(dataTexto))
            } catch (_: Exception) {
                // Ignora datas ausentes ou inválidas.
            }
        }

        datas.add(dataCorrida)

        val inicio = datas.minOrNull() ?: dataCorrida
        val fim = dataCorrida

        val formatoDia = DateTimeFormatter.ofPattern(
            "dd",
            localePt
        )

        val formatoMes = DateTimeFormatter.ofPattern(
            "MMM",
            localePt
        )

        val formatoMesCompleto = DateTimeFormatter.ofPattern(
            "MMMM",
            localePt
        )

        val intervalo = when {
            inicio.year != fim.year -> {
                "${inicio.format(DateTimeFormatter.ofPattern("dd MMM yyyy", localePt))} – " +
                    fim.format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy", localePt)
                    )
            }

            inicio.month != fim.month -> {
                "${inicio.format(formatoDia)} " +
                    "${inicio.format(formatoMes)} – " +
                    "${fim.format(DateTimeFormatter.ofPattern("dd MMM", localePt))}"
            }

            else -> {
                "${inicio.format(formatoDia)} – " +
                    "${fim.format(formatoDia)} " +
                    fim.format(formatoMesCompleto)
            }
        }.uppercase(localePt)

        adicionarTexto(
            intervalo,
            16f,
            branco,
            true
        ).setPadding(0, dp(1), 0, dp(15))
    }

    private fun obterSessoes(race: JSONObject): List<Sessao> {
        val resultado = mutableListOf<Sessao>()

        val campos = listOf(
            "FirstPractice" to "TREINO LIVRE 1",
            "SecondPractice" to "TREINO LIVRE 2",
            "ThirdPractice" to "TREINO LIVRE 3",
            "SprintQualifying" to "CLASSIFICAÇÃO SPRINT",
            "SprintShootout" to "CLASSIFICAÇÃO SPRINT",
            "Sprint" to "CORRIDA SPRINT",
            "Qualifying" to "CLASSIFICAÇÃO"
        )

        val nomesAdicionados = mutableSetOf<String>()

        for ((campo, nome) in campos) {
            if (!race.has(campo)) continue

            val sessao = race.optJSONObject(campo) ?: continue
            val dataTexto = sessao.optString("date", "")

            val data = try {
                LocalDate.parse(dataTexto)
            } catch (_: Exception) {
                continue
            }

            if (nome in nomesAdicionados) continue

            val horaTexto = sessao.optString("time", "")
            val horario = converterHorario(dataTexto, horaTexto)

            resultado.add(
                Sessao(
                    nome = nome,
                    data = horario?.toLocalDate() ?: data,
                    horario = horario
                )
            )

            nomesAdicionados.add(nome)
        }

        val dataCorridaTexto = race.optString("date", "")
        val horaCorridaTexto = race.optString("time", "")

        val dataCorrida = try {
            LocalDate.parse(dataCorridaTexto)
        } catch (_: Exception) {
            null
        }

        if (dataCorrida != null) {
            val horarioCorrida = converterHorario(
                dataCorridaTexto,
                horaCorridaTexto
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

        return resultado
    }

    private fun converterHorario(
        dataTexto: String,
        horaTexto: String
    ): ZonedDateTime? {
        if (dataTexto.isBlank() || horaTexto.isBlank()) {
            return null
        }

        return try {
            OffsetDateTime.parse("${dataTexto}T${horaTexto}")
                .atZoneSameInstant(brasilia)
        } catch (_: Exception) {
            try {
                LocalDate.parse(dataTexto).atStartOfDay(brasilia)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun adicionarSeparador() {
        val separador = View(this).apply {
            setBackgroundColor(linhaCinza)
        }

        conteudo.addView(
            separador,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                bottomMargin = dp(16)
            }
        )
    }

    private fun adicionarGrupoDia(data: LocalDate) {
        val titulo = data.format(
            DateTimeFormatter.ofPattern(
                "EEEE, dd 'DE' MMMM",
                localePt
            )
        ).uppercase(localePt)

        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val marcador = View(this).apply {
            setBackgroundColor(verde)
        }

        linha.addView(
            marcador,
            LinearLayout.LayoutParams(dp(3), dp(20)).apply {
                rightMargin = dp(9)
            }
        )

        val texto = TextView(this).apply {
            text = titulo
            textSize = 12f
            setTextColor(branco)
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.BOLD
            )
        }

        linha.addView(
            texto,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        conteudo.addView(
            linha,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(7)
                bottomMargin = dp(9)
            }
        )
    }

    private fun adicionarSessao(sessao: Sessao) {
        val cartao = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))

            background = GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()

                setColor(
                    if (sessao.corrida) {
                        Color.rgb(15, 45, 15)
                    } else {
                        fundoCard
                    }
                )

                setStroke(
                    dp(if (sessao.corrida) 2 else 1),
                    if (sessao.corrida) verde else verdeEscuro
                )
            }
        }

        val nome = TextView(this).apply {
            text = sessao.nome
            textSize = 13f
            setTextColor(if (sessao.corrida) verde else branco)
            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
            letterSpacing = 0.02f
        }

        cartao.addView(
            nome,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val horarioTexto = sessao.horario?.format(
            DateTimeFormatter.ofPattern("HH:mm")
        ) ?: "--:--"

        val horario = TextView(this).apply {
            text = horarioTexto
            textSize = 16f
            setTextColor(if (sessao.corrida) verde else branco)
            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
            gravity = Gravity.END
        }

        cartao.addView(
            horario,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(10)
            }
        )

        conteudo.addView(
            cartao,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        )
    }

    private fun adicionarTexto(
        texto: String,
        tamanho: Float,
        cor: Int,
        negrito: Boolean
    ): TextView {
        val view = TextView(this).apply {
            text = texto
            textSize = tamanho
            setTextColor(cor)

            if (negrito) {
                typeface = Typeface.create(
                    "sans-serif",
                    Typeface.BOLD
                )
            }
        }

        conteudo.addView(
            view,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return view
    }

    private fun mostrarErro(mensagem: String) {
        conteudo.removeAllViews()

        adicionarTexto(
            "NÃO FOI POSSÍVEL CARREGAR",
            17f,
            verde,
            true
        )

        adicionarTexto(
            mensagem,
            14f,
            branco,
            false
        ).setPadding(0, dp(10), 0, dp(15))

        val tentarNovamente = TextView(this).apply {
            text = "TOQUE AQUI PARA TENTAR NOVAMENTE"
            textSize = 13f
            setTextColor(verde)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(12), 0, dp(12))

            setOnClickListener {
                conteudo.removeAllViews()

                adicionarTexto(
                    "CARREGANDO PROGRAMAÇÃO...",
                    14f,
                    verde,
                    true
                )

                Thread {
                    try {
                        val corrida = buscarCorridaSelecionada()

                        runOnUiThread {
                            if (corrida != null) {
                                mostrarEvento(corrida)
                            } else {
                                mostrarErro(
                                    "Não foi encontrada uma próxima etapa."
                                )
                            }
                        }
                    } catch (_: Exception) {
                        runOnUiThread {
                            mostrarErro(
                                "Falha na conexão. Tente novamente."
                            )
                        }
                    }
                }.start()
            }
        }

        conteudo.addView(tentarNovamente)
    }

    private fun obterRecursoBandeira(pais: String): Int {
        val nome = obterBandeira(pais)

        if (nome.isBlank()) return 0

        return resources.getIdentifier(
            nome,
            "drawable",
            packageName
        )
    }

    private fun obterBandeira(pais: String): String {
        return when (pais.trim().lowercase(localePt)) {
            "bahrain" -> "flag_bh"
            "saudi arabia" -> "flag_sa"
            "australia" -> "flag_au"
            "japan" -> "flag_jp"
            "china" -> "flag_cn"
            "united states", "usa", "miami" -> "flag_us"
            "canada" -> "flag_ca"
            "monaco" -> "flag_mc"
            "spain", "catalunya" -> "flag_es"
            "austria" -> "flag_at"
            "united kingdom", "great britain" -> "flag_gb"
            "belgium" -> "flag_be"
            "hungary" -> "flag_hu"
            "netherlands" -> "flag_nl"
            "italy" -> "flag_it"
            "azerbaijan" -> "flag_az"
            "singapore" -> "flag_sg"
            "mexico" -> "flag_mx"
            "brazil", "são paulo" -> "flag_br"
            "united arab emirates" -> "flag_ae"
            "qatar" -> "flag_qa"
            "portugal" -> "flag_pt"
            "france" -> "flag_fr"
            "germany" -> "flag_de"
            "switzerland" -> "flag_ch"
            "south korea" -> "flag_kr"
            "south africa" -> "flag_za"
            "turkey" -> "flag_tr"
            "russia" -> "flag_ru"
            else -> ""
        }
    }

    private fun dp(valor: Int): Int {
        return (
            valor * resources.displayMetrics.density
        ).toInt()
    }
}