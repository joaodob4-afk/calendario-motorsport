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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class FormulaEActivity : AppCompatActivity() {

    private val preto = Color.BLACK
    private val verde = Color.rgb(57, 255, 20)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(175, 175, 175)
    private val linhaCinza = Color.rgb(55, 55, 55)
    private val fundoCard = Color.rgb(15, 20, 15)
    private val verdeEscuro = Color.rgb(20, 75, 20)

    private val localePt = Locale("pt", "BR")
    private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private lateinit var conteudo: LinearLayout
    private lateinit var scrollView: ScrollView

    data class Sessao(
        val nome: String,
        val data: LocalDate,
        val horario: String?,
        val corrida: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        montarTela()

        // Baixa o calendário mais recente para a próxima abertura.
        FormulaECalendarJson.atualizar(this)

        carregarEvento()
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
            text = "FÓRMULA E"
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

    private fun lerData(texto: String): LocalDate? {
        return try {
            LocalDate.parse(texto, formatoData)
        } catch (_: Exception) {
            null
        }
    }

    private fun carregarEvento() {
        conteudo.removeAllViews()

        val eventos = FormulaECalendarJson.carregar(this)

        if (eventos.isEmpty()) {
            mostrarErro(
                "O calendário da Fórmula E ainda não foi carregado. " +
                    "Abra o app com internet e tente novamente."
            )
            return
        }

        val hoje = LocalDate.now()

        val proximo = eventos.firstOrNull {
            val fim = lerData(it.fim)
            fim != null && !fim.isBefore(hoje)
        }

        val etapaEscolhida = intent.getIntExtra("ETAPA", -1)

        val evento = eventos.find { it.etapa == etapaEscolhida } ?: proximo

        if (evento == null) {
            mostrarErro("A temporada da Fórmula E já terminou.")
            return
        }

        mostrarEvento(evento, evento == proximo)
    }

    private fun mostrarEvento(
        evento: FormulaEEvent,
        ehProxima: Boolean
    ) {
        conteudo.removeAllViews()

        adicionarTexto(
            if (ehProxima) "PRÓXIMA ETAPA" else "ETAPA",
            12f,
            verde,
            true
        ).letterSpacing = 0.16f

        adicionarTexto(
            "${evento.circuito} E-Prix".uppercase(localePt),
            28f,
            branco,
            true
        ).setPadding(0, dp(5), 0, dp(5))

        if (evento.sessoes.size > 1) {
            adicionarTexto(
                "Rodada dupla",
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
            val id = obterRecursoBandeira(evento.pais)

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
            text = "${evento.circuito}, ${evento.pais}"
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

        adicionarIntervalo(evento)
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

        val sessoes = obterSessoes(evento)

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

            for (sessao in sessoesDoDia.sortedBy { it.horario ?: "99:99" }) {
                adicionarSessao(sessao)
            }
        }

        if (sessoes.any { it.horario == null }) {
            adicionarTexto(
                "Horários em Brasília, divulgados pela Fórmula E " +
                    "perto da data da etapa.",
                12f,
                cinza,
                false
            ).setPadding(0, dp(8), 0, 0)
        }
    }

    private fun adicionarIntervalo(evento: FormulaEEvent) {
        val inicio = lerData(evento.inicio)
        val fim = lerData(evento.fim)

        if (inicio == null || fim == null) return

        val formatoDia = DateTimeFormatter.ofPattern("dd", localePt)
        val formatoMes = DateTimeFormatter.ofPattern("MMM", localePt)
        val formatoMesCompleto = DateTimeFormatter.ofPattern("MMMM", localePt)

        val intervalo = when {
            inicio == fim ->
                fim.format(
                    DateTimeFormatter.ofPattern("dd MMMM yyyy", localePt)
                )

            inicio.year != fim.year ->
                "${inicio.format(DateTimeFormatter.ofPattern("dd MMM yyyy", localePt))} – " +
                    fim.format(DateTimeFormatter.ofPattern("dd MMM yyyy", localePt))

            inicio.month != fim.month ->
                "${inicio.format(formatoDia)} ${inicio.format(formatoMes)} – " +
                    fim.format(DateTimeFormatter.ofPattern("dd MMM", localePt))

            else ->
                "${inicio.format(formatoDia)} – ${fim.format(formatoDia)} " +
                    fim.format(formatoMesCompleto)
        }.uppercase(localePt)

        adicionarTexto(
            intervalo,
            16f,
            branco,
            true
        ).setPadding(0, dp(1), 0, dp(15))
    }

    private fun obterSessoes(evento: FormulaEEvent): List<Sessao> {
        val horaValida = Regex("^\\d{1,2}:\\d{2}$")
        val resultado = mutableListOf<Sessao>()

        for (sessao in evento.sessoes) {
            val data = lerData(sessao.data) ?: continue

            val horario = sessao.horario.trim().takeIf {
                horaValida.matches(it)
            }

            resultado.add(
                Sessao(
                    nome = sessao.nome.uppercase(localePt),
                    data = data,
                    horario = horario,
                    corrida = sessao.nome.contains("corrida", ignoreCase = true)
                )
            )
        }

        return resultado
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

        val horario = TextView(this).apply {
            text = sessao.horario ?: "A CONFIRMAR"
            textSize = if (sessao.horario != null) 16f else 12f
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
                carregarEvento()
            }
        }

        conteudo.addView(tentarNovamente)
    }

    private fun obterRecursoBandeira(pais: String): Int {
        val nome = when (pais.trim()) {
            "Arábia Saudita" -> "flag_sa"
            "México" -> "flag_mx"
            "Estados Unidos" -> "flag_us"
            "Brasil" -> "flag_br"
            "Alemanha" -> "flag_de"
            "Mônaco" -> "flag_mc"
            "Reino Unido" -> "flag_gb"
            "Holanda" -> "flag_nl"
            "Espanha" -> "flag_es"
            "China" -> "flag_cn"
            "Japão" -> "flag_jp"
            "Itália" -> "flag_it"
            "Hungria" -> "flag_hu"
            "Bélgica" -> "flag_be"
            "Áustria" -> "flag_at"
            "Austrália" -> "flag_au"
            "Singapura" -> "flag_sg"
            "Catar" -> "flag_qa"
            "Emirados Árabes Unidos", "Abu Dhabi" -> "flag_ae"
            else -> ""
        }

        if (nome.isBlank()) return 0

        return resources.getIdentifier(
            nome,
            "drawable",
            packageName
        )
    }

    private fun dp(valor: Int): Int {
        return (
            valor * resources.displayMetrics.density
        ).toInt()
    }
}