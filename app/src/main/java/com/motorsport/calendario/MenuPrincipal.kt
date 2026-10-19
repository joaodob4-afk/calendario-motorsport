package com.motorsport.calendario

import android.app.Activity
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
import android.widget.Switch
import android.widget.TextView

/** Fundo com efeito de toque (ondulação verde) para linhas clicáveis. */
internal fun fundoDeToque(): RippleDrawable =
    RippleDrawable(
        ColorStateList.valueOf(Color.parseColor("#334CAF50")),
        null,
        ColorDrawable(Color.WHITE)
    )

/**
 * Conteúdo do painel que abre pelo lado da tela inicial (botão no canto superior esquerdo).
 *
 * Visual compacto: cartão geral (liga/desliga), antecedência em botões arredondados,
 * aviso na hora de começar e uma linha por categoria que abre para escolher o que avisar.
 * Para incluir uma função nova no futuro, crie um bloco novo e chame-o em [montar].
 */
internal object MenuPrincipal {

    private val corDoTextoSobreCor = Color.parseColor("#0E1012")
    private val corDoAviso = Color.parseColor("#FFB74D")
    private val corFinalDoDegrade = Color.parseColor("#2DD4BF")
    private val corDoContorno = Color.parseColor("#3A3F45")

    /** Categoria com as opções abertas (uma por vez); null quando todas estão fechadas. */
    private var categoriaAberta: String? = null

    /** Fecha a linha de categoria que estiver aberta (usado ao reconstruir o painel e nos testes). */
    internal fun reiniciar() {
        categoriaAberta = null
    }

    fun montar(activity: Activity, container: LinearLayout) {
        container.removeAllViews()

        val minutos = PreferenciasDeAviso.minutosAntes(activity)
        val ativo = minutos != PreferenciasDeAviso.DESATIVADO

        container.addView(cabecalho(activity))
        container.addView(cartaoGeral(activity, container, ativo))

        container.addView(tituloDeSecao(activity, R.string.antecedencia_titulo, topo = 18f, baixo = 10f))
        container.addView(botoesDeAntecedencia(activity, container, minutos))
        container.addView(linhaNaHora(activity, container, ativo))
        container.addView(divisoria(activity))

        container.addView(tituloDeSecao(activity, R.string.por_categoria_titulo, topo = 14f, baixo = 8f))
        Categorias.todas.forEach { categoria ->
            container.addView(linhaDaCategoria(activity, container, categoria, ativo))
        }

        if (ativo && !Notificacoes.permitidas(activity)) {
            container.addView(cartaoDeBloqueio(activity))
        }

        container.addView(botaoDeTeste(activity))
        container.addView(botaoDeBateria(activity))
    }

    // ---------------------------------------------------------------- blocos

