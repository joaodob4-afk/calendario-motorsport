package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class IndyCarActivity : AppCompatActivity() {

    // Visual igual ao da tela inicial: a cor de destaque é a da categoria (ver DetalheEstilo).
    private val estilo by lazy { DetalheEstilo(this, "IndyCar") }
    private val preto: Int get() = estilo.fundo
    private val verde: Int get() = estilo.corDeDestaque
    private val branco: Int get() = estilo.textoPrincipal
    private val cinza: Int get() = estilo.textoSuave
    private val linhaCinza: Int get() = estilo.borda
    private val fundoCard: Int get() = estilo.superficie
    private val verdeEscuro: Int get() = estilo.borda
    private val fundoCorrida: Int get() = estilo.fundoCorrida

    private val idioma: Locale by lazy { Idioma.locale(this) }
    private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private lateinit var conteudo: LinearLayout
    private lateinit var scrollView: ScrollView
    private val executorCarregamento = Executors.newSingleThreadExecutor()
    private val handlerPrincipal = Handler(Looper.getMainLooper())
    private val geracaoCarregamento = AtomicInteger(0)

    data class Sessao(
        val nome: String,
        val data: LocalDate,
        val horario: String?,
        val corrida: Boolean
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

        // Baixa o calendário e atualiza a tela quando salvar com sucesso.
IndyCarCalendarJson.atualizar(this) {
    if (!isFinishing && !isDestroyed) {
        carregarEvento()
    }
}


        carregarEvento()
    }

    private fun montarTela() {
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(preto)
            setPadding(0, 0, 0, dp(20))
        }

        raiz.addView(
            estilo.cabecalho("INDYCAR", getString(R.string.cd_voltar)) {
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
            setPadding(dp(18), 0, dp(18), 0)
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
        pintarBarrasDoSistema(
            preto,
            getColor(R.color.azul_marinho),
            getColor(R.color.azul_marinho)
        )
    }

    private fun lerData(texto: String): LocalDate? {
        return try {
            LocalDate.parse(texto, formatoData)
        } catch (e: Exception) {
            registrarDescarte("IndyCarActivity.lerData", e)
            null
        }
    }

    /** Lê e interpreta o calendário fora da thread principal. */
    private fun carregarEvento() {
        if (isFinishing || isDestroyed) return
        val geracao = geracaoCarregamento.incrementAndGet()
        executorCarregamento.execute {
            val resultado = runCatching { IndyCarCalendarJson.carregar(this) }
            handlerPrincipal.post {
                if (geracao != geracaoCarregamento.get() || isFinishing || isDestroyed) return@post
                val eventos = resultado.getOrElse {
                    registrarFalha("IndyCarActivity.carregarEvento", it)
                    emptyList()
                }
                mostrarEventoCarregado(eventos)
            }
        }
    }

    private fun mostrarEventoCarregado(eventos: List<IndyCarEvent>) {
        if (isFinishing || isDestroyed) return
        conteudo.removeAllViews()
        if (eventos.isEmpty()) {
            mostrarErro(getString(R.string.calendario_nao_carregado, getString(R.string.nome_indycar)))
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
            mostrarErro(getString(R.string.temporada_terminou, getString(R.string.nome_indycar)))
            return
        }
        mostrarEvento(evento, evento == proximo)
    }

    override fun onDestroy() {
        geracaoCarregamento.incrementAndGet()
        executorCarregamento.shutdownNow()
        super.onDestroy()
    }

    private fun mostrarEvento(
        evento: IndyCarEvent,
        ehProxima: Boolean
    ) {
        conteudo.removeAllViews()

        conteudo.addView(
            estilo.heroi(
                eyebrow = if (ehProxima) getString(R.string.etapa_proxima) else getString(R.string.etapa),
                nome = evento.nome,
                circuito = Idioma.lugar(this, evento.circuito),
                bandeiraRes = obterRecursoBandeira(evento.pais),
                local = if (evento.local.isNotBlank()) {
                    "${Idioma.lugar(this, evento.local)}, ${Idioma.lugar(this, evento.pais)}"
                } else {
                    Idioma.lugar(this, evento.pais)
                },
                periodo = textoDoIntervalo(evento),
                contador = estilo.contador(lerData(evento.inicio), lerData(evento.fim))
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

        val sessoes = obterSessoes(evento)

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

            for (sessao in sessoesDoDia.sortedBy { it.horario ?: "99:99" }) {
                adicionarSessao(sessao)
            }
        }

    }

    private fun textoDoIntervalo(evento: IndyCarEvent): String {
        val inicio = lerData(evento.inicio)
        val fim = lerData(evento.fim)

        if (inicio == null || fim == null) return ""

        if (Idioma.ingles(this)) {
            return Formatacao.periodoEmIngles(inicio, fim).orEmpty().uppercase(idioma)
        }

        val formatoDia = DateTimeFormatter.ofPattern("dd", idioma)
        val formatoMes = DateTimeFormatter.ofPattern("MMM", idioma)
        val formatoMesCompleto = DateTimeFormatter.ofPattern("MMMM", idioma)

        return when {
            inicio == fim ->
                fim.format(
                    DateTimeFormatter.ofPattern("dd MMMM yyyy", idioma)
                )

            inicio.year != fim.year ->
                "${inicio.format(DateTimeFormatter.ofPattern("dd MMM yyyy", idioma))} – " +
                    fim.format(DateTimeFormatter.ofPattern("dd MMM yyyy", idioma))

            inicio.month != fim.month ->
                "${inicio.format(formatoDia)} ${inicio.format(formatoMes)} – " +
                    fim.format(DateTimeFormatter.ofPattern("dd MMM", idioma))

            else ->
                "${inicio.format(formatoDia)} – ${fim.format(formatoDia)} " +
                    fim.format(formatoMesCompleto)
        }.uppercase(idioma)
    }

    private fun obterSessoes(evento: IndyCarEvent): List<Sessao> {
        val horaValida = Regex("^\\d{1,2}:\\d{2}$")
        val resultado = mutableListOf<Sessao>()

        for (sessao in evento.sessoes.map { it.noFusoDoAparelho() }) {
            val data = lerData(sessao.data) ?: continue

            val horario = sessao.horario.trim().takeIf {
                horaValida.matches(it)
            }

            resultado.add(
                Sessao(
                    nome = Idioma.sessao(this, sessao.nome).uppercase(idioma),
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
            DateTimeFormatter.ofPattern(getString(R.string.formato_dia_semana), idioma)
        ).uppercase(idioma)
        estilo.grupoDia(conteudo, titulo)
    }
    private fun adicionarSessao(sessao: Sessao) {
        val horario = sessao.horario?.let { Hora.deTexto(this, it) }
            ?: getString(R.string.a_confirmar)
        estilo.sessao(conteudo, sessao.nome, horario, sessao.corrida)
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
                carregarEvento()
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