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

    private val etapasFixas = listOf(
        Etapa("F1", "Austrália", "Melbourne", "06–08 MAR", "2026-03-06", "albert_park"),
        Etapa("F1", "China", "Xangai", "13–15 MAR", "2026-03-13", "shanghai"),
        Etapa("F1", "Japão", "Suzuka", "27–29 MAR", "2026-03-27", "suzuka"),
        Etapa("F1", "Estados Unidos", "Miami", "01–03 MAI", "2026-05-01", "miami"),
        Etapa("F1", "Canadá", "Montreal", "22–24 MAI", "2026-05-22", "villeneuve"),
        Etapa("F1", "Mônaco", "Monte Carlo", "05–07 JUN", "2026-06-05", "monaco"),
        Etapa("F1", "Espanha", "Barcelona", "12–14 JUN", "2026-06-12", "catalunya"),
        Etapa("F1", "Áustria", "Spielberg", "26–28 JUN", "2026-06-26", "red_bull_ring"),
        Etapa("F1", "Reino Unido", "Silverstone", "03–05 JUL", "2026-07-03", "silverstone"),
        Etapa("F1", "Bélgica", "Spa-Francorchamps", "17–19 JUL", "2026-07-17", "spa"),
        Etapa("F1", "Hungria", "Budapeste", "24–26 JUL", "2026-07-24", "hungaroring"),
        Etapa("F1", "Holanda", "Zandvoort", "21–23 AGO", "2026-08-21", "zandvoort"),
        Etapa("F1", "Itália", "Monza", "04–06 SET", "2026-09-04", "monza"),
        Etapa("F1", "Espanha", "Madrid", "11–13 SET", "2026-09-11", "madring"),
        Etapa("F1", "Azerbaijão", "Baku", "24–26 SET", "2026-09-24", "baku"),
        Etapa("F1", "Singapura", "Marina Bay", "09–11 OUT", "2026-10-09", "marina_bay"),
        Etapa("F1", "Estados Unidos", "Austin", "23–25 OUT", "2026-10-23", "americas"),
        Etapa("F1", "México", "Cidade do México", "30 OUT–01 NOV", "2026-10-30", "rodriguez"),
        Etapa("F1", "Brasil", "Interlagos", "06–08 NOV", "2026-11-06", "interlagos"),
        Etapa("F1", "Estados Unidos", "Las Vegas", "20–22 NOV", "2026-11-20", "vegas"),
        Etapa("F1", "Catar", "Lusail", "27–29 NOV", "2026-11-27", "losail"),
        Etapa("F1", "Abu Dhabi", "Yas Marina", "04–06 DEZ", "2026-12-04", "yas_marina"),

        Etapa("F2", "Austrália", "Melbourne", "06–08 MAR", "2026-03-06"),
        Etapa("F2", "Mônaco", "Monte Carlo", "04–07 JUN", "2026-06-04"),
        Etapa("F2", "Espanha", "Barcelona", "12–14 JUN", "2026-06-12"),
        Etapa("F2", "Áustria", "Spielberg", "26–28 JUN", "2026-06-26"),
        Etapa("F2", "Reino Unido", "Silverstone", "03–05 JUL", "2026-07-03"),
        Etapa("F2", "Bélgica", "Spa-Francorchamps", "17–19 JUL", "2026-07-17"),
        Etapa("F2", "Hungria", "Budapeste", "24–26 JUL", "2026-07-24"),
        Etapa("F2", "Itália", "Monza", "04–06 SET", "2026-09-04"),
        Etapa("F2", "Espanha", "Madrid", "11–13 SET", "2026-09-11"),
        Etapa("F2", "Catar", "Lusail", "27–29 NOV", "2026-11-27"),
        Etapa("F2", "Abu Dhabi", "Yas Marina", "04–06 DEZ", "2026-12-04"),

        Etapa("F3", "Austrália", "Melbourne", "06–08 MAR", "2026-03-06"),
        Etapa("F3", "Mônaco", "Monte Carlo", "04–07 JUN", "2026-06-04"),
        Etapa("F3", "Espanha", "Barcelona", "12–14 JUN", "2026-06-12"),
        Etapa("F3", "Áustria", "Spielberg", "26–28 JUN", "2026-06-26"),
        Etapa("F3", "Reino Unido", "Silverstone", "03–05 JUL", "2026-07-03"),
        Etapa("F3", "Bélgica", "Spa-Francorchamps", "17–19 JUL", "2026-07-17"),
        Etapa("F3", "Hungria", "Budapeste", "24–26 JUL", "2026-07-24"),
        Etapa("F3", "Itália", "Monza", "04–06 SET", "2026-09-04"),
        Etapa("F3", "Espanha", "Madrid", "11–13 SET", "2026-09-11")
    )

    private var etapasF1Dinamicas: List<Etapa>? = null
    private var etapasF2Dinamicas: List<Etapa>? = null
    private var etapasF3Dinamicas: List<Etapa>? = null
    private var etapasFormulaEDinamicas: List<Etapa>? = null
    private var etapasIndyCarDinamicas: List<Etapa>? = null

    private val todasEtapas: List<Etapa>
        get() =
            (etapasF1Dinamicas ?: etapasFixas.filter { it.categoria == "F1" }) +
                (etapasF2Dinamicas ?: etapasFixas.filter { it.categoria == "F2" }) +
                (etapasF3Dinamicas ?: etapasFixas.filter { it.categoria == "F3" }) +
                (etapasFormulaEDinamicas ?: emptyList()) +
                (etapasIndyCarDinamicas ?: emptyList()) +
                etapasFixas.filter {
                    it.categoria != "F1" &&
                        it.categoria != "F2" &&
                        it.categoria != "F3" &&
                        it.categoria != "Formula E" &&
                        it.categoria != "IndyCar"
                }

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
            etapasF2Dinamicas = F2CalendarJson.comoEtapas(F2CalendarJson.carregar(this))
            if (categoriaAtual.isEmpty()) {
                atualizarProximaEtapa()
                mostrarEtapas(todasEtapas)
            }
        }

        etapasF3Dinamicas = F3CalendarJson.comoEtapas(F3CalendarJson.carregar(this))
        F3CalendarJson.atualizar(this) {
            etapasF3Dinamicas = F3CalendarJson.comoEtapas(F3CalendarJson.carregar(this))
            if (categoriaAtual.isEmpty()) {
                atualizarProximaEtapa()
                mostrarEtapas(todasEtapas)
            }
        }

        etapasIndyCarDinamicas = IndyCarCalendarJson.comoEtapas(IndyCarCalendarJson.carregar(this))
        IndyCarCalendarJson.atualizar(this) {
            etapasIndyCarDinamicas = IndyCarCalendarJson.comoEtapas(IndyCarCalendarJson.carregar(this))
            if (categoriaAtual.isEmpty()) {
                atualizarProximaEtapa()
                mostrarEtapas(todasEtapas)
            }
        }

        etapasFormulaEDinamicas = FormulaECalendarJson.comoEtapas(FormulaECalendarJson.carregar(this))
        FormulaECalendarJson.atualizar(this) {
            etapasFormulaEDinamicas = FormulaECalendarJson.comoEtapas(FormulaECalendarJson.carregar(this))
            if (categoriaAtual.isEmpty()) {
                atualizarProximaEtapa()
                mostrarEtapas(todasEtapas)
            }
        }

        etapasF1Dinamicas = F1CalendarApi.carregarCache(this)
        F1CalendarApi.atualizar(this) {
            etapasF1Dinamicas = F1CalendarApi.carregarCache(this)
            if (categoriaAtual.isEmpty()) {
                atualizarProximaEtapa()
                mostrarEtapas(todasEtapas)
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
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

    override fun onDestroy() {
        pesquisaRunnable?.let {
            handlerPesquisa.removeCallbacks(it)
        }
        handlerProximaEtapa.removeCallbacks(atualizadorProximaEtapa)
        super.onDestroy()
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
        animarEntrada()

        listaEtapas = findViewById(R.id.listaEtapas)

        criarBotoesCategorias()

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

        titulo.text = "PRÓXIMA ETAPA • ${proxima.categoria}"
        nome.text = proxima.pais

        val idBandeira = recursoBandeira(proxima.pais)
        if (idBandeira != 0) {
            bandeira.setImageResource(idBandeira)
            bandeira.visibility = android.view.View.VISIBLE
        } else {
            bandeira.visibility = android.view.View.GONE
        }

        circuito.text = proxima.circuito
        data.text = proxima.data

        val dataInicio = parseDataSegura(proxima.dataInicio) ?: hoje
        val dias = java.time.temporal.ChronoUnit.DAYS.between(hoje, dataInicio)

        contador.text = when {
            dias == 0L -> "COMEÇA HOJE"
            dias == 1L -> "AMANHÃ"
            dias > 1L -> "FALTAM $dias DIAS"
            else -> "EM ANDAMENTO"
        }
        estilizarSelo(contador, getColor(R.color.verde_neon))

        findViewById<android.view.View>(R.id.painelProximaEtapa).setOnClickListener {
            when (proxima.categoria) {
                "F1" -> abrirEventoF1PelaEtapa(proxima)
                "F2" -> abrirEventoF2PelaEtapa(proxima)
                "F3" -> abrirEventoF3PelaEtapa(proxima)
                "Formula E" -> abrirEventoFormulaEPelaEtapa(proxima)
                "IndyCar" -> abrirEventoIndyCarPelaEtapa(proxima)
            }
        }
    }

    /** Selo com borda e fundo suave na cor informada (categoria e contagem regressiva). */
    private fun estilizarSelo(view: TextView, cor: Int, raioDp: Float = 10f) {
        val densidade = resources.displayMetrics.density
        view.setTextColor(cor)
        view.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(
                android.graphics.Color.argb(
                    45,
                    android.graphics.Color.red(cor),
                    android.graphics.Color.green(cor),
                    android.graphics.Color.blue(cor)
                )
            )
            cornerRadius = raioDp * densidade
            setStroke((1 * densidade).toInt().coerceAtLeast(1), cor)
        }
    }

    /** Separa "30 OUT–01 NOV" em ("30–01", "OUT–NOV") e "06–08 NOV" em ("06–08", "NOV"). */
    private fun partirData(data: String): Pair<String, String> {
        val limpa = data.trim().uppercase()

        Regex("^(\\d{1,2})\\s+(\\p{L}{3,4})\\s*[–—-]\\s*(\\d{1,2})\\s+(\\p{L}{3,4})$")
            .matchEntire(limpa)?.let {
                val g = it.groupValues
                return "${g[1]}–${g[3]}" to "${g[2]}–${g[4]}"
            }

        Regex("^(\\d{1,2})\\s*[–—-]\\s*(\\d{1,2})\\s+(\\p{L}{3,4})$")
            .matchEntire(limpa)?.let {
                val g = it.groupValues
                return "${g[1]}–${g[2]}" to g[3]
            }

        Regex("^(\\d{1,2})\\s+(\\p{L}{3,4})$")
            .matchEntire(limpa)?.let {
                val g = it.groupValues
                return g[1] to g[2]
            }

        return limpa to ""
    }

    private data class CategoriaInfo(
        val nome: String,
        val cor: String,
        /** Texto curto para o selo dos cards (opcional), útil para nomes muito longos. */
        val abreviacao: String? = null
    )

    /**
     * Lista central de categorias. Para adicionar uma nova, basta incluir uma linha aqui:
     * o botão do topo, o selo dos cards e a cor passam a existir automaticamente.
     * O nome pode ser longo: os botões rolam na horizontal e o selo reduz a fonte para caber.
     */
    private val categoriasApp = listOf(
        CategoriaInfo("F1", "#FF3B30"),
        CategoriaInfo("F2", "#3D9BFF"),
        CategoriaInfo("F3", "#FF8A2B"),
        CategoriaInfo("IndyCar", "#A58BFF"),
        CategoriaInfo("Formula E", "#22D3EE")
    )

    /** Minúsculas e sem acentos: "Fórmula E" e "formula e" são tratados como iguais. */
    private fun normalizar(texto: String): String =
        java.text.Normalizer.normalize(texto.trim(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()

    private fun infoDaCategoria(categoria: String): CategoriaInfo? =
        categoriasApp.firstOrNull { normalizar(it.nome) == normalizar(categoria) }

    /** Cor de destaque da categoria; categorias fora da lista usam o verde padrão. */
    private fun corDaCategoria(categoria: String): Int =
        infoDaCategoria(categoria)?.let { android.graphics.Color.parseColor(it.cor) }
            ?: getColor(R.color.verde_neon)

    /** Texto do selo nos cards: abreviação, quando existir, ou o nome completo. */
    private fun rotuloDoSelo(categoria: String): String =
        (infoDaCategoria(categoria)?.abreviacao ?: categoria).uppercase()

    private fun criarBotoesCategorias() {
        val container = findViewById<LinearLayout>(R.id.containerCategorias)
        val densidade = resources.displayMetrics.density
        container.removeAllViews()

        for (categoria in categoriasApp) {
            val botao = TextView(this).apply {
                text = categoria.nome
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = android.graphics.Typeface.create(
                    "sans-serif-condensed",
                    android.graphics.Typeface.BOLD
                )
                gravity = android.view.Gravity.CENTER
                maxLines = 1
                minWidth = (62 * densidade).toInt()
                setPadding((16 * densidade).toInt(), 0, (16 * densidade).toInt(), 0)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (40 * densidade).toInt()
                ).apply { marginEnd = (8 * densidade).toInt() }
            }

            estilizarSelo(botao, corDaCategoria(categoria.nome), 14f)
            botao.setOnClickListener { abrirCategoria(categoria.nome) }
            container.addView(botao)
        }
    }

    /** Fundo em degradê, borda sutil e marcador lateral (fora do card) na cor da categoria. */
    private fun estilizarCard(item: android.view.View, cor: Int) {
        val densidade = resources.displayMetrics.density

        item.findViewById<android.view.View>(R.id.itemCartao).background = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                android.graphics.Color.parseColor("#0D1B0E"),
                android.graphics.Color.parseColor("#050A06")
            )
        ).apply {
            cornerRadius = 16f * densidade
            setStroke(
                (1 * densidade).toInt().coerceAtLeast(1),
                android.graphics.Color.argb(
                    72,
                    android.graphics.Color.red(cor),
                    android.graphics.Color.green(cor),
                    android.graphics.Color.blue(cor)
                )
            )
        }

        item.findViewById<android.view.View>(R.id.itemAcento).background =
            android.graphics.drawable.GradientDrawable().apply {
                setColor(cor)
                // arredondado só por fora; o lado que encosta no card fica reto
                val raio = 3f * densidade
                cornerRadii = floatArrayOf(raio, raio, 0f, 0f, 0f, 0f, raio, raio)
            }
    }

    /** Cabeçalho de mês: nome espaçado e uma linha fina até o fim. */
    private fun cabecalhoMes(data: java.time.LocalDate, primeiro: Boolean): android.view.View {
        val nomes = listOf(
            "JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO",
            "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"
        )
        val densidade = resources.displayMetrics.density
        val anoAtual = java.time.LocalDate.now().year

        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(
                (8 * densidade).toInt(),
                ((if (primeiro) 2 else 14) * densidade).toInt(),
                (4 * densidade).toInt(),
                (8 * densidade).toInt()
            )
        }

        val titulo = TextView(this).apply {
            text = nomes[data.monthValue - 1] + if (data.year != anoAtual) " ${data.year}" else ""
            setTextColor(android.graphics.Color.parseColor("#7F8B97"))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = android.graphics.Typeface.create(
                "sans-serif-condensed",
                android.graphics.Typeface.BOLD
            )
            letterSpacing = 0.18f
        }

        val traco = android.view.View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#142414"))
            layoutParams = LinearLayout.LayoutParams(
                0,
                (1 * densidade).toInt().coerceAtLeast(1),
                1f
            ).apply { marginStart = (10 * densidade).toInt() }
        }

        linha.addView(titulo)
        linha.addView(traco)
        return linha
    }

    /** Mantém as colunas fixas (alinhadas), mas proporcionais à fonte do sistema. */
    private fun ajustarLargura(view: android.view.View, baseDp: Float, escala: Float) {
        val parametros = view.layoutParams
        parametros.width = (baseDp * escala * resources.displayMetrics.density).toInt()
        view.layoutParams = parametros
    }

    private fun configurarBusca() {
        val busca = findViewById<EditText>(R.id.searchEtapa)

        busca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                pesquisaRunnable?.let {
                    handlerPesquisa.removeCallbacks(it)
                }

                val texto = normalizar(s?.toString() ?: "")

                pesquisaRunnable = Runnable {
                    if (texto.isEmpty()) {
                        mostrarEtapas(todasEtapas)
                        return@Runnable
                    }

                    val filtradas = todasEtapas.filter {
                        normalizar(it.categoria).contains(texto) ||
                            normalizar(it.pais).contains(texto) ||
                            normalizar(it.circuito).contains(texto) ||
                            normalizar(it.data).contains(texto)
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

            // Só a coluna da data acompanha a fonte do sistema (até 10%); o resto se ajusta sozinho
            val escalaFonte = resources.configuration.fontScale.coerceIn(1f, 1.1f)
            if (escalaFonte > 1f) {
                ajustarLargura(item.findViewById(R.id.colunaData), 66f, escalaFonte)
            }

            val status = statusEtapa(etapa, etapasOrdenadas)

            val chipCategoria = item.findViewById<TextView>(R.id.itemCategoria)
            chipCategoria.text = rotuloDoSelo(etapa.categoria)
            estilizarSelo(chipCategoria, corDaCategoria(etapa.categoria))
            estilizarCard(item, corDaCategoria(etapa.categoria))

            val imagemBandeira = item.findViewById<ImageView>(R.id.itemBandeira)
            val idBandeira = recursoBandeira(etapa.pais)

            if (idBandeira != 0) {
                imagemBandeira.setImageResource(idBandeira)
                imagemBandeira.visibility = android.view.View.VISIBLE

                val raioBandeira = 5f * resources.displayMetrics.density
                imagemBandeira.outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, raioBandeira)
                    }
                }
                imagemBandeira.clipToOutline = true
            } else {
                // INVISIBLE mantém o espaço da bandeira e o texto continua alinhado
                imagemBandeira.visibility = android.view.View.INVISIBLE
            }

            item.findViewById<TextView>(R.id.itemPais).text = etapa.pais
            item.findViewById<TextView>(R.id.itemCircuito).text = etapa.circuito

            val (dias, mes) = partirData(etapa.data)
            item.findViewById<TextView>(R.id.itemDia).text = dias
            val itemMes = item.findViewById<TextView>(R.id.itemMes)
            itemMes.text = mes
            itemMes.visibility =
                if (mes.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

            val itemStatus = item.findViewById<TextView>(R.id.itemStatus)
            itemStatus.text = status

            when (status) {
                "FINALIZADA" -> {
                    itemStatus.setTextColor(android.graphics.Color.WHITE)
                    itemStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.rgb(90, 100, 110)
                    )
                }

                "PRÓXIMA", "EM ANDAMENTO" -> {
                    itemStatus.setTextColor(android.graphics.Color.WHITE)
                    itemStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.rgb(25, 150, 95)
                    )
                }

                "AGENDADA" -> {
                    itemStatus.setTextColor(android.graphics.Color.WHITE)
                    itemStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.rgb(30, 70, 105)
                    )
                }
            }

            item.contentDescription = status

            item.setOnClickListener {
                when (etapa.categoria) {
                    "F1" -> abrirEventoF1PelaEtapa(etapa)
                    "F2" -> abrirEventoF2PelaEtapa(etapa)
                    "F3" -> abrirEventoF3PelaEtapa(etapa)
                    "Formula E" -> abrirEventoFormulaEPelaEtapa(etapa)
                    "IndyCar" -> abrirEventoIndyCarPelaEtapa(etapa)
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
            "Abu Dhabi", "Emirados Árabes Unidos" -> "flag_ae"
            else -> return 0
        }

        return resources.getIdentifier(arquivo, "drawable", packageName)
    }

    private fun abrirCategoria(categoria: String) {
        categoriaAtual = categoria

        setContentView(R.layout.activity_category)
        aplicarInsetsDasBarras()
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
            else -> "FÓRMULA E"
        }

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
