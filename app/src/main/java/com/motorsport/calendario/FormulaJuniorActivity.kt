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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tela de detalhes de uma etapa de F2 ou F3 (mesmo layout e mesma lógica).
 * F2Activity e F3Activity só informam categoria, título e fonte dos dados.
 */
abstract class FormulaJuniorActivity : AppCompatActivity() {

    protected abstract val categoria: String
    protected abstract val tituloRes: Int
    protected abstract val calendario: CalendarioFormulaJunior


    // Visual igual ao da tela inicial: a cor de destaque é a da categoria (ver DetalheEstilo).
    private val estilo by lazy { DetalheEstilo(this, categoria) }
    private val preto: Int get() = estilo.fundo
    private val verde: Int get() = estilo.corDeDestaque
    private val branco: Int get() = estilo.textoPrincipal
    private val cinza: Int get() = estilo.textoSuave
    private val linhaCinza: Int get() = estilo.borda
    private val fundoCard: Int get() = estilo.superficie
    private val verdeEscuro: Int get() = estilo.borda
    private val fundoCorrida: Int get() = estilo.fundoCorrida

    private val idioma: Locale by lazy { Idioma.locale(this) }
    private val parsersData: List<SimpleDateFormat> by lazy {
        listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy")
            .map { formato -> SimpleDateFormat(formato, idioma).apply { isLenient = false } }
    }
    private val formatoDiaSemana by lazy { SimpleDateFormat(getString(R.string.formato_dia_semana), idioma) }
    private val formatoDiaMes by lazy { SimpleDateFormat(getString(R.string.formato_dia_mes), idioma) }

    private lateinit var conteudo: LinearLayout
    private val executorCarregamento = Executors.newSingleThreadExecutor()
    private val handlerPrincipal = Handler(Looper.getMainLooper())
    private val geracaoCarregamento = AtomicInteger(0)

