package com.motorsport.calendario

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import org.json.JSONObject
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private companion object {
        const val CHAVE_CATEGORIA = "categoria_atual"
    }

    private var categoriaAtual = ""

    data class Etapa(
        val categoria: String,
        val pais: String,
        val circuito: String,
        val data: String,
        val dataInicio: String = "",
        val circuitId: String = "",
        val dataFim: String = "",
        /** Nome do evento quando a própria fonte já traz um (IndyCar). Vazio: ver [NomesDeEventos]. */
        val nomeEvento: String = "",
        /** Cidade ou local legível, quando a fonte traz um. Vazio: usa [circuito]. */
        val local: String = ""
    )

    private lateinit var listaEtapas: LinearLayout

    /** Resumo do que o carrossel mostra hoje; evita refazê-lo (e voltar ao 1º card) a cada minuto. */
    private var assinaturaDestaques = ""

    private val handlerPesquisa =
        Handler(Looper.getMainLooper())

    private var pesquisaRunnable: Runnable? = null

    private val handlerProximaEtapa =
        Handler(Looper.getMainLooper())

    private val atualizadorProximaEtapa =
        object : Runnable {
            override fun run() {
                if (categoriaAtual.isEmpty()) {
                    atualizarProximaEtapa()
                }

                handlerProximaEtapa.postDelayed(
                    this,
                    60000
                )
            }
        }

    private val handlerRedesenho = Handler(Looper.getMainLooper())

    private val redesenhoDaLista = Runnable {
        if (categoriaAtual.isEmpty()) {
            atualizarProximaEtapa()
            mostrarEtapas(todasEtapas)
        }
    }

    /**
     * Ao abrir o app, vários calendários terminam de baixar quase ao mesmo tempo. Em vez de
     * refazer a lista inteira a cada um, agrupa tudo em um único redesenho.
     */
    private fun redesenharListaDepois() {
        if (isFinishing || isDestroyed) return
        handlerRedesenho.removeCallbacks(redesenhoDaLista)
        handlerRedesenho.postDelayed(redesenhoDaLista, 400)
    }

    private var etapasF1Dinamicas: List<Etapa>? = null
    private var etapasF2Dinamicas: List<Etapa>? = null
    private var etapasF3Dinamicas: List<Etapa>? = null
    private var etapasFormulaEDinamicas: List<Etapa>? = null
    private var etapasIndyCarDinamicas: List<Etapa>? = null
    private var etapasMotoDinamicas: List<Etapa>? = null

    private val todasEtapas: List<Etapa>
        get() =
            (etapasF1Dinamicas ?: emptyList()) +
                (etapasF2Dinamicas ?: emptyList()) +
                (etapasF3Dinamicas ?: emptyList()) +
                (etapasFormulaEDinamicas ?: emptyList()) +
                (etapasIndyCarDinamicas ?: emptyList()) +
                (etapasMotoDinamicas ?: emptyList())

    private fun parseDataSegura(data: String?): java.time.LocalDate? {
        if (data.isNullOrBlank()) return null
        return try {
            java.time.LocalDate.parse(data)
        } catch (e: Exception) {
            registrarDescarte("MainActivity.parseDataSegura", e)
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Em rotação/recriação, não baixa tudo de novo (o cache já está atualizado).
        carregarCalendarios(baixar = savedInstanceState == null)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val gaveta = findViewById<DrawerLayout>(R.id.drawerLayout)
                    if (categoriaAtual.isEmpty() && gaveta != null &&
                        gaveta.isDrawerOpen(GravityCompat.START)
                    ) {
                        gaveta.closeDrawer(GravityCompat.START)
                        return
                    }

                    if (categoriaAtual.isNotEmpty()) {
                        categoriaAtual = ""
                        mostrarMenu()
                        overridePendingTransition(
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        )
                    } else {
                        finish()
                    }
                }
            }
        )

        mostrarMenu()

        savedInstanceState?.getString(CHAVE_CATEGORIA)
            ?.takeIf { it.isNotEmpty() }
            ?.let { abrirCategoria(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CHAVE_CATEGORIA, categoriaAtual)
    }

    /** true até a primeira leitura dos calendários terminar (evita o aviso "nenhuma etapa"). */
    private var carregandoCalendarios = true

    private fun lerEtapas(origem: String, bloco: () -> List<Etapa>?): List<Etapa>? =
        try {
            bloco()
        } catch (e: Exception) {
            registrarFalha("MainActivity.carregarCalendarios.$origem", e)
            null
        }

    /**
     * Lê os calendários (cache ou JSON do APK) fora da thread principal, para a primeira tela
     * aparecer logo. O download das versões novas só começa depois, para uma leitura antiga
     * nunca sobrescrever uma atualização.
     */
    private fun carregarCalendarios(baixar: Boolean = true) {
        val app = applicationContext

        Thread {
            val f1 = lerEtapas("F1") { F1CalendarApi.carregarCache(app) }
            val f2 = lerEtapas("F2") { F2CalendarJson.comoEtapas(F2CalendarJson.carregar(app)) }
            val f3 = lerEtapas("F3") { F3CalendarJson.comoEtapas(F3CalendarJson.carregar(app)) }
            val indy = lerEtapas("IndyCar") {
                IndyCarCalendarJson.comoEtapas(IndyCarCalendarJson.carregar(app))
            }
            val fe = lerEtapas("FormulaE") {
                FormulaECalendarJson.comoEtapas(FormulaECalendarJson.carregar(app))
            }
            val moto = lerEtapas("Moto") { MotoCalendarJson.etapasDeTodas(app) }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                etapasF1Dinamicas = f1
                etapasF2Dinamicas = f2
                etapasF3Dinamicas = f3
                etapasIndyCarDinamicas = indy
                etapasFormulaEDinamicas = fe
                etapasMotoDinamicas = moto
                carregandoCalendarios = false

                if (categoriaAtual.isEmpty()) {
                    atualizarProximaEtapa()
                    mostrarEtapas(todasEtapas)
                }

                if (baixar) baixarCalendariosNovos()
            }
        }.start()
    }

    /** Baixa as versões novas dos calendários; ao terminar cada uma, a lista é atualizada. */
    private fun baixarCalendariosNovos() {
        F2CalendarJson.atualizar(this) {
            recarregar({ F2CalendarJson.comoEtapas(F2CalendarJson.carregar(applicationContext)) }) { etapasF2Dinamicas = it }
        }

        F3CalendarJson.atualizar(this) {
            recarregar({ F3CalendarJson.comoEtapas(F3CalendarJson.carregar(applicationContext)) }) { etapasF3Dinamicas = it }
        }

        IndyCarCalendarJson.atualizar(this) {
            recarregar({ IndyCarCalendarJson.comoEtapas(IndyCarCalendarJson.carregar(applicationContext)) }) { etapasIndyCarDinamicas = it }
        }

        FormulaECalendarJson.atualizar(this) {
            recarregar({ FormulaECalendarJson.comoEtapas(FormulaECalendarJson.carregar(applicationContext)) }) { etapasFormulaEDinamicas = it }
        }

        // MotoGP, Moto2, Moto3 e WEC usam as mesmas telas; cada classe tem o seu calendário.
        for (classe in ClasseMoto.values()) {
            MotoCalendarJson.atualizar(this, classe) {
                recarregar({ MotoCalendarJson.etapasDeTodas(applicationContext) }) { etapasMotoDinamicas = it }
            }
        }

        F1CalendarApi.atualizar(this) {
            recarregar({ F1CalendarApi.carregarCache(applicationContext) }) { etapasF1Dinamicas = it }
        }
    }

    /**
     * Relê um calendário fora da thread principal (disco + JSON), reagenda os avisos e só então
     * aplica o resultado na tela, se ela ainda existir.
     */
    private fun recarregar(carga: () -> List<Etapa>?, aplicar: (List<Etapa>?) -> Unit) {
        val app = applicationContext
        Thread {
            val etapas = lerEtapas("recarregar", carga)
            AgendadorDeAvisos.reagendarEmSegundoPlano(app)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                aplicar(etapas)
                redesenharListaDepois()
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()

        // Refaz os avisos ao abrir o app e depois do pedido de permissão de notificações.
        AgendadorDeAvisos.reagendarEmSegundoPlano(this)

        // O painel lateral mostra o estado das notificações; atualiza ao voltar das configurações.
        findViewById<LinearLayout>(R.id.containerMenuLateral)?.let {
            MenuPrincipal.montar(this, it)
        }
    }

    override fun onDestroy() {
        pesquisaRunnable?.let {
            handlerPesquisa.removeCallbacks(it)
        }
        handlerProximaEtapa.removeCallbacks(atualizadorProximaEtapa)
        handlerRedesenho.removeCallbacks(redesenhoDaLista)
        super.onDestroy()
    }

    /**
     * A tela inicial usa um cinza-escuro no lugar do preto. A janela e as barras do sistema
     * seguem a mesma cor, para não aparecer uma faixa mais escura no topo e no pé.
     */
    private fun definirCorDaJanela(cor: Int) {
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(cor))
        window.statusBarColor = cor
        window.navigationBarColor = cor
    }

    private fun animarEntrada() {
        val tela = findViewById<android.view.View>(android.R.id.content)
        tela.translationX = 80f
        tela.alpha = 0f
        tela.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(280)
            .start()
    }

    private fun mostrarMenu() {
        setContentView(R.layout.activity_main)
        aplicarInsetsDasBarras()
        definirCorDaJanela(getColor(R.color.fundo_tela))
        animarEntrada()

        listaEtapas = findViewById(R.id.listaEtapas)

        criarBotoesCategorias()

        MenuPrincipal.montar(this, findViewById(R.id.containerMenuLateral))
        findViewById<android.view.View>(R.id.btnMenu).setOnClickListener {
            findViewById<DrawerLayout>(R.id.drawerLayout).openDrawer(GravityCompat.START)
        }
        findViewById<android.view.View>(R.id.btnFiltro).setOnClickListener {
            android.widget.Toast.makeText(this, getString(R.string.filtros_em_breve), android.widget.Toast.LENGTH_SHORT).show()
        }
        BarraInferior.montar(this, findViewById(R.id.barraInferior))

        configurarBusca()
        assinaturaDestaques = ""
        atualizarProximaEtapa()

        handlerProximaEtapa.removeCallbacks(atualizadorProximaEtapa)
        handlerProximaEtapa.post(atualizadorProximaEtapa)

        mostrarEtapas(todasEtapas)
    }

    private fun fimDaEtapa(etapa: Etapa): java.time.LocalDate {
        val inicio = parseDataSegura(etapa.dataInicio) ?: java.time.LocalDate.now()
        val fim = parseDataSegura(etapa.dataFim) ?: inicio.plusDays(2)
        return if (fim.isBefore(inicio)) inicio.plusDays(2) else fim
    }

    /**
     * Etapas do carrossel do topo. Se há etapas acontecendo hoje, são todas elas; se não, são
     * todas as que começam na data mais próxima. Assim, duas ou mais etapas no mesmo dia
     * aparecem juntas, uma em cada card, em vez de só uma delas.
     */
    private fun etapasDeDestaque(): List<Etapa> {
        val hoje = java.time.LocalDate.now()

        val abertas = todasEtapas.filter {
            !fimDaEtapa(it).isBefore(hoje) && parseDataSegura(it.dataInicio) != null
        }

        val emAndamento = abertas.filter { parseDataSegura(it.dataInicio)?.isAfter(hoje) == false }

        val grupo = if (emAndamento.isNotEmpty()) {
            emAndamento
        } else {
            val primeiroInicio = abertas.mapNotNull { parseDataSegura(it.dataInicio) }.minOrNull()
                ?: return emptyList()
            abertas.filter { parseDataSegura(it.dataInicio) == primeiroInicio }
        }

        return grupo.sortedWith(
            compareBy<Etapa>(
                { parseDataSegura(it.dataInicio) ?: java.time.LocalDate.MAX },
                { etapa ->
                    Categorias.todas.indexOfFirst {
                        Formatacao.normalizar(it.nome) == Formatacao.normalizar(etapa.categoria)
                    }.let { if (it < 0) Int.MAX_VALUE else it }
                }
            )
        )
    }

    /** Texto do botão do card: HOJE, AMANHÃ, "N DIAS" ou EM ANDAMENTO. */
    private fun textoDoContador(etapa: Etapa, hoje: java.time.LocalDate): String {
        val dataInicio = parseDataSegura(etapa.dataInicio) ?: hoje
        val dias = java.time.temporal.ChronoUnit.DAYS.between(hoje, dataInicio)

        return when {
            dias == 0L -> getString(R.string.contador_hoje)
            dias == 1L -> getString(R.string.contador_amanha)
            dias > 1L -> getString(R.string.contador_dias, dias.toInt())
            else -> getString(R.string.status_em_andamento)
        }
    }

    private fun atualizarProximaEtapa() {
        val painel = findViewById<android.view.View>(R.id.painelProximaEtapa) ?: return
        val container = findViewById<LinearLayout>(R.id.containerDestaques) ?: return
        val carrossel = findViewById<CarrosselDestaque>(R.id.carrosselDestaque) ?: return
        val indicadores = findViewById<LinearLayout>(R.id.indicadoresDestaque) ?: return

        val hoje = java.time.LocalDate.now()
        val destaques = etapasDeDestaque()

        // Fim da temporada: sem próxima etapa o carrossel some, em vez de mostrar dados de exemplo.
        painel.visibility = if (destaques.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        if (destaques.isEmpty()) {
            assinaturaDestaques = ""
            return
        }

        val textos = destaques.map { textoDoContador(it, hoje) }
        val assinatura = destaques.indices.joinToString("#") { i ->
            "${destaques[i].categoria}|${destaques[i].dataInicio}|${destaques[i].circuito}|${textos[i]}"
        } + "|" + Idioma.ingles(this)

        // Nada mudou desde a última vez: mantém os cards e a posição da rolagem.
        if (assinatura == assinaturaDestaques && container.childCount == destaques.size) return
        assinaturaDestaques = assinatura

        val margem = emPx(10f)
        val espaco = emPx(10f)
        val espiada = emPx(24f) // quanto do próximo card aparece à direita

        val larguraTela = findViewById<android.view.View>(R.id.drawerLayout)?.width
            ?.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val varios = destaques.size > 1
        val larguraCard =
            if (varios) larguraTela - margem - espaco - espiada else larguraTela - 2 * margem

        container.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for ((i, etapa) in destaques.withIndex()) {
            val card = inflater.inflate(R.layout.item_destaque, container, false)
            preencherCardDestaque(card, etapa, textos[i])

            container.addView(
                card,
                LinearLayout.LayoutParams(larguraCard, LinearLayout.LayoutParams.MATCH_PARENT)
                    .apply { if (i < destaques.lastIndex) marginEnd = espaco }
            )
        }

        carrossel.passo = larguraCard + espaco
        carrossel.paginas = destaques.size
        carrossel.reiniciar()

        // Bolinhas: só aparecem quando há mais de um card.
        indicadores.removeAllViews()
        indicadores.visibility = if (varios) android.view.View.VISIBLE else android.view.View.GONE

        if (varios) {
            for (i in destaques.indices) {
                indicadores.addView(
                    android.view.View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(emPx(7f), emPx(7f)).apply {
                            marginStart = emPx(4f)
                            marginEnd = emPx(4f)
                        }
                    }
                )
            }
            marcarIndicador(indicadores, 0)
            carrossel.aoMudarPagina = { pagina -> marcarIndicador(indicadores, pagina) }
        } else {
            carrossel.aoMudarPagina = null
        }
    }

    /** Pinta de branco a bolinha do card em foco e de cinza as outras. */
    private fun marcarIndicador(indicadores: LinearLayout, pagina: Int) {
        for (i in 0 until indicadores.childCount) {
            indicadores.getChildAt(i).background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(
                    if (i == pagina) android.graphics.Color.WHITE
                    else android.graphics.Color.parseColor("#3A3D42")
                )
            }
        }
    }

    private fun preencherCardDestaque(card: android.view.View, etapa: Etapa, textoContador: String) {
        val cor = corDaCategoria(etapa.categoria)

        // Fundo: tom escuro da cor da categoria, com bordas arredondadas.
        card.background = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                ColorUtils.blendARGB(getColor(R.color.superficie), cor, 0.22f),
                getColor(R.color.fundo_tela)
            )
        ).apply {
            cornerRadius = emPx(20f).toFloat()
            setStroke(emPx(1f).coerceAtLeast(1), getColor(R.color.borda_suave))
        }
        arredondar(card, 20f)

        // Foto opcional: basta colocar res/drawable/destaque_f1.jpg (ou _wec, _formula_e...).
        val chave = etapa.categoria.lowercase().replace(" ", "_")
        val idFoto = resources.getIdentifier("destaque_$chave", "drawable", packageName)
        if (idFoto != 0) {
            card.findViewById<ImageView>(R.id.destaqueImagem).apply {
                setImageResource(idFoto)
                visibility = android.view.View.VISIBLE
            }
            card.findViewById<android.view.View>(R.id.destaqueVeu).visibility =
                android.view.View.VISIBLE
        }

        // Categoria em destaque: selo preenchido e letreiro gigante translúcido ao fundo
        val rotuloCategoria = Categorias.rotuloDoSelo(etapa.categoria)

        val badge = card.findViewById<TextView>(R.id.destaqueBadge)
        badge.text = rotuloCategoria
        estilizarSeloPreenchido(badge, cor, 9f)

        card.findViewById<TextView>(R.id.destaqueMarca).apply {
            text = rotuloCategoria
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_SP,
                when {
                    rotuloCategoria.length <= 3 -> 110f
                    rotuloCategoria.length <= 6 -> 68f
                    else -> 44f
                }
            )
            setTextColor(android.graphics.Color.argb(
                if (idFoto != 0) 0 else 38,
                android.graphics.Color.red(cor),
                android.graphics.Color.green(cor),
                android.graphics.Color.blue(cor)
            ))
        }

        val nomeDaEtapa = nomeDoEvento(etapa)
        card.findViewById<TextView>(R.id.destaqueNome).text =
            nomeDaEtapa.ifBlank { lugarExibido(etapa.pais) }

        val bandeira = card.findViewById<ImageView>(R.id.destaqueBandeira)
        val idBandeira = recursoBandeira(etapa.pais)
        if (idBandeira != 0) {
            bandeira.setImageResource(idBandeira)
            bandeira.visibility = android.view.View.VISIBLE
            arredondar(bandeira, 4f)
        } else {
            bandeira.visibility = android.view.View.GONE
        }

        card.findViewById<TextView>(R.id.destaqueLocal).text = localDaEtapa(etapa, nomeDaEtapa)
        card.findViewById<TextView>(R.id.destaqueData).text = periodoDoCartao(etapa)

        val botao = card.findViewById<TextView>(R.id.destaqueStatus)
        botao.text = textoContador
        estilizarSeloPreenchido(botao, cor, 10f)

        card.contentDescription =
            "${etapa.categoria}, ${nomeDaEtapa.ifBlank { lugarExibido(etapa.pais) }}, " +
                "${localDaEtapa(etapa, nomeDaEtapa)}, ${periodoDoCartao(etapa)}, $textoContador"

        card.setOnClickListener { abrirEtapa(etapa) }
    }

    /** Abre a tela da etapa, de acordo com a categoria (usado pelo carrossel e pela lista). */
    private fun abrirEtapa(etapa: Etapa) {
        when (etapa.categoria) {
            "F1" -> abrirEventoF1PelaEtapa(etapa)
            "F2" -> abrirEventoF2PelaEtapa(etapa)
            "F3" -> abrirEventoF3PelaEtapa(etapa)
            "Formula E" -> abrirEventoFormulaEPelaEtapa(etapa)
            "IndyCar" -> abrirEventoIndyCarPelaEtapa(etapa)
            "MotoGP", "Moto2", "Moto3", "WEC" -> abrirEventoMotoPelaEtapa(etapa)
        }
    }

    /** Cápsula escura com borda cinza e texto na cor informada (categorias). */
    private fun estilizarCapsula(view: TextView, cor: Int, raioDp: Float) {
        view.setTextColor(cor)
        view.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(getColor(R.color.superficie))
            cornerRadius = emPx(raioDp).toFloat()
            setStroke(emPx(1f).coerceAtLeast(1), getColor(R.color.borda_suave))
        }
    }

    /** Selo só com contorno e texto na cor informada (status das etapas). */
    private fun estilizarContorno(view: TextView, cor: Int) {
        view.setTextColor(cor)
        view.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.TRANSPARENT)
            cornerRadius = emPx(8f).toFloat()
            setStroke(emPx(1f).coerceAtLeast(1), cor)
        }
    }

    /** Selo preenchido na cor da categoria, com texto escuro (selo da categoria e botões). */
    private fun estilizarSeloPreenchido(view: TextView, cor: Int, raioDp: Float) {
        view.setTextColor(android.graphics.Color.parseColor("#0B0D0F"))
        view.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(cor)
            cornerRadius = emPx(raioDp).toFloat()
        }
    }

    private fun arredondar(imagem: android.view.View, raioDp: Float) {
        val raio = emPx(raioDp).toFloat()
        imagem.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, raio)
            }
        }
        imagem.clipToOutline = true
    }

    /** Cor de destaque da categoria; categorias fora da lista usam o verde padrão. */
    private fun corDaCategoria(categoria: String): Int =
        Categorias.info(categoria)?.let { android.graphics.Color.parseColor(it.cor) }
            ?: getColor(R.color.acento)

    private fun criarBotoesCategorias() {
        val container = findViewById<LinearLayout>(R.id.containerCategorias)
        container.removeAllViews()

        for (categoria in Categorias.todas) {
            val botao = TextView(this).apply {
                text = categoria.nome.uppercase()
                textSize = 13f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                letterSpacing = 0.04f
                gravity = android.view.Gravity.CENTER
                maxLines = 1
                minWidth = emPx(52f)
                setPadding(emPx(16f), 0, emPx(16f), 0)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    emPx(38f)
                ).apply { marginEnd = emPx(8f) }
            }

            estilizarCapsula(botao, corDaCategoria(categoria.nome), 12f)
            botao.setOnClickListener { abrirCategoria(categoria.nome) }
            container.addView(botao)
        }
    }

    /** Fundo do card e faixa lateral na cor da categoria, encostada na borda esquerda. */
    private fun estilizarCard(item: android.view.View, cor: Int) {
        val raio = emPx(12f).toFloat()

        item.background = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                ColorUtils.blendARGB(getColor(R.color.superficie), cor, 0.14f),
                getColor(R.color.superficie)
            )
        ).apply {
            cornerRadius = raio
            setStroke(emPx(1f).coerceAtLeast(1), getColor(R.color.borda_suave))
        }

        item.findViewById<android.view.View>(R.id.itemAcento).background =
            android.graphics.drawable.GradientDrawable().apply {
                setColor(cor)
                val r = raio - emPx(1f)
                cornerRadii = floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
            }
    }

    /** Cabeçalho de mês, com uma linha fina separando do grupo anterior. */
    private fun cabecalhoMes(data: java.time.LocalDate, primeiro: Boolean): android.view.View {
        val idiomaApp = Idioma.locale(this)
        val anoAtual = java.time.LocalDate.now().year

        val coluna = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        if (!primeiro) {
            coluna.addView(
                android.view.View(this).apply {
                    setBackgroundColor(getColor(R.color.borda_suave))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        emPx(1f).coerceAtLeast(1)
                    ).apply { topMargin = emPx(10f) }
                }
            )
        }

        coluna.addView(
            TextView(this).apply {
                text = data.month.getDisplayName(java.time.format.TextStyle.FULL, idiomaApp)
                    .uppercase(idiomaApp) + if (data.year != anoAtual) " ${data.year}" else ""
                setTextColor(getColor(R.color.texto_suave))
                textSize = 13f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                letterSpacing = 0.16f
                setPadding(emPx(4f), emPx(if (primeiro) 6f else 16f), 0, emPx(8f))
            }
        )

        return coluna
    }

    private fun configurarBusca() {
        val busca = findViewById<EditText>(R.id.searchEtapa)

        busca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pesquisaRunnable?.let {
                    handlerPesquisa.removeCallbacks(it)
                }

                val texto = Formatacao.normalizar(s?.toString() ?: "")

                pesquisaRunnable = Runnable {
                    if (texto.isEmpty()) {
                        mostrarEtapas(todasEtapas)
                        return@Runnable
                    }

                    val filtradas = todasEtapas.filter {
                        Formatacao.normalizar(it.categoria).contains(texto) ||
                            Formatacao.normalizar(it.pais).contains(texto) ||
                            Formatacao.normalizar(lugarExibido(it.pais)).contains(texto) ||
                            Formatacao.normalizar(it.circuito).contains(texto) ||
                            Formatacao.normalizar(lugarExibido(it.circuito)).contains(texto) ||
                            Formatacao.normalizar(nomeDoEvento(it)).contains(texto) ||
                            Formatacao.normalizar(it.data).contains(texto) ||
                            (Idioma.ingles(this@MainActivity) &&
                                Formatacao.normalizar(datasEmIngles(it)).contains(texto))
                    }

                    mostrarEtapas(filtradas, buscando = true)
                }

                handlerPesquisa.postDelayed(pesquisaRunnable!!, 250)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /** Nome do evento (ex.: "GP da Austrália"), ou vazio quando não há um conhecido. */
    private fun nomeDoEvento(etapa: Etapa): String =
        Idioma.evento(
            this,
            etapa.nomeEvento.ifBlank {
                NomesDeEventos.nome(
                    etapa.categoria, etapa.circuito, etapa.circuitId, etapa.pais
                ).orEmpty()
            }
        )

    /** Datas da etapa em inglês ("Oct 23–25 october"), para a busca por mês funcionar. */
    private fun datasEmIngles(etapa: Etapa): String {
        val inicio = parseDataSegura(etapa.dataInicio)
        val fim = parseDataSegura(etapa.dataFim)

        val meses = listOfNotNull(inicio, fim)
            .map {
                it.month.getDisplayName(
                    java.time.format.TextStyle.FULL,
                    java.util.Locale.ENGLISH
                )
            }
            .distinct()

        return (listOf(Formatacao.periodoEmIngles(inicio, fim).orEmpty()) + meses)
            .joinToString(" ")
    }

    /** País ou cidade no idioma do app (em inglês, traduz os nomes em português). */
    private fun lugarExibido(nome: String): String =
        if (Idioma.ingles(this)) TraducaoDeNomes.lugar(nome) else nome

    /** Período mostrado no cartão de destaque: "23–25 OUT" ou "OCT 23–25". */
    private fun periodoDoCartao(etapa: Etapa): String {
        if (!Idioma.ingles(this)) return etapa.data

        return Formatacao.periodoEmIngles(
            parseDataSegura(etapa.dataInicio),
            parseDataSegura(etapa.dataFim)
        )?.uppercase(java.util.Locale.ENGLISH) ?: etapa.data
    }

    /** Texto do status; os códigos em português continuam sendo usados só na lógica. */
    private fun textoDoStatus(codigo: String): String =
        when (codigo) {
            "FINALIZADA" -> getString(R.string.status_finalizada)
            "PRÓXIMA" -> getString(R.string.status_proxima)
            "EM ANDAMENTO" -> getString(R.string.status_em_andamento)
            else -> getString(R.string.status_agendada)
        }

    /**
     * "Cidade, País". Sem nome de evento conhecido, mantém só o circuito, como antes, porque
     * nesse caso o país é que aparece como título.
     */
    private fun localDaEtapa(etapa: Etapa, nomeEvento: String): String {
        if (nomeEvento.isBlank()) return lugarExibido(etapa.circuito)

        val cidade = etapa.local.ifBlank { etapa.circuito }
        return "${lugarExibido(cidade)}, ${lugarExibido(etapa.pais)}"
    }

    /**
     * [buscando] vale quando a lista é o resultado de uma busca: aí a próxima etapa também
     * entra, senão procurar o país dela mostraria "nenhuma etapa" mesmo havendo resultado.
     */
    private fun mostrarEtapas(etapas: List<Etapa>, buscando: Boolean = false) {
        val hoje = java.time.LocalDate.now()

        val etapasDoCarrossel = etapasDeDestaque()

        val etapasVisiveis =
            if (categoriaAtual.isEmpty()) {
                etapas.filter { etapa ->
                    !fimDaEtapa(etapa).isBefore(hoje) &&
                        (buscando || etapa !in etapasDoCarrossel)
                }
            } else {
                etapas
            }

        val etapasOrdenadas =
            etapasVisiveis.sortedBy {
                parseDataSegura(it.dataInicio) ?: java.time.LocalDate.MAX
            }

        listaEtapas.removeAllViews()
        val inflater = LayoutInflater.from(this)

        if (etapasOrdenadas.isEmpty()) {
            // Ainda lendo os calendários: melhor mostrar nada do que "nenhuma etapa".
            if (carregandoCalendarios) return

            val vazio = TextView(this)
            vazio.text = getString(R.string.lista_vazia)
            vazio.setTextColor(android.graphics.Color.WHITE)
            vazio.textSize = 15f
            vazio.gravity = android.view.Gravity.CENTER
            vazio.setPadding(0, 30, 0, 30)
            listaEtapas.addView(vazio)
            return
        }

        var mesAnterior = -1

        for (etapa in etapasOrdenadas) {
            val inicioItem = parseDataSegura(etapa.dataInicio)
            if (inicioItem != null) {
                val chaveMes = inicioItem.year * 12 + inicioItem.monthValue
                if (chaveMes != mesAnterior) {
                    listaEtapas.addView(cabecalhoMes(inicioItem, mesAnterior == -1))
                    mesAnterior = chaveMes
                }
            }

            val item = inflater.inflate(R.layout.item_etapa, listaEtapas, false)

            val status = statusEtapa(etapa, etapasOrdenadas)

            // Categoria: selo preenchido na cor dela, acima do nome
            val corItem = corDaCategoria(etapa.categoria)
            val chipCategoria = item.findViewById<TextView>(R.id.itemCategoria)
            chipCategoria.text = Categorias.rotuloDoSelo(etapa.categoria)
            estilizarSeloPreenchido(chipCategoria, corItem, 7f)
            estilizarCard(item, corItem)

            val nomeEvento = nomeDoEvento(etapa)
            item.findViewById<TextView>(R.id.itemPais).text = nomeEvento.ifBlank { lugarExibido(etapa.pais) }
            item.findViewById<TextView>(R.id.itemCircuito).text = localDaEtapa(etapa, nomeEvento)

            // DD/MM, sem nome de mês nem ano (já aparecem no cabeçalho da lista)
            val periodo = Formatacao.periodo(
                parseDataSegura(etapa.dataInicio),
                parseDataSegura(etapa.dataFim)
            ) ?: etapa.data
            val periodoExibido = if (Idioma.ingles(this)) {
                Formatacao.periodoEmIngles(
                    parseDataSegura(etapa.dataInicio),
                    parseDataSegura(etapa.dataFim)
                ) ?: periodo
            } else {
                periodo
            }
            val itemData = item.findViewById<TextView>(R.id.itemData)
            itemData.text = periodoExibido

            val itemStatus = item.findViewById<TextView>(R.id.itemStatus)
            itemStatus.text = textoDoStatus(status)

            // Próxima e em andamento: selo preenchido na cor da categoria
            itemData.setTextColor(android.graphics.Color.WHITE)
            when (status) {
                "PRÓXIMA", "EM ANDAMENTO" -> {
                    estilizarSeloPreenchido(itemStatus, corItem, 8f)
                }
                "FINALIZADA" -> {
                    estilizarContorno(itemStatus, getColor(R.color.texto_suave))
                }
                else -> {
                    estilizarContorno(itemStatus, getColor(R.color.status_azul))
                }
            }

            item.contentDescription =
                "${etapa.categoria}, ${nomeEvento.ifBlank { lugarExibido(etapa.pais) }}, " +
                    "${localDaEtapa(etapa, nomeEvento)}, $periodoExibido, ${textoDoStatus(status)}"

            item.setOnClickListener { abrirEtapa(etapa) }

            listaEtapas.addView(item)
        }
    }

    private fun statusEtapa(
        etapa: Etapa,
        etapasOrdenadas: List<Etapa>
    ): String {
        val hoje = java.time.LocalDate.now()
        val inicioEtapa = parseDataSegura(etapa.dataInicio) ?: return "AGENDADA"

        if (fimDaEtapa(etapa).isBefore(hoje)) {
            return "FINALIZADA"
        }

        if (!inicioEtapa.isAfter(hoje)) {
            return "EM ANDAMENTO"
        }

        val proximasDaCategoria = todasEtapas.filter { it.categoria == etapa.categoria }
        val proxima = proximasDaCategoria.firstOrNull { !fimDaEtapa(it).isBefore(hoje) }

        return if (proxima == etapa) "PRÓXIMA" else "AGENDADA"
    }

    private fun recursoBandeira(pais: String): Int {
        val arquivo = when (pais) {
            "Austrália" -> "flag_au"
            "Japão" -> "flag_jp"
            "Bahrein" -> "flag_bh"
            "Arábia Saudita" -> "flag_sa"
            "Estados Unidos" -> "flag_us"
            "Canadá" -> "flag_ca"
            "Mônaco" -> "flag_mc"
            "Espanha" -> "flag_es"
            "Áustria" -> "flag_at"
            "Reino Unido" -> "flag_gb"
            "Bélgica" -> "flag_be"
            "Hungria" -> "flag_hu"
            "Holanda" -> "flag_nl"
            "Itália" -> "flag_it"
            "Azerbaijão" -> "flag_az"
            "Singapura" -> "flag_sg"
            "México" -> "flag_mx"
            "Brasil" -> "flag_br"
            "Catar" -> "flag_qa"
            "China" -> "flag_cn"
            "Alemanha" -> "flag_de"
            "Tailândia" -> "flag_th"
            "França" -> "flag_fr"
            "República Tcheca" -> "flag_cz"
            "Indonésia" -> "flag_id"
            "Malásia" -> "flag_my"
            "San Marino" -> "flag_sm"
            "Portugal" -> "flag_pt"
            "Abu Dhabi", "Emirados Árabes Unidos" -> "flag_ae"
            else -> return 0
        }

        return resources.getIdentifier(arquivo, "drawable", packageName)
    }

    private fun abrirCategoria(categoria: String) {
        categoriaAtual = categoria

        setContentView(R.layout.activity_category)
        aplicarInsetsDasBarras()
        definirCorDaJanela(android.graphics.Color.BLACK)
        animarEntrada()

        val titulo = findViewById<TextView>(R.id.categoryTitle)
        val calendario = findViewById<TextView>(R.id.fullCalendarButton)
        val voltar = findViewById<android.view.View>(R.id.backButton)
        val cartaoCategoria = findViewById<LinearLayout>(R.id.categoryInfoCard)
        val rotuloCategoria = findViewById<TextView>(R.id.categoryEyebrow)
        val tituloCalendario = findViewById<TextView>(R.id.calendarSectionTitle)

        titulo.text = when (categoria) {
            "F1" -> getString(R.string.formula_1)
            "F2" -> getString(R.string.formula_2)
            "F3" -> getString(R.string.formula_3)
            "IndyCar" -> "INDYCAR"
            "MotoGP" -> "MOTOGP"
            "Moto2" -> "MOTO2"
            "Moto3" -> "MOTO3"
            "WEC" -> "WEC"
            else -> getString(R.string.formula_e)
        }

        val rotuloTemporada = findViewById<TextView>(R.id.categorySeasonLabel)
        val anosTemporada = todasEtapas
            .filter { it.categoria == categoria }
            .mapNotNull { parseDataSegura(it.dataInicio)?.year }
            .distinct()
            .sorted()
        val temporadaTexto = when {
            anosTemporada.isEmpty() -> java.time.LocalDate.now().year.toString()
            anosTemporada.size == 1 -> anosTemporada.first().toString()
            else -> anosTemporada.first().toString() + "/" +
                anosTemporada.drop(1).joinToString("/") {
                    (it % 100).toString().padStart(2, '0')
                }
        }
        rotuloTemporada.text = getString(R.string.temporada, temporadaTexto)

        // Todas as categorias usam a identidade visual padrão verde fluorescente.
        val corCategoria = android.graphics.Color.rgb(57, 255, 20)

        findViewById<android.view.View>(R.id.categoryAccentLine)
            .setBackgroundColor(corCategoria)
        rotuloCategoria.setTextColor(corCategoria)
        tituloCalendario.setTextColor(corCategoria)
        cartaoCategoria.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(android.graphics.Color.BLACK)
            cornerRadius = 8f * resources.displayMetrics.density
            setStroke(
                (1 * resources.displayMetrics.density).toInt().coerceAtLeast(1),
                android.graphics.Color.argb(
                    150,
                    android.graphics.Color.red(corCategoria),
                    android.graphics.Color.green(corCategoria),
                    android.graphics.Color.blue(corCategoria)
                )
            )
        }
        calendario.setTextColor(corCategoria)
        calendario.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(android.graphics.Color.BLACK)
            cornerRadius = 14f * resources.displayMetrics.density
            setStroke(
                (2 * resources.displayMetrics.density).toInt().coerceAtLeast(1),
                corCategoria
            )
        }

        voltar.setOnClickListener {
            categoriaAtual = ""
            mostrarMenu()
            overridePendingTransition(
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }

        calendario.setOnClickListener {
            when (categoriaAtual) {
                "F1" -> {
                    startActivity(Intent(this, F1CalendarActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }

                "F2" -> {
                    startActivity(Intent(this, F2CalendarActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }

                "F3" -> {
                    startActivity(Intent(this, F3CalendarActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }

                "Formula E" -> {
                    startActivity(Intent(this, FormulaECalendarActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }

                "IndyCar" -> {
                    startActivity(Intent(this, IndyCarCalendarActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }

                "MotoGP", "Moto2", "Moto3", "WEC" -> {
                    startActivity(
                        Intent(this, MotoCalendarActivity::class.java)
                            .putExtra(ClasseMoto.EXTRA, categoriaAtual)
                    )
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }

                else -> {
                    // Categoria sem calendário.
                }
            }
        }
    }

    private fun abrirEventoF1PelaEtapa(etapa: Etapa) {
        val dataCorrida = fimDaEtapa(etapa).toString()
        val localizacao = JSONObject().apply {
            put("locality", etapa.circuito)
            put("country", etapa.pais)
        }
        val circuito = JSONObject().apply {
            put("circuitId", etapa.circuitId)
            put("circuitName", etapa.circuito)
            put("Location", localizacao)
        }
        val corrida = JSONObject().apply {
            put("season", dataCorrida.take(4))
            put("raceName", etapa.circuito + " Grand Prix")
            put("date", dataCorrida)
            put("Circuit", circuito)
        }

        val intent = Intent(this, F1Activity::class.java)
        intent.putExtra("CIRCUIT_ID_F1", etapa.circuitId)
        intent.putExtra("RACE", corrida.toString())
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /** O início do evento (dd/MM/yyyy) é o mesmo da etapa (yyyy-MM-dd)? Desempata circuitos repetidos. */
    private fun mesmaData(inicio: String, etapa: Etapa): Boolean =
        runCatching {
            java.time.LocalDate.parse(inicio, DateTimeFormatter.ofPattern("dd/MM/uuuu")).toString() == etapa.dataInicio
        }.getOrDefault(false)

    private fun abrirEventoF2PelaEtapa(etapa: Etapa) {
        var encontrado: F2Event? = null

        val doCircuito = F2CalendarJson.carregar(this)
            .filter { it.circuito.equals(etapa.circuito, ignoreCase = true) }
        encontrado = doCircuito.firstOrNull { mesmaData(it.inicio, etapa) } ?: doCircuito.firstOrNull()

        if (encontrado != null) {
            abrirEventoF2Item(encontrado)
        } else {
            abrirCategoria("F2")
        }
    }

    private fun abrirEventoF2Item(evento: F2Event) {
        val intent = Intent(this, F2Activity::class.java)
        intent.putExtra("ETAPA", evento.etapa)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun abrirEventoMotoPelaEtapa(etapa: Etapa) {
        val classe = ClasseMoto.porRotulo(etapa.categoria) ?: return

        val doCircuito = MotoCalendarJson.carregar(this, classe)
            .filter { it.circuito.equals(etapa.circuito, ignoreCase = true) }
        val encontrado = doCircuito.firstOrNull { mesmaData(it.inicio, etapa) } ?: doCircuito.firstOrNull()

        if (encontrado != null) {
            val intent = Intent(this, MotoActivity::class.java)
            intent.putExtra(ClasseMoto.EXTRA, classe.rotulo)
            intent.putExtra("ETAPA", encontrado.etapa)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } else {
            abrirCategoria(classe.rotulo)
        }
    }

    private fun abrirEventoF3PelaEtapa(etapa: Etapa) {
        var encontrado: F3Event? = null

        val doCircuito = F3CalendarJson.carregar(this)
            .filter { it.circuito.equals(etapa.circuito, ignoreCase = true) }
        encontrado = doCircuito.firstOrNull { mesmaData(it.inicio, etapa) } ?: doCircuito.firstOrNull()

        if (encontrado != null) {
            abrirEventoF3Item(encontrado)
        } else {
            abrirCategoria("F3")
        }
    }

    private fun abrirEventoF3Item(evento: F3Event) {
        val intent = Intent(this, F3Activity::class.java)
        intent.putExtra("ETAPA", evento.etapa)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun abrirEventoFormulaEPelaEtapa(etapa: Etapa) {
        var encontrado: FormulaEEvent? = null

        val doCircuito = FormulaECalendarJson.carregar(this)
            .filter { it.circuito.equals(etapa.circuito, ignoreCase = true) }
        encontrado = doCircuito.firstOrNull { mesmaData(it.inicio, etapa) } ?: doCircuito.firstOrNull()

        if (encontrado != null) {
            abrirEventoFormulaEItem(encontrado)
        } else {
            abrirCategoria("Formula E")
        }
    }

    private fun abrirEventoFormulaEItem(evento: FormulaEEvent) {
        val intent = Intent(this, FormulaEActivity::class.java)
        intent.putExtra("ETAPA", evento.etapa)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun abrirEventoIndyCarPelaEtapa(etapa: Etapa) {
        var encontrado: IndyCarEvent? = null
        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        for (item in IndyCarCalendarJson.carregar(this)) {
            try {
                val inicioIso = java.time.LocalDate.parse(item.inicio, formato).toString()
                if (
                    item.circuito.equals(etapa.circuito, ignoreCase = true) &&
                    inicioIso == etapa.dataInicio
                ) {
                    encontrado = item
                    break
                }
            } catch (e: Exception) {
                registrarDescarte("MainActivity.abrirEventoIndyCarPelaEtapa", e)
                // Ignora etapa com data inválida.
            }
        }

        if (encontrado != null) {
            abrirEventoIndyCarItem(encontrado)
        } else {
            abrirCategoria("IndyCar")
        }
    }

    private fun abrirEventoIndyCarItem(evento: IndyCarEvent) {
        val intent = Intent(this, IndyCarActivity::class.java)
        intent.putExtra("ETAPA", evento.etapa)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}