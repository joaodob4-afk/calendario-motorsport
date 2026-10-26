package com.motorsport.calendario

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Fundo com efeito de toque (ondulação clara, para fundos azul-marinho) em linhas clicáveis. */
internal fun fundoDeToque(): RippleDrawable =
    RippleDrawable(
        ColorStateList.valueOf(Color.parseColor("#33FFFFFF")),
        null,
        ColorDrawable(Color.WHITE)
    )

/**
 * Conteúdo do painel que abre pelo lado da tela inicial (botão no canto superior esquerdo).
 *
 * Ordem: alertar na hora de começar, antecedências (dá para marcar mais de uma), as categorias
 * (em cada linha o nome e, ao lado, as três opções sempre à vista), envio de teste e atalho da
 * bateria. Só os botões e o interruptor respondem ao toque.
 * Para incluir uma função nova no futuro, crie um bloco novo e chame-o em [montar].
 */
internal object MenuPrincipal {

    private val corDoTextoSobreCor = Color.parseColor("#0E1012")
    private val corDoAviso = Color.parseColor("#FFB74D")
    private val corDoContorno = Color.parseColor("#3A3F45")

    /**
     * Antes, cada categoria abria suas opções ao toque e este método fechava a aberta. Agora as
     * opções ficam sempre à vista e não há estado a limpar; o método continua para os testes.
     */
    internal fun reiniciar() {
        estadoAnteriorDosInterruptores.clear()
        categoriasAbertas = false
    }

    /** Lista de categorias recolhida (só o título e a quantidade) ou aberta. */
    private var categoriasAbertas = false

    fun montar(activity: Activity, container: LinearLayout) {
        container.removeAllViews()

        val selecionados = PreferenciasDeAviso.minutosSelecionados(activity)
        val naHora = PreferenciasDeAviso.avisarNaHora(activity)

        // Há alerta ligado se existir alguma antecedência escolhida ou o aviso da hora de começar.
        val ativo = naHora || selecionados.isNotEmpty()

        container.addView(cabecalho(activity))

        container.addView(cartaoNaHora(activity, container, naHora))

        container.addView(tituloDeSecao(activity, R.string.antecedencia_titulo, topo = 18f, baixo = 10f))
        container.addView(botoesDeAntecedencia(activity, container, selecionados))
        container.addView(botaoPersonalizar(activity, selecionados))

        container.addView(cartaoRecolhivelDeCategorias(activity, container, ativo))

        if (ativo && !Notificacoes.permitidas(activity)) {
            container.addView(cartaoDeBloqueio(activity))
        }

        container.addView(botaoDeTeste(activity))

        container.addView(rotuloDaBateria(activity))
        container.addView(botaoDeBateria(activity))
    }

    // ---------------------------------------------------------------- blocos

    private fun cabecalho(activity: Activity): View {
        val icone = ImageView(activity).apply {
            setImageResource(R.drawable.ic_sino)
            setColorFilter(Color.WHITE)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setPadding(activity.emPx(9f), activity.emPx(9f), activity.emPx(9f), activity.emPx(9f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(activity.getColor(R.color.azul_marinho_claro))
                setStroke(activity.emPx(1f), activity.getColor(R.color.azul_marinho_borda))
            }
            layoutParams = LinearLayout.LayoutParams(activity.emPx(40f), activity.emPx(40f))
        }

        // Mostra "Gerenciamento de alertas"; se não couber numa linha, usa "Gerenciar Alertas".
        val tituloLongo = activity.getString(R.string.menu_titulo)
        val tituloCurto = activity.getString(R.string.menu_titulo_curto)

        val titulo = texto(activity, tituloLongo, 22f, principal(activity), Typeface.BOLD).apply {
            isSingleLine = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = activity.emPx(12f) }
            addOnLayoutChangeListener { _, esquerda, _, direita, _, _, _, _, _ ->
                val largura = direita - esquerda - paddingLeft - paddingRight
                if (largura > 0) {
                    val escolhido = if (paint.measureText(tituloLongo) <= largura) tituloLongo else tituloCurto
                    if (text.toString() != escolhido) text = escolhido
                }
            }
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = parametros(activity, baixo = 16f)
            addView(icone)
            addView(titulo)
        }
    }

    private fun tituloDeSecao(activity: Activity, textoRes: Int, topo: Float, baixo: Float) =
        texto(activity, activity.getString(textoRes), 16f, principal(activity), Typeface.BOLD).apply {
            layoutParams = parametros(activity, topo = topo, baixo = baixo, inicio = 2f)
        }