    private var etapaEhProxima = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Atualiza os detalhes assim que o calendário novo for salvo.
        calendario.atualizar(this) {
            if (!isFinishing && !isDestroyed) {
                carregarEvento()
            }
        }

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    voltar()
                }
            }
        )

        montarTela()

        carregarEvento()
    }

    /** Lê e interpreta o calendário fora da thread principal. */
    private fun carregarEvento() {
        if (!::conteudo.isInitialized || isFinishing || isDestroyed) return
        val geracao = geracaoCarregamento.incrementAndGet()
        executorCarregamento.execute {
            val resultado = runCatching { calendario.carregar(this) }
            handlerPrincipal.post {
                if (geracao != geracaoCarregamento.get() || isFinishing || isDestroyed) return@post
                val eventos = resultado.getOrElse {
                    registrarFalha("FormulaJuniorActivity.carregarEvento", it)
                    emptyList()
                }
                mostrarEventoCarregado(eventos)
            }
        }
    }

    private fun mostrarEventoCarregado(eventos: List<FormulaJuniorEvent>) {
        if (!::conteudo.isInitialized || isFinishing || isDestroyed) return
        conteudo.removeAllViews()
        val proximaEtapa = obterProximaEtapa(eventos)
        val etapaEscolhida = intent.getIntExtra("ETAPA", -1)
        val evento = eventos.find { it.etapa == etapaEscolhida } ?: proximaEtapa
        etapaEhProxima = evento == null || evento == proximaEtapa
        if (evento != null) {
            mostrarEvento(evento)
        } else {
            adicionarTexto(getString(R.string.calendario_indisponivel), 17f, verde, true)
            adicionarTexto(getString(R.string.sem_proxima_etapa_calendario), 14f, cinza, false)
        }
    }

    override fun onDestroy() {
        geracaoCarregamento.incrementAndGet()
        executorCarregamento.shutdownNow()
        super.onDestroy()
    }

    private fun obterProximaEtapa(
        eventos: List<FormulaJuniorEvent>
    ): FormulaJuniorEvent? {
        val hoje = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.US
        ).format(Date())

        val dataHoje = converterData(hoje) ?: return null

        return eventos
            .filter { evento ->
                val dataFim = converterData(evento.fim)
                dataFim != null && !dataFim.before(dataHoje)
            }
            .sortedBy { evento ->
                converterData(evento.inicio)?.time
                    ?: Long.MAX_VALUE
            }
            .firstOrNull()
    }

    private fun montarTela() {
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(preto)
            setPadding(0, 0, 0, dp(20))
        }

        raiz.addView(
            estilo.cabecalho(getString(tituloRes), getString(R.string.cd_voltar)) {
                voltar()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        )

        val scrollView = ScrollView(this).apply {
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

    private fun voltar() {
        finish()

        overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }

    private fun mostrarEvento(evento: FormulaJuniorEvent) {
        conteudo.removeAllViews()

        val nomeAutodromo = Autodromos.nome(evento.circuito)

        val inicio = formatarData(evento.inicio)
        val fim = formatarData(evento.fim)

        val intervalo = if (
            inicio.isNotBlank() && fim.isNotBlank()
        ) {
            "$inicio – $fim"
        } else {
            inicio.ifBlank { fim }
        }

        conteudo.addView(
            estilo.heroi(
                eyebrow = if (etapaEhProxima) getString(R.string.etapa_proxima) else getString(R.string.etapa),
                nome = evento.nomeEvento.takeIf { it.isNotBlank() }?.let { Idioma.evento(this, it) }
                    ?: NomesDeEventos.nome(categoria, evento.circuito)?.let { Idioma.evento(this, it) }
                    ?: getString(tituloRes),
                circuito = nomeAutodromo.ifBlank { evento.circuito },
                bandeiraRes = obterRecursoBandeira(evento.pais),
                // Sem nome de autódromo mapeado (caso da NASCAR), o circuito já aparece acima:
                // a linha da bandeira mostra só o país, sem repetir a pista.
                local = if (nomeAutodromo.isBlank()) {
                    Idioma.lugar(this, evento.pais)
                } else {
                    "${Idioma.lugar(this, evento.circuito)}, ${Idioma.lugar(this, evento.pais)}"
                },
                periodo = intervalo.uppercase(idioma),
                contador = estilo.contador(converterData(evento.inicio), converterData(evento.fim))
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

        if (evento.sessoes.isEmpty()) {
            adicionarTexto(
                getString(R.string.horarios_indisponiveis),
                14f,
                cinza,
                false
            )
            return
        }

        val sessoesOrdenadas = PreparadorDeSessoes.ordenar(
            evento.sessoes.map { it.noFusoDoAparelho() },
            obterData = { converterData(it.data) },
            obterHorario = { it.horario }
        )

        var dataAtual: String? = null

        for (preparada in sessoesOrdenadas) {
            val sessao = preparada.sessao
            val dataSessao = formatarDiaPorExtenso(preparada.data, sessao.data)
                .ifBlank { sessao.data }

            if (dataSessao != dataAtual) {
                adicionarGrupoDia(dataSessao)
                dataAtual = dataSessao
            }

            adicionarSessao(sessao)
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

    private fun adicionarGrupoDia(data: String) {
        estilo.grupoDia(conteudo, data.uppercase(idioma))
    }
    private fun adicionarSessao(sessao: FormulaJuniorSession) {
        val nomeOriginal = sessao.nome.trim()
        val nomeNormalizado = nomeOriginal.lowercase(idioma)
        val corridaPrincipal =
            nomeNormalizado.contains("feature") ||
            nomeNormalizado.contains("corrida principal") ||
            nomeNormalizado == "corrida" ||
            nomeNormalizado == "race"
        val sprint = nomeNormalizado.contains("sprint")
        val numeroTreino = Regex("""(?:practice|treino)\s*(\d+)""")
            .find(nomeNormalizado)?.groupValues?.get(1)
        val nomeExibido = when {
            nomeNormalizado.contains("feature") ->
                getString(R.string.sessao_corrida_principal)
            nomeNormalizado.contains("practice") && nomeNormalizado.contains("qualifying") ->
                getString(R.string.sessao_treino_livre) + " / " + getString(R.string.sessao_classificacao)
            nomeNormalizado.contains("qualifying race") ->
                Idioma.sessao(this, nomeOriginal).uppercase(idioma)
            nomeNormalizado.contains("practice") || nomeNormalizado.contains("treino") ->
                getString(R.string.sessao_treino_livre) + numeroTreino?.let { " $it" }.orEmpty()
            nomeNormalizado.contains("qualifying") || nomeNormalizado.contains("classificação") ->
                getString(R.string.sessao_classificacao)
            sprint ->
                getString(R.string.sessao_corrida_sprint)
            nomeNormalizado.contains("grid invertido") || nomeNormalizado.contains("reverse") ->
                getString(R.string.sessao_corrida_sprint)
            nomeNormalizado.contains("abertura") || nomeNormalizado.contains("opening") ->
                Idioma.sessao(this, nomeOriginal).uppercase(idioma)
            nomeNormalizado.contains("corrida") || nomeNormalizado == "race" ->
                getString(R.string.sessao_corrida)
            else -> Idioma.sessao(this, nomeOriginal).uppercase(idioma)
        }
        val horario = sessao.horario.trim().let {
            if (it.isBlank() || it.equals("a confirmar", ignoreCase = true)) {
                getString(R.string.a_confirmar)
            } else {
                Hora.deTexto(this, it)
            }
        }
        estilo.sessao(conteudo, nomeExibido, horario, corridaPrincipal)
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

    private fun obterRecursoBandeira(pais: String): Int = Bandeiras.recurso(this, pais)


    /** "QUINTA-FEIRA, 19 DE OUTUBRO": o dia da semana e a data por extenso (maiúsculas ao exibir). */
    private fun formatarDiaPorExtenso(data: Date?, original: String): String =
        data?.let { formatoDiaSemana.format(it) } ?: original

    private fun formatarData(data: String): String {
        if (data.isBlank()) return ""
        val convertida = converterData(data) ?: return data
        return formatoDiaMes.format(convertida)
    }

    private fun converterData(data: String): Date? {
        if (data.isBlank()) return null
        for (parser in parsersData) {
            try {
                val resultado = parser.parse(data)
                if (resultado != null) return resultado
            } catch (e: Exception) {
                registrarDescarte("FormulaJuniorActivity.converterData", e)
            }
        }
        return null
    }

    private fun dp(valor: Int): Int {
        return (
            valor * resources.displayMetrics.density
        ).toInt()
    }
}