    private fun cabecalho(activity: Activity): View {
        val icone = ImageView(activity).apply {
            setImageResource(R.drawable.ic_sino)
            setColorFilter(activity.getColor(R.color.acento))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setPadding(activity.emPx(8f), activity.emPx(8f), activity.emPx(8f), activity.emPx(8f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(activity.getColor(R.color.acento_escuro))
                setStroke(activity.emPx(1f), activity.getColor(R.color.verde_borda))
            }
            layoutParams = LinearLayout.LayoutParams(activity.emPx(38f), activity.emPx(38f))
        }

        val titulo = texto(activity, activity.getString(R.string.menu_titulo), 22f, Color.WHITE, Typeface.BOLD).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = activity.emPx(12f) }
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
        texto(activity, activity.getString(textoRes), 16f, Color.WHITE, Typeface.BOLD).apply {
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

    private fun divisoria(activity: Activity) = View(activity).apply {
        setBackgroundColor(activity.getColor(R.color.borda_suave))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            activity.emPx(1f)
        ).apply { topMargin = activity.emPx(4f) }
    }

    /** Interruptor só para exibir o estado; o toque é tratado pela linha inteira. */
    private fun interruptor(activity: Activity, ligado: Boolean) = Switch(activity).apply {
        isChecked = ligado
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        val estados = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        thumbTintList = ColorStateList(
            estados,
            intArrayOf(activity.getColor(R.color.acento), Color.parseColor("#B0B6BC"))
        )
        trackTintList = ColorStateList(
            estados,
            intArrayOf(Color.parseColor("#664CAF50"), Color.parseColor("#33FFFFFF"))
        )
    }

    /** "Geral": liga ou desliga todos os avisos. */
    private fun cartaoGeral(activity: Activity, container: LinearLayout, ativo: Boolean): View {
        val suave = activity.getColor(R.color.texto_suave)

        val titulos = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(
                texto(
                    activity,
                    activity.getString(if (ativo) R.string.avisos_ligados else R.string.avisos_desligados),
                    16f,
                    Color.WHITE,
                    Typeface.BOLD
                )
            )
            addView(
                texto(activity, activity.getString(R.string.avisos_ligados_desc), 13f, suave, Typeface.NORMAL)
                    .apply { setPadding(0, activity.emPx(2f), 0, 0) }
            )
        }

        val linha = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            contentDescription = activity.getString(
                if (ativo) R.string.cd_desligar_avisos else R.string.cd_ligar_avisos
            )
            background = comToque(activity, Color.TRANSPARENT, null, 10f)
            layoutParams = parametros(activity, topo = 8f)
            addView(titulos)
            addView(interruptor(activity, ativo))
            setOnClickListener {
                val novo = if (ativo) {
                    PreferenciasDeAviso.DESATIVADO
                } else {
                    PreferenciasDeAviso.ultimaAntecedencia(activity)
                }
                aplicarAntecedencia(activity, container, novo)
            }
        }

