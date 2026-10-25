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

    // Visual igual ao da tela inicial: a cor de destaque é a da categoria (ver DetalheEstilo).
    private val estilo by lazy { DetalheEstilo(this, "F1") }
    private val preto: Int get() = estilo.fundo
    private val verde: Int get() = estilo.corDeDestaque
    private val branco: Int get() = estilo.textoPrincipal
    private val cinza: Int get() = estilo.textoSuave
    private val linhaCinza: Int get() = estilo.borda
    private val fundoCard: Int get() = estilo.superficie
    private val verdeEscuro: Int get() = estilo.borda
    private val fundoCorrida: Int get() = estilo.fundoCorrida

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

        // O botão físico (ou o gesto) de voltar usa o mesmo slide do botão da tela.
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
        )

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
                    if (isFinishing || isDestroyed) return@runOnUiThread
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
                    if (isFinishing || isDestroyed) return@runOnUiThread
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

        raiz.addView(
            estilo.cabecalho(getString(R.string.formula_1), getString(R.string.cd_voltar)) {
                finish()
                overridePendingTransition(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
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

        // A janela e as barras do sistema seguem a cor do fundo, sem faixa mais escura.
        pintarBarrasDoSistema(preto, getColor(R.color.azul_marinho))
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

        // O nome que a própria fonte dá à corrida, igual ao da tela inicial; só se ela não trouxer
        // nome é que vale a tabela do app.
        val nomeDaFonte = race.optString("raceName", "").trim()
            .takeIf { it.isNotBlank() && it != "Grande Prêmio" }

        val nomeGP = nomeDaFonte ?: Idioma.evento(
            this,
            NomesDeEventos.nome(
                "F1",
                race.optJSONObject("Circuit")?.optJSONObject("Location")
                    ?.optString("locality", "").orEmpty(),
                race.optJSONObject("Circuit")?.optString("circuitId", "").orEmpty(),
                race.optJSONObject("Circuit")?.optJSONObject("Location")
                    ?.optString("country", "").orEmpty()
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
            Fuso.dataDaF1(race.optString("date", ""), race.optString("time", ""))
                ?: throw IllegalArgumentException("data da corrida inválida")
        } catch (e: Exception) {
            registrarDescarte("F1Activity.mostrarEvento", e)
            LocalDate.now(brasilia)
        }

        val (intervalo, inicioFimDeSemana, fimFimDeSemana) =
            intervaloDoFimDeSemana(race, dataCorrida)

        conteudo.addView(
            estilo.heroi(
                eyebrow = if (corridaEhProxima) getString(R.string.etapa_proxima) else getString(R.string.etapa),
                nome = nomeGP,
                circuito = nomeCircuito,
                bandeiraRes = obterRecursoBandeira(pais),
                local = listOf(cidade, pais)
                    .filter { it.isNotBlank() }
                    .joinToString(", "),
                periodo = intervalo,
                contador = estilo.contador(inicioFimDeSemana, fimFimDeSemana)
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(18)
            }
        )

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

    /** Período do fim de semana (do 1º treino à corrida), mais a primeira e a última data. */
    private fun intervaloDoFimDeSemana(
        race: JSONObject,
        dataCorrida: LocalDate
    ): Triple<String, LocalDate, LocalDate> {
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
                datas.add(
                    Fuso.dataDaF1(dataTexto, sessao.optString("time", ""))
                        ?: throw IllegalArgumentException("data da sessão inválida")
                )
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

        return Triple(intervalo, inicio, fim)
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
                cornerRadius = dp(12).toFloat()

                setColor(
                    if (sessao.corrida) {
                        fundoCorrida
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
            textSize = 12f
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

        val horarioTexto = sessao.horario
            ?.let { Hora.deZonedDateTime(this, it) }
            ?: "--:--"

        val horario = TextView(this).apply {
            text = horarioTexto
            textSize = 14f
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
                            if (isFinishing || isDestroyed) return@runOnUiThread
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
                            if (isFinishing || isDestroyed) return@runOnUiThread
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

    private fun obterRecursoBandeira(pais: String): Int = Bandeiras.recurso(this, pais)

    private fun dp(valor: Int): Int {
        return (
            valor * resources.displayMetrics.density
        ).toInt()
    }
}