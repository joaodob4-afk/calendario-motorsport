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
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import org.json.JSONObject
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private var categoriaAtual = ""

    data class Etapa(
        val categoria: String,
        val pais: String,
        val circuito: String,
        val data: String,
        val dataInicio: String = "",
        val circuitId: String = "",
        val dataFim: String = ""
    )

    private lateinit var listaEtapas: LinearLayout

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

        etapasF2Dinamicas = F2CalendarJson.comoEtapas(F2CalendarJson.carregar(this))
        F2CalendarJson.atualizar(this) {
            AgendadorDeAvisos.reagendarEmSegundoPlano(this)
            etapasF2Dinamicas = F2CalendarJson.comoEtapas(F2CalendarJson.carregar(this))
            redesenharListaDepois()
        }

        etapasF3Dinamicas = F3CalendarJson.comoEtapas(F3CalendarJson.carregar(this))
        F3CalendarJson.atualizar(this) {
            AgendadorDeAvisos.reagendarEmSegundoPlano(this)
            etapasF3Dinamicas = F3CalendarJson.comoEtapas(F3CalendarJson.carregar(this))
            redesenharListaDepois()
        }

        etapasIndyCarDinamicas = IndyCarCalendarJson.comoEtapas(IndyCarCalendarJson.carregar(this))
        IndyCarCalendarJson.atualizar(this) {
            AgendadorDeAvisos.reagendarEmSegundoPlano(this)
            etapasIndyCarDinamicas = IndyCarCalendarJson.comoEtapas(IndyCarCalendarJson.carregar(this))
            redesenharListaDepois()
        }

        etapasFormulaEDinamicas = FormulaECalendarJson.comoEtapas(FormulaECalendarJson.carregar(this))
        FormulaECalendarJson.atualizar(this) {
            AgendadorDeAvisos.reagendarEmSegundoPlano(this)
            etapasFormulaEDinamicas = FormulaECalendarJson.comoEtapas(FormulaECalendarJson.carregar(this))
            redesenharListaDepois()
        }

        // MotoGP, Moto2 e Moto3 usam as mesmas telas; cada classe tem o seu calendário.
        etapasMotoDinamicas = MotoCalendarJson.etapasDeTodas(this)
        for (classe in ClasseMoto.values()) {
            MotoCalendarJson.atualizar(this, classe) {
                AgendadorDeAvisos.reagendarEmSegundoPlano(this)
                etapasMotoDinamicas = MotoCalendarJson.etapasDeTodas(this)
                redesenharListaDepois()
            }
        }

        etapasF1Dinamicas = F1CalendarApi.carregarCache(this)
        F1CalendarApi.atualizar(this) {
            AgendadorDeAvisos.reagendarEmSegundoPlano(this)
            etapasF1Dinamicas = F1CalendarApi.carregarCache(this)
            redesenharListaDepois()
        }

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
            android.widget.Toast.makeText(this, "Filtros: em breve", android.widget.Toast.LENGTH_SHORT).show()
        }
        BarraInferior.montar(this, findViewById(R.id.barraInferior))

        configurarBusca()
        atualizarProximaEtapa()

        handlerProximaEtapa.removeCallbacks(atualizadorProximaEtapa)
        handlerProximaEtapa.post(atualizadorProximaEtapa)

        mostrarEtapas(todasEtapas)
    }

    private fun fimDaEtapa(etapa: Etapa): java.time.LocalDate {
        val inicio = parseDataSegura(etapa.dataInicio) ?: java.time.LocalDate.now()
        return try {
            val fim = parseDataSegura(etapa.dataFim) ?: inicio.plusDays(2)
            if (fim.isBefore(inicio)) inicio.plusDays(2) else fim
        } catch (e: Exception) {
            registrarDescarte("MainActivity.fimDaEtapa", e)
            inicio.plusDays(2)
        }
    }

    private fun atualizarProximaEtapa() {
        val hoje = java.time.LocalDate.now()

        val proxima =
            todasEtapas
                .filter { !fimDaEtapa(it).isBefore(hoje) }
                .minByOrNull {
                    parseDataSegura(it.dataInicio) ?: java.time.LocalDate.MAX
                }

        if (proxima == null) return

        val titulo = findViewById<TextView>(R.id.tituloProximaEtapa)
        val nome = findViewById<TextView>(R.id.nomeProximaEtapa)
        val bandeira = findViewById<ImageView>(R.id.bandeiraProximaEtapa)
        val circuito = findViewById<TextView>(R.id.circuitoProximaEtapa)
        val data = findViewById<TextView>(R.id.dataProximaEtapa)
        val contador = findViewById<TextView>(R.id.contadorProximaEtapa)

        val prefixo = "PRÓXIMA ETAPA  •  "
        val textoTitulo = android.text.SpannableString(prefixo + proxima.categoria)
        textoTitulo.setSpan(
            android.text.style.ForegroundColorSpan(getColor(R.color.acento)),
            prefixo.length,
            textoTitulo.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        titulo.text = textoTitulo
        nome.text = proxima.pais

        val idBandeira = recursoBandeira(proxima.pais)
        if (idBandeira != 0) {
            bandeira.setImageResource(idBandeira)
            bandeira.visibility = android.view.View.VISIBLE
            arredondar(bandeira, 6f)
        } else {
            bandeira.visibility = android.view.View.GONE
        }

        circuito.text = proxima.circuito
        data.text = proxima.data

        val dataInicio = parseDataSegura(proxima.dataInicio) ?: hoje
        val dias = java.time.temporal.ChronoUnit.DAYS.between(hoje, dataInicio)

        contador.text = when {
            dias == 0L -> "HOJE"
            dias == 1L -> "AMANHÃ"
            dias > 1L -> "$dias DIAS"
            else -> "EM ANDAMENTO"
        }
        estilizarChipDestaque(contador)

        findViewById<android.view.View>(R.id.painelProximaEtapa).setOnClickListener {
            when (proxima.categoria) {
                "F1" -> abrirEventoF1PelaEtapa(proxima)
                "F2" -> abrirEventoF2PelaEtapa(proxima)
                "F3" -> abrirEventoF3PelaEtapa(proxima)
                "Formula E" -> abrirEventoFormulaEPelaEtapa(proxima)
                "IndyCar" -> abrirEventoIndyCarPelaEtapa(proxima)
                "MotoGP", "Moto2", "Moto3" -> abrirEventoMotoPelaEtapa(proxima)
            }
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

    /** Selo preenchido em verde (contagem regressiva da próxima etapa). */
    private fun estilizarChipDestaque(view: TextView) {
        view.setTextColor(getColor(R.color.acento_escuro))
        view.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(getColor(R.color.acento))
            cornerRadius = emPx(8f).toFloat()
        }
    }

    private fun arredondar(imagem: ImageView, raioDp: Float) {
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

        item.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(getColor(R.color.superficie))
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
        val nomes = listOf(
            "JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO",
            "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"
        )
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
                text = nomes[data.monthValue - 1] + if (data.year != anoAtual) " ${data.year}" else ""
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
                            Formatacao.normalizar(it.circuito).contains(texto) ||
                            Formatacao.normalizar(it.data).contains(texto)
                    }

                    mostrarEtapas(filtradas)
                }

                handlerPesquisa.postDelayed(pesquisaRunnable!!, 250)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun mostrarEtapas(etapas: List<Etapa>) {
        val hoje = java.time.LocalDate.now()

        val proximaEtapa =
            todasEtapas
                .filter { !fimDaEtapa(it).isBefore(hoje) }
                .minByOrNull {
                    parseDataSegura(it.dataInicio) ?: java.time.LocalDate.MAX
                }

        val etapasVisiveis =
            if (categoriaAtual.isEmpty()) {
                etapas.filter { etapa ->
                    !fimDaEtapa(etapa).isBefore(hoje) &&
                        etapa != proximaEtapa
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
            val vazio = TextView(this)
            vazio.text = "Nenhuma etapa encontrada."
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

            val chipCategoria = item.findViewById<TextView>(R.id.itemCategoria)
            chipCategoria.text = Categorias.rotuloDoSelo(etapa.categoria)
            estilizarCapsula(chipCategoria, corDaCategoria(etapa.categoria), 8f)
            estilizarCard(item, corDaCategoria(etapa.categoria))

            val imagemBandeira = item.findViewById<ImageView>(R.id.itemBandeira)
            val idBandeira = recursoBandeira(etapa.pais)

            if (idBandeira != 0) {
                imagemBandeira.setImageResource(idBandeira)
                imagemBandeira.visibility = android.view.View.VISIBLE

                arredondar(imagemBandeira, 6f)
            } else {
                // INVISIBLE mantém o espaço da bandeira e o texto continua alinhado
                imagemBandeira.visibility = android.view.View.INVISIBLE
            }

            item.findViewById<TextView>(R.id.itemPais).text = etapa.pais
            item.findViewById<TextView>(R.id.itemCircuito).text = etapa.circuito

            // DD/MM, sem nome de mês nem ano (já aparecem no cabeçalho da lista)
            val periodo = Formatacao.periodo(
                parseDataSegura(etapa.dataInicio),
                parseDataSegura(etapa.dataFim)
            ) ?: etapa.data
            item.findViewById<TextView>(R.id.itemData).text = periodo

            val itemStatus = item.findViewById<TextView>(R.id.itemStatus)
            itemStatus.text = status

            val corStatus = when (status) {
                "FINALIZADA" -> getColor(R.color.texto_suave)
                "PRÓXIMA", "EM ANDAMENTO" -> getColor(R.color.acento)
                else -> getColor(R.color.status_azul)
            }
            estilizarContorno(itemStatus, corStatus)

            item.contentDescription =
                "${etapa.categoria}, ${etapa.pais}, ${etapa.circuito}, $periodo, $status"

            item.setOnClickListener {
                when (etapa.categoria) {
                    "F1" -> abrirEventoF1PelaEtapa(etapa)
                    "F2" -> abrirEventoF2PelaEtapa(etapa)
                    "F3" -> abrirEventoF3PelaEtapa(etapa)
                    "Formula E" -> abrirEventoFormulaEPelaEtapa(etapa)
                    "IndyCar" -> abrirEventoIndyCarPelaEtapa(etapa)
                    "MotoGP", "Moto2", "Moto3" -> abrirEventoMotoPelaEtapa(etapa)
                }
            }

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
            "F1" -> "FÓRMULA 1"
            "F2" -> "FÓRMULA 2"
            "F3" -> "FÓRMULA 3"
            "IndyCar" -> "INDYCAR"
            "MotoGP" -> "MOTOGP"
            "Moto2" -> "MOTO2"
            "Moto3" -> "MOTO3"
            else -> "FÓRMULA E"
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
        rotuloTemporada.text = "Temporada $temporadaTexto"

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

                "MotoGP", "Moto2", "Moto3" -> {
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

    private fun abrirEventoF2PelaEtapa(etapa: Etapa) {
        var encontrado: F2Event? = null

        for (item in F2CalendarJson.carregar(this)) {
            if (item.circuito.equals(etapa.circuito, ignoreCase = true)) {
                encontrado = item
                break
            }
        }

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

        val encontrado = MotoCalendarJson.carregar(this, classe)
            .firstOrNull { it.circuito.equals(etapa.circuito, ignoreCase = true) }

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

        for (item in F3CalendarJson.carregar(this)) {
            if (item.circuito.equals(etapa.circuito, ignoreCase = true)) {
                encontrado = item
                break
            }
        }

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

        for (item in FormulaECalendarJson.carregar(this)) {
            if (
                item.circuito.equals(etapa.circuito, ignoreCase = true) &&
                item.inicio.takeLast(4) == etapa.dataInicio.take(4)
            ) {
                encontrado = item
                break
            }
        }

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