        return cartao(activity).apply {
            addView(texto(activity, activity.getString(R.string.geral_titulo), 14f, Color.WHITE, Typeface.BOLD))
            addView(linha)
        }
    }

    /** Antecedência: quatro botões arredondados; o escolhido ganha o degradê verde. */
    private fun botoesDeAntecedencia(activity: Activity, container: LinearLayout, minutos: Int): View {
        val ativo = minutos != PreferenciasDeAviso.DESATIVADO

        // Com os avisos desligados, a antecedência lembrada aparece esmaecida;
        // tocar em outra opção também religa os avisos.
        val marcada = if (ativo) minutos else PreferenciasDeAviso.ultimaAntecedencia(activity)

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = parametros(activity)
            alpha = if (ativo) 1f else 0.45f

            PreferenciasDeAviso.opcoes.reversed().forEachIndexed { indice, opcao ->
                val selecionada = opcao == marcada
                val rotulo = if (opcao == 60) {
                    activity.getString(R.string.antecedencia_chip_hora)
                } else {
                    "$opcao min"
                }

                addView(
                    texto(
                        activity,
                        rotulo,
                        14f,
                        if (selecionada) corDoTextoSobreCor else Color.WHITE,
                        if (selecionada) Typeface.BOLD else Typeface.NORMAL
                    ).apply {
                        gravity = Gravity.CENTER
                        contentDescription = descricaoDaAntecedencia(activity, opcao)
                        isClickable = true
                        isFocusable = true
                        background = if (selecionada) {
                            comDegrade(activity, 20f)
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
                        setOnClickListener { aplicarAntecedencia(activity, container, opcao) }
                    }
                )
            }
        }
    }

    /** Liga/desliga o aviso extra "começando agora", na hora exata da sessão. */
    private fun linhaNaHora(activity: Activity, container: LinearLayout, ativo: Boolean): View {
        val ligado = PreferenciasDeAviso.avisarNaHora(activity)

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = activity.emPx(48f)
            isClickable = true
            isFocusable = true
            alpha = if (ativo) 1f else 0.45f
            contentDescription = activity.getString(
                if (ligado) R.string.cd_desligar_na_hora else R.string.cd_ligar_na_hora
            )
            background = comToque(activity, Color.TRANSPARENT, null, 10f)
            layoutParams = parametros(activity, topo = 8f)
            addView(
                texto(activity, activity.getString(R.string.na_hora_curto), 15f, Color.WHITE, Typeface.NORMAL)
                    .apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            )
            addView(interruptor(activity, ligado))
            setOnClickListener {
                PreferenciasDeAviso.definirAvisarNaHora(activity, !ligado)
                AgendadorDeAvisos.reagendarEmSegundoPlano(activity)
                montar(activity, container)
            }
        }
    }

    /**
     * Uma linha por categoria: selo colorido, nome, o que está escolhido e uma seta.
     * Tocar na linha abre as três opções (todas, só corridas ou nenhuma).
     */
    private fun linhaDaCategoria(
        activity: Activity,
        container: LinearLayout,
        categoria: CategoriaInfo,
        ativo: Boolean
    ): View {
        val suave = activity.getColor(R.color.texto_suave)
        val modoAtual = PreferenciasDeAviso.modoDaCategoria(activity, categoria.nome)
        val aberta = categoriaAberta == categoria.nome
        val resumo = resumoDoModo(activity, modoAtual)

        val selo = texto(activity, seloDaCategoria(categoria.nome), 12f, corDoTextoSobreCor, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            background = forma(activity, Color.parseColor(categoria.cor), null, 14f)
            layoutParams = LinearLayout.LayoutParams(activity.emPx(56f), activity.emPx(28f))
        }

        val nome = texto(activity, categoria.nome, 15f, Color.WHITE, Typeface.BOLD).apply {
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = activity.emPx(12f) }
        }

        val seta = ImageView(activity).apply {
            setImageResource(R.drawable.ic_seta_baixo)
            setColorFilter(suave)
            rotation = if (aberta) 180f else 0f
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = LinearLayout.LayoutParams(activity.emPx(20f), activity.emPx(20f))
                .apply { marginStart = activity.emPx(6f) }
        }

        val cabecalhoDaLinha = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = activity.emPx(44f)
            setPadding(activity.emPx(8f), activity.emPx(4f), activity.emPx(10f), activity.emPx(4f))
            isClickable = true
            isFocusable = true
            contentDescription = "${categoria.nome}, $resumo"
            background = comToque(activity, Color.TRANSPARENT, null, 12f)
            addView(selo)
            addView(nome)
            addView(texto(activity, resumo, 13f, suave, Typeface.NORMAL))
            addView(seta)
            setOnClickListener {
                categoriaAberta = if (aberta) null else categoria.nome
                montar(activity, container)
            }
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = forma(
                activity,
                activity.getColor(R.color.superficie),
                activity.getColor(R.color.borda_suave),
                12f
            )
            // Esmaecido quando os avisos estão desligados; as escolhas continuam valendo.
            alpha = if (ativo) 1f else 0.45f
            layoutParams = parametros(activity, baixo = 6f)
            addView(cabecalhoDaLinha)

            if (aberta) {
                addView(
                    seletorDeModo(activity, modoAtual, categoria.cor) { novo ->
                        PreferenciasDeAviso.definirModo(activity, categoria.nome, novo)
                        AgendadorDeAvisos.reagendarEmSegundoPlano(activity)
                        categoriaAberta = null
                        montar(activity, container)
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            activity.emPx(40f)
                        ).apply {
                            marginStart = activity.emPx(10f)
                            marginEnd = activity.emPx(10f)
                            topMargin = activity.emPx(2f)
                        }
                    }
                )
                addView(
                    texto(activity, activity.getString(R.string.campeonato_legenda), 12f, suave, Typeface.NORMAL)
                        .apply {
                            setLineSpacing(0f, 1.15f)
                            layoutParams = parametros(activity, topo = 8f, baixo = 10f).apply {
                                marginStart = activity.emPx(12f)
                                marginEnd = activity.emPx(12f)
                            }
                        }
                )
            }
        }
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
            setPadding(activity.emPx(3f), activity.emPx(3f), activity.emPx(3f), activity.emPx(3f))

            ModoDeAviso.values().forEach { modo ->
                val marcado = modo == atual
                val corMarcada = if (modo == ModoDeAviso.NENHUMA) {
                    Color.parseColor("#3A3F45")
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
                        13f,
                        corDoTexto,
                        if (marcado) Typeface.BOLD else Typeface.NORMAL
                    ).apply {
                        gravity = Gravity.CENTER
                        isClickable = true
                        isFocusable = true
                        contentDescription = rotuloDoModo(activity, modo)
                        background = if (marcado) {
                            comToque(activity, corMarcada, null, 9f)
                        } else {
                            comToque(activity, Color.TRANSPARENT, null, 9f)
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1f
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
            setColorFilter(Color.WHITE)
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
            background = comToque(activity, Color.TRANSPARENT, corDoContorno, 14f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.emPx(46f)
            ).apply { topMargin = activity.emPx(14f) }
            addView(sino)
            addView(texto(activity, activity.getString(R.string.botao_teste), 15f, Color.WHITE, Typeface.BOLD))
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

    /** Atalho para quem para de receber avisos: o Android ou o fabricante pode estar "dormindo" o app. */
    private fun botaoDeBateria(activity: Activity): View =
        texto(
            activity,
            activity.getString(R.string.botao_bateria),
            12f,
            activity.getColor(R.color.texto_suave),
            Typeface.NORMAL
        ).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = comToque(activity, Color.TRANSPARENT, null, 10f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.emPx(40f)
            ).apply { topMargin = activity.emPx(4f) }
            setOnClickListener { Notificacoes.abrirConfiguracoesDeBateria(activity) }
        }

    // --------------------------------------------------------------- ações

    /** Salva a antecedência (ou desliga, com zero), pede permissão se preciso e redesenha. */
    private fun aplicarAntecedencia(activity: Activity, container: LinearLayout, minutos: Int) {
        PreferenciasDeAviso.definir(activity, minutos)

        if (minutos != PreferenciasDeAviso.DESATIVADO &&
            Notificacoes.precisaPedirPermissao(activity)
        ) {
            Notificacoes.pedirPermissao(activity)
        }

        AgendadorDeAvisos.reagendarEmSegundoPlano(activity)
        montar(activity, container)
    }

    // ------------------------------------------------------------- apoio

    /** "30 minutos antes da sessão" / "30 minutes before the session". */
    private fun descricaoDaAntecedencia(activity: Activity, minutos: Int): String =
        if (minutos == 60) {
            activity.getString(R.string.antecedencia_hora)
        } else {
            activity.getString(R.string.antecedencia_minutos, minutos)
        }

    /** Texto curto da linha da categoria: "Todas", "Só corridas" ou "Nenhuma". */
    private fun resumoDoModo(activity: Activity, modo: ModoDeAviso): String =
        activity.getString(
            when (modo) {
                ModoDeAviso.TODAS -> R.string.modo_todas
                ModoDeAviso.CORRIDAS -> R.string.modo_so_corridas
                ModoDeAviso.NENHUMA -> R.string.modo_nenhuma
            }
        )

    /** Texto do selo colorido; nomes longos usam uma abreviação para caber. */
    private fun seloDaCategoria(nome: String): String =
        when (Formatacao.normalizar(nome)) {
            "formula e" -> "FE"
            "indycar" -> "INDY"
            else -> nome.uppercase()
        }

    private fun rotuloDoModo(activity: Activity, modo: ModoDeAviso): String =
        activity.getString(
            when (modo) {
                ModoDeAviso.TODAS -> R.string.modo_todas
                ModoDeAviso.CORRIDAS -> R.string.modo_corridas
                ModoDeAviso.NENHUMA -> R.string.modo_nenhuma
            }
        )

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
            ColorStateList.valueOf(Color.parseColor("#334CAF50")),
            forma(activity, preenchimento, borda, raioDp),
            forma(activity, Color.WHITE, null, raioDp)
        )

    /** Botão arredondado com degradê verde e ondulação ao toque. */
    private fun comDegrade(activity: Activity, raioDp: Float) =
        RippleDrawable(
            ColorStateList.valueOf(Color.parseColor("#33FFFFFF")),
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(activity.getColor(R.color.acento), corFinalDoDegrade)
            ).apply { cornerRadius = activity.emPx(raioDp).toFloat() },
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
