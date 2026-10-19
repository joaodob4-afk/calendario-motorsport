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

    // Apesar do nome, é o fuso atual do aparelho (os horários da F1 vêm em UTC).
    private val brasilia: ZoneId = ZoneId.systemDefault()
    private val idioma: Locale by lazy { Idioma.locale(this) }

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
            getString(R.string.f1_carregando),
            14f,
            verde,
            true
        )

        Thread {
            try {
                val corrida = buscarCorridaSelecionada()

                runOnUiThread {
                    try {
                        if (corrida != null) {
                            mostrarEvento(corrida)
                        } else {
                            mostrarErro(
                                getString(R.string.f1_sem_proxima)
                            )
                        }
                    } catch (e: Exception) {
                        // Esta parte roda fora do try da thread: sem isto um dado
                        // inesperado fecharia o app em vez de mostrar o erro.
                        registrarFalha("F1Activity.mostrarEvento", e)
                        mostrarErro(getString(R.string.f1_erro_carregar))
                    }
                }
            } catch (e: Exception) {
                registrarFalha("F1Activity.onCreate", e)
                runOnUiThread {
                    mostrarErro(
                        getString(R.string.f1_erro_carregar)
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

            contentDescription = getString(R.string.cd_voltar)
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
            text = getString(R.string.formula_1)
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
        aplicarInsetsDasBarras()
    }

    // false quando o usuário abriu uma corrida específica (não a próxima).
    private var corridaEhProxima = true

    /**
     * Usa a corrida escolhida pelo usuário (extra "RACE" ou
     * "CIRCUIT_ID_F1"). Sem escolha, ou se não achar, usa a próxima.
     */
    private fun buscarCorridaSelecionada(): JSONObject? {
        val corridaTexto = intent.getStringExtra("RACE")
        // O calendário envia a corrida em "RACE"; extraímos o circuito desse
        // objeto para localizar a versão detalhada salva, inclusive offline.
        val circuitId = intent.getStringExtra("CIRCUIT_ID_F1")
            ?.takeIf { it.isNotBlank() }
            ?: try {
                if (corridaTexto.isNullOrBlank()) {
                    null
                } else {
                    JSONObject(corridaTexto)
                        .optJSONObject("Circuit")
                        ?.optString("circuitId", "")
                        ?.takeIf { it.isNotBlank() }
                }
            } catch (e: Exception) {
                registrarFalha("F1Activity.buscarCorridaSelecionada", e)
                null
            }

        // A tela inicial pode enviar apenas um resumo da etapa, sem sessões.
        // Quando há circuito selecionado, prioriza o cache completo antes do resumo.
        if (!circuitId.isNullOrBlank()) {
            val jsonCache = F1CalendarApi.corridaJsonDoCache(this, circuitId)
            if (!jsonCache.isNullOrBlank()) {
                try {
                    val corridaCache = JSONObject(jsonCache)
                    val temSessoes = listOf(
                        "FirstPractice", "SecondPractice", "ThirdPractice",
                        "SprintQualifying", "SprintShootout", "Sprint", "Qualifying"
                    ).any { corridaCache.has(it) && corridaCache.optJSONObject(it) != null }

                    if (temSessoes || corridaTexto.isNullOrBlank()) {
                        corridaEhProxima = false
                        return corridaCache
                    }
                } catch (e: Exception) {
                    registrarFalha("F1Activity.buscarCorridaSelecionada", e)
                    // Cache inválido: tenta os outros caminhos.
                }
            }
        }

        // Usa o objeto recebido diretamente apenas se ele já tiver sessões,
        // ou se não houver um circuito para procurar uma versão mais completa.
        if (!corridaTexto.isNullOrBlank()) {
            try {
                val corridaRecebida = JSONObject(corridaTexto)
                val temSessoes = listOf(
                    "FirstPractice", "SecondPractice", "ThirdPractice",
                    "SprintQualifying", "SprintShootout", "Sprint", "Qualifying"
                ).any { corridaRecebida.has(it) && corridaRecebida.optJSONObject(it) != null }

                if (temSessoes || circuitId.isNullOrBlank()) {
                    corridaEhProxima = false
                    return corridaRecebida
                }
            } catch (e: Exception) {
                registrarFalha("F1Activity.buscarCorridaSelecionada", e)
                // Tenta a fonte online.
            }
        }

        if (!circuitId.isNullOrBlank()) {
            val selecionada = try {
                buscarCorridaPorCircuito(circuitId)
            } catch (e: Exception) {
                registrarFalha("F1Activity.buscarCorridaSelecionada", e)
                null
            }

            if (selecionada != null) {
                corridaEhProxima = false
                return selecionada
            }
        }

        corridaEhProxima = true
        return buscarProximaCorrida()
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
            } catch (e: Exception) {
                registrarDescarte("F1Activity.buscarProximaCorrida", e)
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
            } catch (e: Exception) {
                registrarDescarte("F1Activity.buscarProximaCorrida", e)
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

        // Mesmo nome da tela inicial; se o circuito for novo, usa o nome que vem da API.
        val nomeGP = Idioma.evento(
            this,
            NomesDeEventos.nome(
                "F1",
                "",
                race.optJSONObject("Circuit")?.optString("circuitId", "").orEmpty()
            ) ?: obterTituloGrandPrix(
                race.optString("raceName", getString(R.string.grande_premio_nome))
            )
        )

        val circuito = race.optJSONObject("Circuit")
        val nomeCircuito =
            circuito?.optString("circuitName", "") ?: ""

        val localizacao = circuito?.optJSONObject("Location")
        val cidade = localizacao?.optString("locality", "") ?: ""
        val pais = localizacao?.optString("country", "") ?: ""

        val dataCorrida = try {
            LocalDate.parse(race.optString("date", ""))
        } catch (e: Exception) {
            registrarDescarte("F1Activity.mostrarEvento", e)
            LocalDate.now(brasilia)
        }

        adicionarTexto(
            if (corridaEhProxima) getString(R.string.etapa_proxima) else getString(R.string.etapa),
            12f,
            verde,
            true
        ).letterSpacing = 0.16f

        adicionarTexto(
            nomeGP.uppercase(idioma),
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
            getString(R.string.programacao),
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
                getString(R.string.horarios_indisponiveis),
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
            return getString(R.string.grande_premio_titulo)
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
            } catch (e: Exception) {
                registrarDescarte("F1Activity.adicionarFimDeSemana", e)
                // Ignora datas ausentes ou inválidas.
            }
        }

        datas.add(dataCorrida)

        val inicio = datas.minOrNull() ?: dataCorrida
        val fim = dataCorrida

        val formatoDia = DateTimeFormatter.ofPattern(
            "dd",
            idioma
        )

        val formatoMes = DateTimeFormatter.ofPattern(
            "MMM",
            idioma
        )

        val formatoMesCompleto = DateTimeFormatter.ofPattern(
            "MMMM",
            idioma
        )

        val intervalo = when {
            inicio.year != fim.year -> {
                "${inicio.format(DateTimeFormatter.ofPattern("dd MMM yyyy", idioma))} – " +
                    fim.format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy", idioma)
                    )
            }

            inicio.month != fim.month -> {
                "${inicio.format(formatoDia)} " +
                    "${inicio.format(formatoMes)} – " +
                    "${fim.format(DateTimeFormatter.ofPattern("dd MMM", idioma))}"
            }

            else -> {
                "${inicio.format(formatoDia)} – " +
                    "${fim.format(formatoDia)} " +
                    fim.format(formatoMesCompleto)
            }
        }.uppercase(idioma)

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
            "FirstPractice" to getString(R.string.sessao_treino_livre_1),
            "SecondPractice" to getString(R.string.sessao_treino_livre_2),
            "ThirdPractice" to getString(R.string.sessao_treino_livre_3),
            "SprintQualifying" to getString(R.string.sessao_classificacao_sprint),
            "SprintShootout" to getString(R.string.sessao_classificacao_sprint),
            "Sprint" to getString(R.string.sessao_corrida_sprint),
            "Qualifying" to getString(R.string.sessao_classificacao)
        )

        val nomesAdicionados = mutableSetOf<String>()

        for ((campo, nome) in campos) {
            if (!race.has(campo)) continue

            val sessao = race.optJSONObject(campo) ?: continue
            val dataTexto = sessao.optString("date", "")

            val data = try {
                LocalDate.parse(dataTexto)
            } catch (e: Exception) {
                registrarDescarte("F1Activity.obterSessoes", e)
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
        } catch (e: Exception) {
            registrarDescarte("F1Activity.obterSessoes", e)
            null
        }

        if (dataCorrida != null) {
            val horarioCorrida = converterHorario(
                dataCorridaTexto,
                horaCorridaTexto
            )

            resultado.add(
                Sessao(
                    nome = getString(R.string.sessao_corrida),
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
        } catch (e: Exception) {
            registrarDescarte("F1Activity.converterHorario", e)
            try {
                LocalDate.parse(dataTexto).atStartOfDay(brasilia)
            } catch (e: Exception) {
                registrarDescarte("F1Activity.converterHorario", e)
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
                getString(R.string.formato_dia_semana),
                idioma
            )
        ).uppercase(idioma)

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
            getString(R.string.f1_nao_foi_possivel),
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
            text = getString(R.string.f1_tentar_novamente)
            textSize = 13f
            setTextColor(verde)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(12), 0, dp(12))

            setOnClickListener {
                conteudo.removeAllViews()

                adicionarTexto(
                    getString(R.string.f1_carregando),
                    14f,
                    verde,
                    true
                )

                Thread {
                    try {
                        val corrida = buscarCorridaSelecionada()

                        runOnUiThread {
                            try {
                                if (corrida != null) {
                                    mostrarEvento(corrida)
                                } else {
                                    mostrarErro(
                                        getString(R.string.f1_sem_proxima)
                                    )
                                }
                            } catch (e: Exception) {
                                registrarFalha("F1Activity.mostrarEvento", e)
                                mostrarErro(getString(R.string.f1_falha_conexao))
                            }
                        }
                    } catch (e: Exception) {
                        registrarFalha("F1Activity.mostrarErro", e)
                        runOnUiThread {
                            mostrarErro(
                                getString(R.string.f1_falha_conexao)
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
        return when (pais.trim().lowercase(idioma)) {
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