    private fun cartao(activity: Activity) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        background = forma(
            activity,
            activity.getColor(R.color.superficie),
            activity.getColor(R.color.borda_suave),
            16f
        )
        setPadding(activity.emPx(16f), activity.emPx(14f), activity.emPx(16f), activity.emPx(14f))
        layoutParams = parametros(activity)
    }

    /** Estado dos interruptores antes do toque; faz a bolinha deslizar quando o painel é refeito. */
    private val estadoAnteriorDosInterruptores = mutableMapOf<String, Boolean>()

    /** Só o interruptor responde ao toque; a linha em volta não. */
    private fun interruptor(
        activity: Activity,
        chave: String,
        ligado: Boolean,
        descricao: String,
        aoTocar: () -> Unit
    ): View {
        val anterior = estadoAnteriorDosInterruptores.remove(chave)

        return InterruptorModerno(
            activity,
            ligado,
            anterior,
            corLigado = activity.getColor(R.color.azul_destaque),
            corDaTrilha = activity.getColor(R.color.fundo_tela),
            corDoContorno = activity.getColor(R.color.borda_suave)
        ).apply {
            contentDescription = descricao
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                estadoAnteriorDosInterruptores[chave] = ligado
                aoTocar()
            }
        }
    }

    /** "Alertar na hora de começar": aviso "começando agora", na hora exata da sessão. */
    private fun cartaoNaHora(activity: Activity, container: LinearLayout, ligado: Boolean): View {
        val linha = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                texto(activity, activity.getString(R.string.na_hora_curto), 16f, principal(activity), Typeface.BOLD)
                    .apply {
                        setPadding(0, 0, activity.emPx(8f), 0)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
            )
            addView(
                interruptor(
                    activity,
                    "nahora",
                    ligado,
                    activity.getString(if (ligado) R.string.cd_desligar_na_hora else R.string.cd_ligar_na_hora)
                ) {
                    val novo = !ligado
                    PreferenciasDeAviso.definirAvisarNaHora(activity, novo)

                    if (novo && Notificacoes.precisaPedirPermissao(activity)) {
                        Notificacoes.pedirPermissao(activity)
                    }

                    AgendadorDeAvisos.reagendarEmSegundoPlano(activity)
                    montar(activity, container)
                }
            )
        }

        return cartao(activity).apply { addView(linha) }
    }

    /**
     * Por antecedência: quatro botões arredondados, e dá para marcar mais de um. Os marcados
     * ficam preenchidos de azul. Sem nenhum marcado, só o aviso da hora de começar continua valendo.
     */
    private fun botoesDeAntecedencia(
        activity: Activity,
        container: LinearLayout,
        selecionados: List<Int>
    ): View =
        LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = parametros(activity)

            PreferenciasDeAviso.opcoes.reversed().forEachIndexed { indice, opcao ->
                val selecionada = opcao in selecionados
                val rotulo = rotuloCurtoDaAntecedencia(activity, opcao)

                addView(
                    texto(
                        activity,
                        rotulo,
                        14f,
                        if (selecionada) corDoTextoSobreCor else principal(activity),
                        if (selecionada) Typeface.BOLD else Typeface.NORMAL
                    ).apply {
                        gravity = Gravity.CENTER
                        contentDescription = descricaoDaAntecedencia(activity, opcao)
                        isClickable = true
                        isFocusable = true
                        background = if (selecionada) {
                            comToque(activity, activity.getColor(R.color.azul_destaque), null, 20f)
                        } else {
                            comToque(
                                activity,
                                activity.getColor(R.color.superficie),
                                activity.getColor(R.color.borda_suave),
                                20f
                            )
                        }
                        layoutParams = LinearLayout.LayoutParams(0, activity.emPx(40f), 1f).apply {
                            if (indice > 0) marginStart = activity.emPx(8f)
                        }
                        setOnClickListener {
                            val novo = if (selecionada) selecionados - opcao else selecionados + opcao
                            aplicarAntecedencia(activity, container, novo)
                        }
                    }
                )
            }
        }

    /**
     * "Personalizar alertas": abre, descendo do topo, a página com todas as antecedências.
     * Quando há antecedências fora dos botões rápidos marcadas, mostra quantas são.
     */
    private fun botaoPersonalizar(activity: Activity, selecionados: List<Int>): View {
        val extras = selecionados.count { it !in PreferenciasDeAviso.opcoes }

        val rotulo = texto(
            activity,
            activity.getString(R.string.personalizar_alertas),
            15f,
            principal(activity),
            Typeface.BOLD
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            contentDescription = activity.getString(R.string.personalizar_alertas)
            background = comToque(activity, Color.TRANSPARENT, activity.getColor(R.color.borda_suave), 14f)
            setPadding(activity.emPx(16f), 0, activity.emPx(14f), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.emPx(46f)
            ).apply { topMargin = activity.emPx(12f) }

            addView(rotulo)

            if (extras > 0) {
                addView(
                    texto(activity, "+$extras", 12f, corDoTextoSobreCor, Typeface.BOLD).apply {
                        gravity = Gravity.CENTER
                        minWidth = activity.emPx(24f)
                        setPadding(activity.emPx(7f), activity.emPx(2f), activity.emPx(7f), activity.emPx(2f))
                        background = forma(activity, activity.getColor(R.color.azul_destaque), null, 10f)
                    }
                )
            }

            setOnClickListener {
                activity.startActivity(Intent(activity, PersonalizarAlertasActivity::class.java))
                activity.overridePendingTransition(R.anim.descer_entrada, R.anim.parada)
            }
        }
    }

    /**
     * "Categorias" com a quantidade ao lado, como um menu que abre ao toque. Fechado, só mostra
     * o título; aberto, mostra uma linha por categoria com as opções Todas, Corridas e Nenhuma.
     */
    private fun cartaoRecolhivelDeCategorias(
        activity: Activity,
        container: LinearLayout,
        ativo: Boolean
    ): View {
        val quantidade = texto(
            activity,
            Categorias.todas.size.toString(),
            16f,
            principal(activity),
            Typeface.NORMAL
        )

        val seta = ImageView(activity).apply {
            setImageResource(R.drawable.ic_chevron)
            setColorFilter(activity.getColor(R.color.texto_suave))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            rotation = if (categoriasAbertas) 180f else 0f
            layoutParams = LinearLayout.LayoutParams(activity.emPx(20f), activity.emPx(20f))
                .apply { marginStart = activity.emPx(6f) }
        }

        val caixa = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = forma(activity, activity.getColor(R.color.fundo_tela), activity.getColor(R.color.borda_suave), 12f)
            setPadding(activity.emPx(14f), activity.emPx(8f), activity.emPx(10f), activity.emPx(8f))
            addView(quantidade)
            addView(seta)
        }

        val cabecalhoDoCartao = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            contentDescription = activity.getString(R.string.filtros_categorias)
            minimumHeight = activity.emPx(48f)
            background = comToque(activity, Color.TRANSPARENT, null, 14f)
            setPadding(activity.emPx(16f), activity.emPx(6f), activity.emPx(12f), activity.emPx(6f))
            addView(
                texto(
                    activity,
                    activity.getString(R.string.filtros_categorias),
                    16f,
                    principal(activity),
                    Typeface.BOLD
                ).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
            )
            addView(caixa)
            setOnClickListener {
                categoriasAbertas = !categoriasAbertas
                montar(activity, container)
            }
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = forma(
                activity,
                activity.getColor(R.color.superficie),
                activity.getColor(R.color.borda_suave),
                16f
            )
            layoutParams = parametros(activity, topo = 14f)
            addView(cabecalhoDoCartao)

            if (categoriasAbertas) {
                addView(
                    LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(activity.emPx(8f), 0, activity.emPx(8f), activity.emPx(10f))
                        addView(cartaoDeCategorias(activity, container, ativo).apply { background = null })
                        addView(legendaDosModos(activity))
                    }
                )
            }
        }
    }

    /**
     * Um cartão com todas as categorias. Em cada linha, o nome (com a cor dela) à esquerda e, ao
     * lado, as três opções sempre à vista: Todas, Corridas e Nenhuma. Nada abre ou fecha.
     */
    private fun cartaoDeCategorias(activity: Activity, container: LinearLayout, ativo: Boolean): View =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = forma(
                activity,
                activity.getColor(R.color.superficie),
                activity.getColor(R.color.borda_suave),
                16f
            )
            // Esmaecido quando os avisos estão desligados; as escolhas continuam valendo.
            alpha = if (ativo) 1f else 0.45f
            layoutParams = parametros(activity)

            Categorias.todas.forEachIndexed { indice, categoria ->
                if (indice > 0) addView(divisor(activity))
                addView(linhaDaCategoria(activity, container, categoria))
            }
        }

    private fun divisor(activity: Activity) =
        View(activity).apply {
            setBackgroundColor(activity.getColor(R.color.borda_suave))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.emPx(1f).coerceAtLeast(1)
            ).apply {
                marginStart = activity.emPx(14f)
                marginEnd = activity.emPx(14f)
            }
        }

    private fun linhaDaCategoria(
        activity: Activity,
        container: LinearLayout,
        categoria: CategoriaInfo
    ): View {
        val modoAtual = PreferenciasDeAviso.modoDaCategoria(activity, categoria.nome)

        val ponto = View(activity).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(categoria.cor))
            }
            layoutParams = LinearLayout.LayoutParams(activity.emPx(10f), activity.emPx(10f))
                .apply { marginEnd = activity.emPx(8f) }
        }

        val nome = texto(activity, categoria.nome, 14f, principal(activity), Typeface.BOLD).apply {
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = activity.emPx(56f)
            setPadding(activity.emPx(12f), activity.emPx(8f), activity.emPx(6f), activity.emPx(8f))
            addView(ponto)
            addView(nome)
            addView(
                seletorDeModo(activity, modoAtual, categoria.cor) { novo ->
                    PreferenciasDeAviso.definirModo(activity, categoria.nome, novo)
                    AgendadorDeAvisos.reagendarEmSegundoPlano(activity)
                    montar(activity, container)
                }
            )
        }
    }

    private fun legendaDosModos(activity: Activity) =
        texto(
            activity,
            activity.getString(R.string.campeonato_legenda),
            12f,
            activity.getColor(R.color.texto_suave),
            Typeface.NORMAL
        ).apply {
            setLineSpacing(0f, 1.15f)
            layoutParams = parametros(activity, topo = 8f, inicio = 2f)
        }

    /** Três opções lado a lado; a escolhida ganha a cor do campeonato. */
    private fun seletorDeModo(
        activity: Activity,
        atual: ModoDeAviso,
        corHex: String,
        aoEscolher: (ModoDeAviso) -> Unit
    ): View {
        val suave = activity.getColor(R.color.texto_suave)

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            background = forma(activity, activity.getColor(R.color.fundo_tela), activity.getColor(R.color.borda_suave), 12f)
            setPadding(activity.emPx(2f), activity.emPx(3f), activity.emPx(2f), activity.emPx(3f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                activity.emPx(38f)
            )

            ModoDeAviso.values().forEach { modo ->
                val marcado = modo == atual
                val corMarcada = if (modo == ModoDeAviso.NENHUMA) {
                    Color.parseColor("#6B7280")
                } else {
                    Color.parseColor(corHex)
                }
                val corDoTexto = when {
                    !marcado -> suave
                    modo == ModoDeAviso.NENHUMA -> Color.WHITE
                    else -> corDoTextoSobreCor
                }

                addView(
                    texto(
                        activity,
                        rotuloDoModo(activity, modo),
                        11f,
                        corDoTexto,
                        if (marcado) Typeface.BOLD else Typeface.NORMAL
                    ).apply {
                        gravity = Gravity.CENTER
                        isSingleLine = true
                        isClickable = true
                        isFocusable = true
                        contentDescription = rotuloDoModo(activity, modo)
                        setPadding(activity.emPx(4f), 0, activity.emPx(4f), 0)
                        background = if (marcado) {
                            comToque(activity, corMarcada, null, 9f)
                        } else {
                            comToque(activity, Color.TRANSPARENT, null, 9f)
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        setOnClickListener { aoEscolher(modo) }
                    }
                )
            }
        }
    }

    /** Aparece quando há avisos ligados, mas o Android bloqueia as notificações do app. */
    private fun cartaoDeBloqueio(activity: Activity): View =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            background = comToque(activity, Color.parseColor("#2A2113"), corDoAviso, 14f)
            setPadding(activity.emPx(16f), activity.emPx(14f), activity.emPx(16f), activity.emPx(14f))
            layoutParams = parametros(activity, topo = 16f)
            addView(texto(activity, activity.getString(R.string.bloqueio_titulo), 15f, corDoAviso, Typeface.BOLD))
            addView(
                texto(
                    activity,
                    activity.getString(R.string.bloqueio_texto),
                    13f,
                    activity.getColor(R.color.texto_suave),
                    Typeface.NORMAL
                ).apply { setPadding(0, activity.emPx(2f), 0, 0) }
            )
            setOnClickListener {
                if (Notificacoes.precisaPedirPermissao(activity)) {
                    Notificacoes.pedirPermissao(activity)
                } else {
                    Notificacoes.abrirConfiguracoes(activity)
                }
            }
        }

    private fun botaoDeTeste(activity: Activity): View {
        val sino = ImageView(activity).apply {
            setImageResource(R.drawable.ic_sino)
            setColorFilter(principal(activity))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = LinearLayout.LayoutParams(activity.emPx(20f), activity.emPx(20f))
                .apply { marginEnd = activity.emPx(10f) }
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = activity.getString(R.string.botao_teste)
            background = comToque(activity, Color.TRANSPARENT, activity.getColor(R.color.borda_suave), 14f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.emPx(46f)
            ).apply { topMargin = activity.emPx(14f) }
            addView(sino)
            addView(texto(activity, activity.getString(R.string.botao_teste), 15f, principal(activity), Typeface.BOLD))
            setOnClickListener {
                when {
                    Notificacoes.precisaPedirPermissao(activity) ->
                        Notificacoes.pedirPermissao(activity)

                    !Notificacoes.permitidas(activity) ->
                        Notificacoes.abrirConfiguracoes(activity)

                    else -> Notificacoes.mostrarTeste(activity)
                }
            }
        }
    }

    private fun rotuloDaBateria(activity: Activity) =
        texto(activity, activity.getString(R.string.alertas_nao_chegam), 13f, activity.getColor(R.color.texto_suave), Typeface.NORMAL)
            .apply { layoutParams = parametros(activity, topo = 22f, inicio = 2f) }

    /** Atalho para quem para de receber alertas: o Android ou o fabricante pode estar "dormindo" o app. */
    private fun botaoDeBateria(activity: Activity): View =
        texto(activity, activity.getString(R.string.botao_bateria), 14f, principal(activity), Typeface.NORMAL).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = comToque(activity, Color.TRANSPARENT, activity.getColor(R.color.borda_suave), 14f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.emPx(44f)
            ).apply { topMargin = activity.emPx(8f) }
            setOnClickListener { Notificacoes.abrirConfiguracoesDeBateria(activity) }
        }

    // --------------------------------------------------------------- ações

    /** Salva as antecedências (lista vazia desliga), pede permissão se preciso e redesenha. */
    private fun aplicarAntecedencia(activity: Activity, container: LinearLayout, minutos: Collection<Int>) {
        PreferenciasDeAviso.definirSelecionados(activity, minutos)

        if (minutos.isNotEmpty() && Notificacoes.precisaPedirPermissao(activity)) {
            Notificacoes.pedirPermissao(activity)
        }

        AgendadorDeAvisos.reagendarEmSegundoPlano(activity)
        montar(activity, container)
    }

    // ------------------------------------------------------------- apoio

    private fun rotuloDoModo(activity: Activity, modo: ModoDeAviso): String =
        activity.getString(
            when (modo) {
                ModoDeAviso.TODAS -> R.string.modo_todas
                ModoDeAviso.CORRIDAS -> R.string.modo_corridas
                ModoDeAviso.NENHUMA -> R.string.modo_nenhuma
            }
        )

    /** Cor do texto principal no tema atual (branco no escuro, quase preto no claro). */
    private fun principal(activity: Activity): Int = activity.getColor(R.color.texto_principal)

    private fun forma(activity: Activity, preenchimento: Int, borda: Int?, raioDp: Float) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = activity.emPx(raioDp).toFloat()
            setColor(preenchimento)
            if (borda != null) setStroke(activity.emPx(1f), borda)
        }

    /** Forma arredondada com ondulação ao toque que respeita os cantos. */
    private fun comToque(activity: Activity, preenchimento: Int, borda: Int?, raioDp: Float) =
        RippleDrawable(
            ColorStateList.valueOf(Color.parseColor("#334C8DFF")),
            forma(activity, preenchimento, borda, raioDp),
            forma(activity, Color.WHITE, null, raioDp)
        )

    private fun parametros(
        activity: Activity,
        topo: Float = 0f,
        baixo: Float = 0f,
        inicio: Float = 0f
    ) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        topMargin = activity.emPx(topo)
        bottomMargin = activity.emPx(baixo)
        marginStart = activity.emPx(inicio)
    }

    private fun texto(
        activity: Activity,
        conteudo: String,
        tamanhoSp: Float,
        cor: Int,
        estilo: Int
    ) = TextView(activity).apply {
        text = conteudo
        setTextColor(cor)
        textSize = tamanhoSp
        typeface = Typeface.create("sans-serif", estilo)
    }
}

internal fun Activity.emPx(valorDp: Float): Int = (valorDp * resources.displayMetrics.density).toInt()
