package com.motorsport.calendario

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
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
        const val CHAVE_FILTRO_CATEGORIA = "filtro_categoria"
        const val CHAVE_FILTRO_FINALIZADAS = "filtro_finalizadas"
        const val CHAVE_FILTRO_PAISES = "filtro_paises"
        const val CHAVE_FILTRO_MESES = "filtro_meses"

        /** Cards criados por vez ao desenhar a lista; o resto entra nos quadros seguintes. */
        const val ETAPAS_POR_LOTE = 8
    }

    /** Diferente de vazio enquanto a tela de detalhe de uma categoria está aberta. */
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
        val local: String = "",
        /**
         * Instante (ms) a partir do qual a etapa já terminou, calculado pelo horário da última
         * sessão (ver [RegrasDeFiltro.comFimPorHorario]). 0 = desconhecido: vale só o último dia.
         */
        val terminaEmMs: Long = 0L
    )

    private lateinit var listaEtapas: LinearLayout

    /** Categorias favoritas e finalizadas escolhidas (painel do coração e interruptor da tela inicial). */
    private var filtros = FiltrosDeEtapas()

    /** Painel que abre pela direita (categorias favoritas); criado em [mostrarMenu]. */
    private var menuDeFiltros: MenuDeFiltros? = null

    /** Cabeçalhos de mês da lista atual; o primeiro que já passou do topo vira o mês flutuante. */
    private val cabecalhosDeMes = mutableListOf<View>()

    private var botaoTopoVisivel = false

    /** Primeira sessão (instante exato) de cada etapa da lista atual; dá o horário e o dia do card. */
    private var primeirasSessoesDaLista: Map<Etapa, java.time.Instant> = emptyMap()

    /** Última sessão (instante exato) de cada etapa da lista atual; dá a linha de baixo da data. */
    private var ultimasSessoesDaLista: Map<Etapa, java.time.Instant> = emptyMap()

    /** Tema (claro/escuro) com que a tela foi montada; se mudar enquanto ela está parada, ela se refaz. */
    private var modoNoturnoDaTela = false

    /**
     * A tela trata a troca de tema sozinha (uiMode no manifesto), em vez de ser recriada: o
     * Android mostraria um fundo branco no meio da recriação. Aqui as views são refeitas no
     * lugar, com as cores novas, sem fechar a janela. Filtros e categoria escolhidos continuam.
     */
    override fun onConfigurationChanged(novaConfiguracao: android.content.res.Configuration) {
        super.onConfigurationChanged(novaConfiguracao)
        atualizarParaOTema()
    }

    private fun atualizarParaOTema() {
        val atual = modoNoturnoAtual()
        if (atual == modoNoturnoDaTela) return
        modoNoturnoDaTela = atual

        // Na tela de detalhe da categoria (sempre escura) não há nada a refazer; a tela inicial
        // é montada de novo, já no tema certo, quando o usuário voltar a ela.
        if (categoriaAtual.isEmpty()) {
            mostrarMenu(animar = false)
        }
    }

    private fun modoNoturnoAtual(): Boolean =
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    /** Sessões com horário confirmado de todas as categorias; definem quem começa mais cedo. */
    private var sessoes: List<SessaoParaAviso> = emptyList()

    /** Muda a cada desenho da lista; lotes de um desenho antigo se cancelam ao ver que mudou. */
    private var geracaoDaLista = 0

    /**
     * Primeira linha da lista que não é de etapa finalizada: o "topo de verdade". Com as
     * finalizadas ligadas elas ficam acima dele, e é daqui que a lista começa e para onde o
     * botão de subir leva.
     */
    private var ancoraDoTopo: View? = null

    private class LinhaDaLista(val etapa: Etapa?, val mes: java.time.LocalDate?)

    /** Dia em que a lista foi desenhada pela última vez; se o dia mudar, ela é refeita. */
    private var dataDaUltimaLista: java.time.LocalDate? = null

    /** Quando a lista foi desenhada pela última vez; etapas também terminam no meio do dia. */
    private var instanteDaUltimaLista = 0L

    private val handlerRedesenho = Handler(Looper.getMainLooper())

    private val redesenhoDaLista = Runnable {
        if (categoriaAtual.isEmpty()) {
            mostrarEtapas()
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

    /** Verdadeiro enquanto uma recarga única da Moto está esperando para rodar. */
    private var recargaMotoAgendada = false

    private var etapasF1Dinamicas: List<Etapa>? = null
    private var etapasF2Dinamicas: List<Etapa>? = null
    private var etapasF3Dinamicas: List<Etapa>? = null
    private var etapasF1AcademyDinamicas: List<Etapa>? = null
    private var etapasFormulaEDinamicas: List<Etapa>? = null
    private var etapasIndyCarDinamicas: List<Etapa>? = null
    private var etapasMotoDinamicas: List<Etapa>? = null

    /** Resultado de [todasEtapas] e as listas de onde ele saiu: só é refeito quando alguma muda. */
    private var cacheDeTodasEtapas: List<Etapa>? = null
    private var origemDoCache: List<Any?> = emptyList()

    /**
     * Todas as etapas, com o fim de cada uma ajustado pelas sessões. É consultada várias vezes a
     * cada mudança de filtro; calcular de novo toda vez causava travadas, então fica em cache.
     */
    private val todasEtapas: List<Etapa>
        get() {
            val origem = listOf(
                etapasF1Dinamicas, etapasF2Dinamicas, etapasF3Dinamicas, etapasF1AcademyDinamicas,
                etapasFormulaEDinamicas, etapasIndyCarDinamicas, etapasMotoDinamicas, sessoes
            )
            val guardado = cacheDeTodasEtapas

            if (guardado != null && origem.indices.all { origem[it] === origemDoCache[it] }) {
                return guardado
            }

            val novas = RegrasDeFiltro.comFimPorHorario(
                (etapasF1Dinamicas ?: emptyList()) +
                    (etapasF2Dinamicas ?: emptyList()) +
                    (etapasF3Dinamicas ?: emptyList()) +
                    (etapasF1AcademyDinamicas ?: emptyList()) +
                    (etapasFormulaEDinamicas ?: emptyList()) +
                    (etapasIndyCarDinamicas ?: emptyList()) +
                    (etapasMotoDinamicas ?: emptyList()),
                sessoes
            )

            cacheDeTodasEtapas = novas
            origemDoCache = origem
            return novas
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

        modoNoturnoDaTela = modoNoturnoAtual()

        savedInstanceState?.let {
            filtros = FiltrosDeEtapas(
                categoria = it.getString(CHAVE_FILTRO_CATEGORIA).orEmpty(),
                mostrarFinalizadas = it.getBoolean(CHAVE_FILTRO_FINALIZADAS),
                paises = it.getStringArrayList(CHAVE_FILTRO_PAISES)?.toSet().orEmpty(),
                meses = it.getStringArrayList(CHAVE_FILTRO_MESES)?.toSet().orEmpty()
            )
        }

        // As categorias favoritas ficam gravadas no aparelho.
        filtros = filtros.copy(categoriasFavoritas = PreferenciasDeFavoritas.categorias(this))

        // Em rotação/recriação, não baixa tudo de novo (o cache já está atualizado).
        carregarCalendarios(baixar = savedInstanceState == null)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val gaveta = findViewById<DrawerLayout>(R.id.drawerLayout)
                    if (categoriaAtual.isEmpty() && gaveta != null) {
                        if (gaveta.isDrawerOpen(GravityCompat.START)) {
                            gaveta.closeDrawer(GravityCompat.START)
                            return
                        }
                        if (gaveta.isDrawerOpen(GravityCompat.END)) {
                            gaveta.closeDrawer(GravityCompat.END)
                            return
                        }
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
        outState.putString(CHAVE_FILTRO_CATEGORIA, filtros.categoria)
        outState.putBoolean(CHAVE_FILTRO_FINALIZADAS, filtros.mostrarFinalizadas)
        outState.putStringArrayList(CHAVE_FILTRO_PAISES, ArrayList(filtros.paises))
        outState.putStringArrayList(CHAVE_FILTRO_MESES, ArrayList(filtros.meses))
    }

    /** true até a primeira leitura dos calendários terminar (evita o aviso "nenhuma etapa"). */
    private var carregandoCalendarios = true

    /** Lê as sessões (cache ou JSON do APK); um problema aqui só desfaz a ordem por horário. */
    private fun coletarSessoes(app: android.content.Context): List<SessaoParaAviso> =
        try {
            ColetorDeSessoes.coletar(app)
        } catch (e: Exception) {
            registrarFalha("MainActivity.coletarSessoes", e)
            emptyList()
        }

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
            val f1a = lerEtapas("F1Academy") {
                F1AcademyCalendarJson.comoEtapas(F1AcademyCalendarJson.carregar(app))
            }
            val indy = lerEtapas("IndyCar") {
                IndyCarCalendarJson.comoEtapas(IndyCarCalendarJson.carregar(app))
            }
            val fe = lerEtapas("FormulaE") {
                FormulaECalendarJson.comoEtapas(FormulaECalendarJson.carregar(app))
            }
            val moto = lerEtapas("Moto") { MotoCalendarJson.etapasDeTodas(app) }
            val coletadas = coletarSessoes(app)

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                etapasF1Dinamicas = f1
                etapasF2Dinamicas = f2
                etapasF3Dinamicas = f3
                etapasF1AcademyDinamicas = f1a
                etapasIndyCarDinamicas = indy
                etapasFormulaEDinamicas = fe
                etapasMotoDinamicas = moto
                sessoes = coletadas
                carregandoCalendarios = false

                // Filtros restaurados de uma rotação podem apontar para algo que não existe mais.
                filtros = RegrasDeFiltro.podar(todasEtapas, filtros, java.time.LocalDate.now())

                if (categoriaAtual.isEmpty()) {
                    atualizarBotaoFiltro()
                    atualizarBarraDeFinalizadas()
                    mostrarEtapas(animar = true)
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

        F1AcademyCalendarJson.atualizar(this) {
            recarregar({ F1AcademyCalendarJson.comoEtapas(F1AcademyCalendarJson.carregar(applicationContext)) }) { etapasF1AcademyDinamicas = it }
        }

        IndyCarCalendarJson.atualizar(this) {
            recarregar({ IndyCarCalendarJson.comoEtapas(IndyCarCalendarJson.carregar(applicationContext)) }) { etapasIndyCarDinamicas = it }
        }

        FormulaECalendarJson.atualizar(this) {
            recarregar({ FormulaECalendarJson.comoEtapas(FormulaECalendarJson.carregar(applicationContext)) }) { etapasFormulaEDinamicas = it }
        }

        // MotoGP, Moto2, Moto3 e WEC usam as mesmas telas; cada classe tem o seu calendário.
        // As 4 classes costumam terminar quase juntas: agrupa tudo em uma única recarga.
        for (classe in ClasseMoto.values()) {
            MotoCalendarJson.atualizar(this, classe) {
                if (recargaMotoAgendada) return@atualizar
                recargaMotoAgendada = true
                handlerRedesenho.postDelayed({
                    recargaMotoAgendada = false
                    recarregar({ MotoCalendarJson.etapasDeTodas(applicationContext) }) { etapasMotoDinamicas = it }
                }, 800)
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
            val coletadas = coletarSessoes(app)
            AgendadorDeAvisos.reagendarEmSegundoPlano(app)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (etapas != null) aplicar(etapas)
                sessoes = coletadas
                redesenharListaDepois()
            }
        }.start()
    }


    override fun onResume() {
        super.onResume()

        // O tema pode ter sido trocado nas configurações enquanto esta tela estava parada.
        atualizarParaOTema()

        // Refaz os avisos ao abrir o app e depois do pedido de permissão de notificações.
        AgendadorDeAvisos.reagendarEmSegundoPlano(this)

        // O painel lateral mostra o estado das notificações; atualiza ao voltar das configurações.
        findViewById<LinearLayout>(R.id.containerMenuLateral)?.let {
            MenuPrincipal.montar(this, it)
        }

        // Etapas passam a "finalizadas" na virada do dia e algumas horas depois da última sessão:
        // se o app ficou aberto ou em segundo plano, refaz a lista.
        val ultima = dataDaUltimaLista
        val passouUmTempo = System.currentTimeMillis() - instanteDaUltimaLista > 10 * 60 * 1000L
        if (categoriaAtual.isEmpty() && ultima != null &&
            (ultima != java.time.LocalDate.now() || passouUmTempo)
        ) {
            mostrarEtapas()
        }
    }

    override fun onDestroy() {
        handlerRedesenho.removeCallbacksAndMessages(null)
        super.onDestroy()
    }


    /**
     * A tela inicial usa um cinza-escuro no lugar do preto. A janela e as barras do sistema
     * seguem a mesma cor, para não aparecer uma faixa mais escura no topo e no pé.
     */
    private fun definirCorDaJanela(cor: Int) {
        pintarBarrasDoSistema(cor)
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

    private fun mostrarMenu(animar: Boolean = true) {
        setContentView(R.layout.activity_main)
        aplicarInsetsDasBarras()
        definirCorDaJanela(getColor(R.color.azul_marinho))
        if (animar) animarEntrada()

        listaEtapas = findViewById(R.id.listaEtapas)

        MenuPrincipal.montar(this, findViewById(R.id.containerMenuLateral))
        findViewById<View>(R.id.btnMenu).setOnClickListener {
            findViewById<DrawerLayout>(R.id.drawerLayout).openDrawer(GravityCompat.START)
        }

        menuDeFiltros = MenuDeFiltros(
            activity = this,
            container = findViewById(R.id.containerFiltros),
            favoritas = { filtros.categoriasFavoritas },
            aoMudar = { novas -> aplicarFiltros(filtros.copy(categoriasFavoritas = novas)) }
        ).also { it.montar() }

        configurarAnimacaoDosPaineis()

        findViewById<View>(R.id.btnFiltro).setOnClickListener {
            findViewById<DrawerLayout>(R.id.drawerLayout).openDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.btnMenu).comEfeitoDeToque(escala = 0.9f)
        findViewById<View>(R.id.btnFiltro).comEfeitoDeToque(escala = 0.9f)
        BarraInferior.montar(this, findViewById(R.id.barraInferior))

        configurarBotaoTopo()
        configurarAtualizacao()
        atualizarBotaoFiltro()
        atualizarBarraDeFinalizadas()

        mostrarEtapas()
    }

    /** Selo no coração: quantas categorias favoritas há (some quando são 0). */
    private fun atualizarBotaoFiltro() {
        val selo = findViewById<TextView>(R.id.badgeFiltro) ?: return
        val quantos = filtros.categoriasFavoritas.size
        val estavaVisivel = selo.visibility == View.VISIBLE
        val antes = selo.text.toString()

        selo.text = quantos.toString()
        selo.visibility = if (quantos > 0) View.VISIBLE else View.GONE

        if (quantos > 0 && (!estavaVisivel || antes != quantos.toString())) selo.animarPop()
        if (quantos > 0 && antes != quantos.toString()) {
            findViewById<View>(R.id.btnFiltro)?.animarPop()
        }
    }

    /**
     * Os dois painéis laterais entram em cascata: cada linha sobe e aparece, uma após a outra,
     * assim que o painel começa a abrir.
     */
    private fun configurarAnimacaoDosPaineis() {
        val gaveta = findViewById<DrawerLayout>(R.id.drawerLayout) ?: return
        val jaAnimou = HashSet<Int>()

        gaveta.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                if (slideOffset <= 0f || !jaAnimou.add(drawerView.id)) return

                when (drawerView.id) {
                    R.id.scrollFiltros -> menuDeFiltros?.animarEntrada()
                    else -> findViewById<LinearLayout>(R.id.containerMenuLateral)?.entrarEmCascata()
                }
            }

            override fun onDrawerClosed(drawerView: View) {
                jaAnimou.remove(drawerView.id)

                // O painel dos favoritos fechou: agora a lista é refeita, já à vista.
                if (drawerView.id == R.id.scrollFiltros && listaDesatualizada) {
                    handlerRedesenho.removeCallbacks(redesenhoDosFiltros)
                    handlerRedesenho.postDelayed(redesenhoDosFiltros, 60)
                }
            }
        })
    }

    /** Há uma mudança de filtro ainda não refletida na lista da tela inicial. */
    private var listaDesatualizada = false
    private var rolarAoTopoPendente = false

    private val redesenhoDosFiltros = Runnable {
        if (!listaDesatualizada || categoriaAtual.isNotEmpty()) return@Runnable

        listaDesatualizada = false
        mostrarEtapas(animar = true)

        if (rolarAoTopoPendente) {
            rolarAoTopoPendente = false
            findViewById<ScrollView>(R.id.scrollPrincipal)?.scrollTo(0, 0)
        }
    }

    /**
     * Aplica uma mudança vinda do painel do coração ou do interruptor "Finalizadas": guarda o
     * que é gravado no aparelho (categorias favoritas) e atualiza a lista.
     *
     * Refazer a lista é o trabalho mais pesado do app e travava as animações do toque. Por isso
     * ele espera: com o painel da direita aberto, só acontece quando ele fecha; com o
     * interruptor, começa depois da animação dele.
     */
    private fun aplicarFiltros(novos: FiltrosDeEtapas) {
        val anteriores = filtros

        filtros = RegrasDeFiltro.podar(todasEtapas, novos, java.time.LocalDate.now())

        val favoritasMudaram = filtros.categoriasFavoritas != anteriores.categoriasFavoritas

        if (favoritasMudaram) {
            PreferenciasDeFavoritas.definirCategorias(this, filtros.categoriasFavoritas)
        }

        atualizarBotaoFiltro()
        atualizarBarraDeFinalizadas()

        if (favoritasMudaram || filtros.mostrarFinalizadas != anteriores.mostrarFinalizadas) {
            listaDesatualizada = true
            if (favoritasMudaram) rolarAoTopoPendente = true

            handlerRedesenho.removeCallbacks(redesenhoDosFiltros)

            val gaveta = findViewById<DrawerLayout>(R.id.drawerLayout)
            if (gaveta == null || !gaveta.isDrawerOpen(GravityCompat.END)) {
                handlerRedesenho.postDelayed(redesenhoDosFiltros, 220)
            }
        }
    }

    /** "Finalizadas" e o interruptor, acima das etapas: ligado, as etapas que já acabaram aparecem. */
    private fun atualizarBarraDeFinalizadas() {
        val barra = findViewById<View>(R.id.barraFinalizadas) ?: return
        val espaco = findViewById<android.widget.FrameLayout>(R.id.trocoFinalizadas) ?: return
        val ligado = filtros.mostrarFinalizadas
        val anterior = (espaco.getChildAt(0) as? InterruptorModerno)?.ligado

        espaco.removeAllViews()
        espaco.addView(
            InterruptorModerno(
                this,
                ligado,
                anterior,
                corLigado = getColor(R.color.azul_destaque),
                corDaTrilha = getColor(R.color.azul_marinho),
                corDoContorno = getColor(R.color.azul_marinho_borda)
            ).apply {
                isClickable = true
                isFocusable = true
                contentDescription = getString(R.string.cd_finalizadas)
                setOnClickListener {
                    aplicarFiltros(filtros.copy(mostrarFinalizadas = !ligado))
                }
            }
        )

        // Tocar em qualquer ponto da linha também liga e desliga.
        barra.isClickable = true
        barra.isFocusable = true
        barra.setOnClickListener {
            aplicarFiltros(filtros.copy(mostrarFinalizadas = !filtros.mostrarFinalizadas))
        }
    }

    /**
     * Puxar a lista para baixo, estando no topo, atualiza: baixa os calendários de novo e refaz
     * os avisos. A lista se atualiza sozinha quando cada calendário termina de baixar.
     */
    private fun configurarAtualizacao() {
        val puxar = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.atualizarLista)
            ?: return

        puxar.setColorSchemeColors(getColor(R.color.azul_destaque))
        puxar.setProgressBackgroundColorSchemeColor(getColor(R.color.superficie))

        puxar.setOnRefreshListener {
            AgendadorDeAvisos.reagendarEmSegundoPlano(this)
            baixarCalendariosNovos()
            redesenharListaDepois()

            // Os downloads seguem em segundo plano; o indicador some depois de um instante.
            handlerRedesenho.postDelayed({ puxar.isRefreshing = false }, 1400)
        }
    }

    /** Botão redondo que aparece ao rolar a lista para baixo e leva de volta ao topo. */
    private fun configurarBotaoTopo() {
        val rolagem = findViewById<ScrollView>(R.id.scrollPrincipal) ?: return
        val botao = findViewById<View>(R.id.btnTopo) ?: return

        botaoTopoVisivel = false
        mesNoTopo = -1
        val limite = emPx(240f)

        rolagem.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            mostrarBotaoTopo(botao, scrollY > topoDaLista() + limite)
            atualizarMesDoTopo()
        }

        // Cards entram aos poucos e as finalizadas aparecem por cima: o mês é refeito a cada layout.
        listaEtapas.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> atualizarMesDoTopo() }
        findViewById<TextView>(R.id.mesTopo)?.typeface =
            android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        botao.setOnClickListener { rolagem.smoothScrollTo(0, topoDaLista()) }
        botao.comEfeitoDeToque(escala = 0.88f)
    }

    /** Posição de rolagem do "topo de verdade" (0 quando não há finalizadas acima dele). */
    private fun topoDaLista(): Int {
        val ancora = ancoraDoTopo ?: return 0
        if (ancora.parent !== listaEtapas) return 0

        return maxOf(0, listaEtapas.top + ancora.top - emPx(4f))
    }

    /** Sobe e aparece ao rolar; desce e some ao voltar para perto do topo. */
    private fun mostrarBotaoTopo(botao: View, mostrar: Boolean) {
        if (mostrar == botaoTopoVisivel) return
        botaoTopoVisivel = mostrar

        botao.animate().cancel()
        val deslocamento = emPx(20f).toFloat()

        if (mostrar) {
            botao.alpha = 0f
            botao.translationY = deslocamento
            botao.visibility = View.VISIBLE
            botao.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(0)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            botao.animate()
                .alpha(0f)
                .translationY(deslocamento)
                .setStartDelay(0)
                .setDuration(160)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction { botao.visibility = View.GONE }
                .start()
        }
    }

    private fun fimDaEtapa(etapa: Etapa): java.time.LocalDate =
        RegrasDeFiltro.fim(etapa, java.time.LocalDate.now())


    /** Abre a tela da etapa, de acordo com a categoria (usado pela lista). */
    private fun abrirEtapa(etapa: Etapa) {
        when (etapa.categoria) {
            "F1" -> abrirEventoF1PelaEtapa(etapa)
            "F2" -> abrirEventoF2PelaEtapa(etapa)
            "F3" -> abrirEventoF3PelaEtapa(etapa)
            "F1 Academy" -> abrirEventoF1AcademyPelaEtapa(etapa)
            "Formula E" -> abrirEventoFormulaEPelaEtapa(etapa)
            "IndyCar" -> abrirEventoIndyCarPelaEtapa(etapa)
            "MotoGP", "Moto2", "Moto3", "WEC" -> abrirEventoMotoPelaEtapa(etapa)
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

    /** Cor de destaque da categoria; categorias fora da lista usam o verde padrão. */
    private fun corDaCategoria(categoria: String): Int =
        Categorias.info(categoria)?.let { android.graphics.Color.parseColor(it.cor) }
            ?: getColor(R.color.acento)

    /** Fundo do card com a faixa lateral na cor da categoria, colada na borda (ver [FundoDeCard]). */
    private fun estilizarCard(item: android.view.View, cor: Int) {
        item.background = FundoDeCard(
            corDaFaixa = cor,
            corInicial = ColorUtils.blendARGB(getColor(R.color.superficie), cor, 0.14f),
            corFinal = getColor(R.color.superficie),
            corDaBorda = getColor(R.color.borda_suave),
            raio = emPx(16f).toFloat(),
            larguraDaFaixa = emPx(4f).toFloat(),
            larguraDaBorda = emPx(1f).coerceAtLeast(1).toFloat()
        )
    }

    /** Mês mostrado agora na faixa azul (ano*12+mês); evita animar quando nada mudou. */
    private var mesNoTopo = -1

    /** "Novembro" em destaque e, fora do ano atual, o ano menor e mais claro ao lado. */
    private fun rotuloDoMes(data: java.time.LocalDate): CharSequence {
        val idiomaApp = Idioma.locale(this)
        val nome = data.month.getDisplayName(java.time.format.TextStyle.FULL, idiomaApp)
            .replaceFirstChar { it.titlecase(idiomaApp) }

        if (data.year == java.time.LocalDate.now().year) return nome

        val texto = android.text.SpannableString("$nome ${data.year}")
        val inicioDoAno = nome.length + 1
        texto.setSpan(
            android.text.style.RelativeSizeSpan(0.68f),
            inicioDoAno, texto.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        texto.setSpan(
            android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#9FB4DC")),
            inicioDoAno, texto.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return texto
    }

    /** Troca o mês da faixa azul: o antigo sobe e some, o novo sobe de baixo. */
    private fun mostrarMesNoTopo(data: java.time.LocalDate) {
        val rotulo = findViewById<TextView>(R.id.mesTopo) ?: return
        val chave = data.year * 12 + data.monthValue

        if (chave == mesNoTopo) return

        val primeiraVez = mesNoTopo == -1
        mesNoTopo = chave

        rotulo.animate().cancel()

        if (primeiraVez) {
            rotulo.text = rotuloDoMes(data)
            rotulo.alpha = 1f
            rotulo.translationY = 0f
            return
        }

        val deslocamento = emPx(10f).toFloat()
        rotulo.animate().alpha(0f).translationY(-deslocamento).setDuration(110).withEndAction {
            rotulo.text = rotuloDoMes(data)
            rotulo.translationY = deslocamento
            rotulo.animate().alpha(1f).translationY(0f).setDuration(200)
                .setInterpolator(DecelerateInterpolator()).start()
        }.start()
    }

    /**
     * Mês da faixa azul. Acompanha a rolagem: troca quando a lista chega às etapas de outro mês.
     * Os cabeçalhos de mês da lista são só marcadores; o mês é lido da etiqueta de cada um.
     */
    private fun atualizarMesDoTopo() {
        val rolagem = findViewById<ScrollView>(R.id.scrollPrincipal) ?: return

        val marcadores = cabecalhosDeMes
            .filter { it.parent === listaEtapas && it.visibility == View.VISIBLE }
            .sortedBy { it.top }

        if (marcadores.isEmpty()) return

        val linhaDeCorte = rolagem.scrollY + emPx(12f)
        var atual = marcadores.first()

        for (marcador in marcadores) {
            if (listaEtapas.top + marcador.top <= linhaDeCorte) atual = marcador else break
        }

        (atual.tag as? java.time.LocalDate)?.let { mostrarMesNoTopo(it) }
    }

    /** Cabeçalho de mês, com uma linha fina separando do grupo anterior. */
    private fun cabecalhoMes(data: java.time.LocalDate, primeiro: Boolean): android.view.View {
        // O nome do mês fica na faixa azul; aqui só um respiro e uma linha fina entre os meses.
        val coluna = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        if (!primeiro) {
            coluna.addView(
                android.view.View(this).apply {
                    setBackgroundColor(getColor(R.color.borda_suave))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        emPx(1f).coerceAtLeast(1)
                    ).apply { topMargin = emPx(8f); bottomMargin = emPx(8f) }
                }
            )
        } else {
            coluna.addView(
                android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        emPx(6f)
                    )
                }
            )
        }

        return coluna
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

    /** País ou cidade no idioma do app (em inglês, traduz os nomes em português). */
    private fun lugarExibido(nome: String): String =
        if (Idioma.ingles(this)) TraducaoDeNomes.lugar(nome) else nome

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

    /** Desenha a lista da tela inicial com a categoria e os filtros escolhidos. */
    private fun mostrarEtapas(animar: Boolean = false) {
        val hoje = java.time.LocalDate.now()
        dataDaUltimaLista = hoje
        instanteDaUltimaLista = System.currentTimeMillis()

        // Um novo desenho cancela o anterior, caso ele ainda estivesse entrando aos poucos.
        geracaoDaLista++
        val geracao = geracaoDaLista
        ancoraDoTopo = null
        cabecalhosDeMes.clear()

        val etapasOrdenadas = RegrasDeFiltro.aplicar(todasEtapas, filtros, hoje, sessoes)
        primeirasSessoesDaLista = RegrasDeFiltro.primeirasSessoes(etapasOrdenadas, sessoes)
        ultimasSessoesDaLista = RegrasDeFiltro.ultimasSessoes(etapasOrdenadas, sessoes)

        listaEtapas.removeAllViews()

        if (etapasOrdenadas.isEmpty()) {
            // Ainda lendo os calendários: melhor mostrar nada do que "nenhuma etapa".
            if (carregandoCalendarios) return

            val vazio = TextView(this)
            vazio.text = getString(R.string.lista_vazia)
            vazio.setTextColor(getColor(R.color.texto_suave))
            vazio.textSize = 15f
            vazio.gravity = android.view.Gravity.CENTER
            vazio.setPadding(0, emPx(64f), 0, emPx(64f))
            listaEtapas.addView(vazio)
            if (animar) animarEntradaDoItem(vazio, 0, 1f)
            return
        }

        // As linhas da lista: um cabeçalho a cada mês novo e um card por etapa.
        val linhas = ArrayList<LinhaDaLista>()
        var mesAnterior = -1
        var primeiraAtiva = -1

        for (etapa in etapasOrdenadas) {
            val inicioItem = parseDataSegura(etapa.dataInicio)
            if (inicioItem != null) {
                val chaveMes = inicioItem.year * 12 + inicioItem.monthValue
                if (chaveMes != mesAnterior) {
                    linhas.add(LinhaDaLista(null, inicioItem))
                    mesAnterior = chaveMes
                }
            }

            if (primeiraAtiva < 0 && !RegrasDeFiltro.finalizada(etapa, hoje)) {
                primeiraAtiva = linhas.size
            }
            linhas.add(LinhaDaLista(etapa, null))
        }

        // A lista começa no cabeçalho do mês da primeira etapa não finalizada. As finalizadas
        // (quando ligadas) entram por cima, sem mexer no que o usuário está vendo.
        var ancora = if (primeiraAtiva > 0) primeiraAtiva else 0
        while (ancora > 0 && linhas[ancora].etapa != null) ancora--
        if (linhas[ancora].etapa != null) ancora = maxOf(primeiraAtiva, 0)

        val inflater = LayoutInflater.from(this)
        val proximas = proximasPorCategoria(hoje)
        var proxima = ancora
        var anterior = ancora - 1
        var cardsCriados = 0
        val acima = ArrayList<View>()

        fun criarLinha(indice: Int): View {
            val etapa = linhas[indice].etapa
            val mes = linhas[indice].mes

            return if (etapa == null && mes != null) {
                cabecalhoMes(mes, indice == 0).also {
                    it.tag = mes
                    cabecalhosDeMes.add(it)
                }
            } else {
                criarCard(inflater, etapa!!, statusEtapa(etapa, hoje, proximas), hoje)
            }
        }

        // Com as finalizadas a lista passa de uma centena de cards. Criar todos de uma vez
        // trava o app por alguns segundos; em lotes pequenos ele continua respondendo.
        fun desenharLote() {
            if (geracao != geracaoDaLista || isFinishing || isDestroyed || categoriaAtual.isNotEmpty()) {
                return
            }

            var criadas = 0

            // 1) De cima para baixo, a partir do topo de verdade.
            while (proxima < linhas.size && criadas < ETAPAS_POR_LOTE) {
                val view = criarLinha(proxima)
                listaEtapas.addView(view)

                if (proxima == ancora) ancoraDoTopo = view

                if (linhas[proxima].etapa != null) {
                    if (animar) animarEntradaDoItem(view, cardsCriados, view.alpha)
                    cardsCriados++
                }

                proxima++
                criadas++
            }

            // 2) Depois, as anteriores (finalizadas): entram escondidas, no começo da lista.
            while (proxima >= linhas.size && anterior >= 0 && criadas < ETAPAS_POR_LOTE) {
                val view = criarLinha(anterior)
                view.visibility = View.GONE
                listaEtapas.addView(view, 0)
                acima.add(view)

                anterior--
                criadas++
            }

            if (proxima < linhas.size || anterior >= 0) {
                handlerRedesenho.post { desenharLote() }
            } else if (acima.isNotEmpty()) {
                revelarEtapasDeCima(acima)
            }
        }

        desenharLote()
    }

    /**
     * Mostra de uma vez as etapas criadas acima do topo de verdade e empurra a rolagem pelo
     * tamanho que elas ocupam, para o que o usuário está vendo não sair do lugar. Ele rola
     * para cima para encontrá-las.
     */
    private fun revelarEtapasDeCima(views: List<View>) {
        val ancora = ancoraDoTopo ?: return
        val rolagem = findViewById<ScrollView>(R.id.scrollPrincipal) ?: return

        listaEtapas.viewTreeObserver.addOnPreDrawListener(
            object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    listaEtapas.viewTreeObserver.removeOnPreDrawListener(this)
                    if (ancora.parent !== listaEtapas) return true

                    val topoAntes = ancora.top
                    for (view in views) view.visibility = View.VISIBLE

                    // Quando o novo layout estiver pronto, compensa antes de desenhar.
                    listaEtapas.viewTreeObserver.addOnPreDrawListener(
                        object : android.view.ViewTreeObserver.OnPreDrawListener {
                            override fun onPreDraw(): Boolean {
                                listaEtapas.viewTreeObserver.removeOnPreDrawListener(this)
                                if (ancora.parent === listaEtapas) {
                                    rolagem.scrollBy(0, ancora.top - topoAntes)
                                }
                                return true
                            }
                        }
                    )

                    // Cancela este desenho: o próximo já sai com tudo no lugar.
                    return false
                }
            }
        )
    }

    /** "Sex, 23/10 · 15:30": dia da semana, data e, se houver, o horário da sessão. */
    private fun linhaDeData(
        data: java.time.LocalDate?,
        horario: String?,
        textoOriginal: String
    ): String {
        if (data == null) return textoOriginal

        val linha = "${FormatadorDeData.diaDaSemana(data, Idioma.locale(this))}, " +
            PreferenciasDeData.curta(this, data)

        return if (horario != null) "$linha · $horario" else linha
    }

    /** "HOJE", "3 DIAS", "1 SEMANA", "2 MESES": quanto falta para a etapa começar. */
    private fun textoDaContagem(inicio: java.time.LocalDate, hoje: java.time.LocalDate): String =
        TextoDeContagem.de(this, inicio, hoje)

    /** Cria o card de uma etapa (sem colocá-lo na lista). */
    private fun criarCard(
        inflater: LayoutInflater,
        etapa: Etapa,
        status: String,
        hoje: java.time.LocalDate
    ): View {
        val item = inflater.inflate(R.layout.item_etapa, listaEtapas, false)

        // Categoria: selo preenchido na cor dela, acima do nome
        val corItem = corDaCategoria(etapa.categoria)
        val chipCategoria = item.findViewById<TextView>(R.id.itemCategoria)
        chipCategoria.text = Categorias.rotuloDoSelo(etapa.categoria)
        estilizarSeloPreenchido(chipCategoria, corItem, 7f)
        estilizarCard(item, corItem)

        val nomeEvento = nomeDoEvento(etapa)
        item.findViewById<TextView>(R.id.itemPais).text = nomeEvento.ifBlank { lugarExibido(etapa.pais) }
        item.findViewById<TextView>(R.id.itemCircuito).text = localDaEtapa(etapa, nomeEvento)

        // Duas linhas: o começo (primeira sessão) em cima e o fim (última sessão) embaixo, com dia
        // da semana, data e horário no fuso do aparelho. Sem horários, valem as datas da etapa.
        val zona = java.time.ZoneId.systemDefault()
        val usa24 = Hora.usa24Horas(this)
        val primeira = primeirasSessoesDaLista[etapa]?.atZone(zona)
        val ultima = ultimasSessoesDaLista[etapa]?.atZone(zona)

        val inicioDaEtapa = primeira?.toLocalDate() ?: parseDataSegura(etapa.dataInicio)
        val textoDaData = linhaDeData(
            inicioDaEtapa,
            primeira?.let { Hora.formatar(it.hour, it.minute, usa24) },
            etapa.data
        )

        val textoDoFim: String? = if (primeira != null && ultima != null) {
            if (ultima.toInstant() != primeira.toInstant()) {
                linhaDeData(ultima.toLocalDate(), Hora.formatar(ultima.hour, ultima.minute, usa24), "")
            } else {
                null
            }
        } else {
            val fim = parseDataSegura(etapa.dataFim)
            if (fim != null && inicioDaEtapa != null && fim.isAfter(inicioDaEtapa)) {
                linhaDeData(fim, null, "")
            } else {
                null
            }
        }

        item.findViewById<TextView>(R.id.itemData).text = textoDaData

        val itemDataFim = item.findViewById<TextView>(R.id.itemDataFim)
        if (textoDoFim != null) {
            itemDataFim.text = textoDoFim
        } else {
            itemDataFim.visibility = View.GONE
        }

        // Quanto falta para começar (a próxima etapa vem preenchida); ao vivo e finalizada têm texto próprio.
        val textoStatus = when (status) {
            "PRÓXIMA", "AGENDADA" ->
                inicioDaEtapa?.let { textoDaContagem(it, hoje) }.orEmpty()
            else -> textoDoStatus(status)
        }

        val itemStatus = item.findViewById<TextView>(R.id.itemStatus)
        if (textoStatus.isEmpty()) {
            itemStatus.visibility = View.GONE
        } else {
            itemStatus.text = textoStatus
        }

        // Próxima e em andamento: selo preenchido na cor da categoria
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

        // Etapas finalizadas (só aparecem se o usuário pedir nos filtros) ficam apagadas.
        item.alpha = if (status == "FINALIZADA") 0.45f else 1f

        item.contentDescription =
            "${etapa.categoria}, ${nomeEvento.ifBlank { lugarExibido(etapa.pais) }}, " +
                "${localDaEtapa(etapa, nomeEvento)}, $textoDaData, ${textoDoFim.orEmpty()}, $textoStatus"

        item.setOnClickListener { abrirEtapa(etapa) }
        item.comEfeitoDeToque(escala = 0.97f)

        return item
    }

    /** Os primeiros cards entram um após o outro, subindo e aparecendo; os demais já nascem prontos. */
    private fun animarEntradaDoItem(item: View, indice: Int, alvoDeAlpha: Float) {
        if (indice >= 10) return

        item.alpha = 0f
        item.translationY = emPx(14f).toFloat()
        item.animate()
            .alpha(alvoDeAlpha)
            .translationY(0f)
            .setStartDelay(indice * 30L)
            .setDuration(240)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }


    private fun statusEtapa(
        etapa: Etapa,
        hoje: java.time.LocalDate,
        proximas: Map<String, Etapa>
    ): String {
        val inicioEtapa = parseDataSegura(etapa.dataInicio) ?: return "AGENDADA"

        if (RegrasDeFiltro.finalizada(etapa, hoje)) {
            return "FINALIZADA"
        }

        // Em andamento só quando a primeira sessão começa; no primeiro dia, antes disso, ainda
        // é "por vir". Sem horários conhecidos, vale o primeiro dia da etapa.
        val primeira = primeirasSessoesDaLista[etapa]
        val comecou = if (primeira != null) {
            !java.time.Instant.now().isBefore(primeira)
        } else {
            !inicioEtapa.isAfter(hoje)
        }

        if (comecou) {
            return "EM ANDAMENTO"
        }

        return if (proximas[etapa.categoria] == etapa) "PRÓXIMA" else "AGENDADA"
    }

    /** A primeira etapa ainda não finalizada de cada categoria, calculada uma vez por desenho. */
    private fun proximasPorCategoria(hoje: java.time.LocalDate): Map<String, Etapa> {
        val proximas = HashMap<String, Etapa>()

        for (etapa in todasEtapas) {
            if (etapa.categoria !in proximas && !RegrasDeFiltro.finalizada(etapa, hoje)) {
                proximas[etapa.categoria] = etapa
            }
        }

        return proximas
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
            "F1 Academy" -> getString(R.string.f1_academy)
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

                "F1 Academy" -> {
                    startActivity(Intent(this, F1AcademyCalendarActivity::class.java))
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

    private fun abrirEventoF1AcademyPelaEtapa(etapa: Etapa) {
        val doCircuito = F1AcademyCalendarJson.carregar(this)
            .filter { it.circuito.equals(etapa.circuito, ignoreCase = true) }
        val encontrado = doCircuito.firstOrNull { mesmaData(it.inicio, etapa) } ?: doCircuito.firstOrNull()

        if (encontrado != null) {
            val intent = Intent(this, F1AcademyActivity::class.java)
            intent.putExtra("ETAPA", encontrado.etapa)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } else {
            abrirCategoria("F1 Academy")
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

        if (encontrado == null) {
            // Mesmo critério das outras categorias: sem data igual, usa o circuito.
            encontrado = IndyCarCalendarJson.carregar(this)
                .firstOrNull { it.circuito.equals(etapa.circuito, ignoreCase = true) }
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