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
 * O painel é montado em blocos: cabeçalho, cartão de lembretes, cartão por campeonato,
 * aviso de bloqueio, botão de teste e rodapé. Para incluir uma função nova no futuro,
 * crie um bloco novo e chame-o em [montar].
 */
internal object MenuPrincipal {

    private val corDoTextoSobreCor = Color.parseColor("#0E1012")
    private val corDoAviso = Color.parseColor("#FFB74D")

    fun montar(activity: Activity, container: LinearLayout) {
        container.removeAllViews()

        val minutos = PreferenciasDeAviso.minutosAntes(activity)
        val ativo = minutos != PreferenciasDeAviso.DESATIVADO

        container.addView(cabecalho(activity))

        container.addView(rotuloDeSecao(activity, activity.getString(R.string.menu_secao_lembretes)))
        container.addView(cartaoDeLembretes(activity, container, minutos))

        container.addView(rotuloDeSecao(activity, activity.getString(R.string.menu_secao_campeonato)))
        container.addView(cartaoPorCampeonato(activity, container, ativo))

        if (ativo && !Notificacoes.permitidas(activity)) {
            container.addView(cartaoDeBloqueio(activity))
        }

        container.addView(botaoDeTeste(activity))
        container.addView(botaoDeBateria(activity))

        container.addView(
            texto(
                activity,
                activity.getString(R.string.menu_em_breve),
                13f,
                activity.getColor(R.color.texto_suave),
                Typeface.NORMAL
            ).apply {
                gravity = Gravity.CENTER
                layoutParams = parametros(activity, topo = 28f)
            }
        )
    }

    // ---------------------------------------------------------------- blocos

    private fun cabecalho(activity: Activity): View {
        val icone = ImageView(activity).apply {
            setImageResource(R.drawable.ic_notificacao)
            setColorFilter(activity.getColor(R.color.acento))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setPadding(activity.emPx(10f), activity.emPx(10f), activity.emPx(10f), activity.emPx(10f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(activity.getColor(R.color.acento_escuro))
                setStroke(activity.emPx(1f), activity.getColor(R.color.verde_borda))
            }
            layoutParams = LinearLayout.LayoutParams(activity.emPx(46f), activity.emPx(46f))
        }

        val textos = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = parametros(activity, inicio = 14f)
            addView(
                texto(activity, activity.getString(R.string.menu_marca), 11f, activity.getColor(R.color.texto_suave), Typeface.NORMAL)
                    .apply { letterSpacing = 0.12f }
            )
            addView(texto(activity, activity.getString(R.string.menu_titulo), 24f, Color.WHITE, Typeface.BOLD))
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(icone)
            addView(textos)
        }
    }

    private fun rotuloDeSecao(activity: Activity, titulo: String) =
        texto(activity, titulo, 12f, activity.getColor(R.color.texto_suave), Typeface.NORMAL).apply {
            letterSpacing = 0.14f
            layoutParams = parametros(activity, topo = 28f, baixo = 10f, inicio = 4f)
        }

    private fun cartao(activity: Activity) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        background = forma(
            activity,
            activity.getColor(R.color.superficie),
            activity.getColor(R.color.borda_suave),
            16f
        )
        setPadding(activity.emPx(16f), activity.emPx(16f), activity.emPx(16f), activity.emPx(16f))
        layoutParams = parametros(activity)
    }

    /** Liga/desliga os avisos e escolhe a antecedência. */
    private fun cartaoDeLembretes(activity: Activity, container: LinearLayout, minutos: Int): View {
        val ativo = minutos != PreferenciasDeAviso.DESATIVADO
        val verde = activity.getColor(R.color.acento)
        val suave = activity.getColor(R.color.texto_suave)

        val chave = Switch(activity).apply {
            isChecked = ativo
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            val estados = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(estados, intArrayOf(verde, Color.parseColor("#B0B6BC")))
            trackTintList = ColorStateList(
                estados,
                intArrayOf(Color.parseColor("#664CAF50"), Color.parseColor("#33FFFFFF"))
            )
        }

        val titulos = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(texto(activity, activity.getString(R.string.lembrete_titulo), 16f, Color.WHITE, Typeface.BOLD))
            addView(
                texto(
                    activity,
                    if (ativo) descricaoDaAntecedencia(activity, minutos) else activity.getString(R.string.desligado),
                    13f,
                    if (ativo) verde else suave,
                    Typeface.NORMAL
                ).apply { setPadding(0, activity.emPx(2f), 0, 0) }
            )
        }

        val linhaPrincipal = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            contentDescription = if (ativo) activity.getString(R.string.cd_desligar_avisos) else activity.getString(R.string.cd_ligar_avisos)
            background = comToque(activity, Color.TRANSPARENT, null, 10f)
            addView(titulos)
            addView(chave)
            setOnClickListener {
                val novo = if (ativo) {
                    PreferenciasDeAviso.DESATIVADO
                } else {
                    PreferenciasDeAviso.ultimaAntecedencia(activity)
                }
                aplicarAntecedencia(activity, container, novo)
            }
        }

        // Com os avisos desligados, a antecedência lembrada aparece esmaecida;
        // tocar em outra opção também religa os avisos.
        val marcada = if (ativo) minutos else PreferenciasDeAviso.ultimaAntecedencia(activity)

        val chips = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = parametros(activity, topo = 14f)
            alpha = if (ativo) 1f else 0.45f

            PreferenciasDeAviso.opcoes.reversed().forEachIndexed { indice, opcao ->
                val selecionada = opcao == marcada
                val rotulo = if (opcao == 60) "1 h" else "$opcao min"

                addView(
                    texto(
                        activity,
                        rotulo,
                        13f,
                        if (selecionada) activity.getColor(R.color.acento_escuro) else suave,
                        if (selecionada) Typeface.BOLD else Typeface.NORMAL
                    ).apply {
                        gravity = Gravity.CENTER
                        contentDescription = descricaoDaAntecedencia(activity, opcao)
                        isClickable = true
                        isFocusable = true
                        background = if (selecionada) {
                            comToque(activity, verde, null, 10f)
                        } else {
                            comToque(
                                activity,
                                activity.getColor(R.color.fundo_tela),
                                activity.getColor(R.color.borda_suave),
                                10f
                            )
                        }
                        layoutParams = LinearLayout.LayoutParams(0, activity.emPx(38f), 1f).apply {
                            if (indice > 0) marginStart = activity.emPx(6f)
                        }
                        setOnClickListener { aplicarAntecedencia(activity, container, opcao) }
                    }
                )
            }
        }

        return cartao(activity).apply {
            addView(linhaPrincipal)
            addView(chips)
            addView(linhaNaHora(activity, container, ativo))
        }
    }

    /** Liga/desliga o aviso extra "começando agora", na hora exata da sessão. */
    private fun linhaNaHora(activity: Activity, container: LinearLayout, ativo: Boolean): View {
        val verde = activity.getColor(R.color.acento)
        val suave = activity.getColor(R.color.texto_suave)
        val ligado = PreferenciasDeAviso.avisarNaHora(activity)

        val chave = Switch(activity).apply {
            isChecked = ligado
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            val estados = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(estados, intArrayOf(verde, Color.parseColor("#B0B6BC")))
            trackTintList = ColorStateList(
                estados,
                intArrayOf(Color.parseColor("#664CAF50"), Color.parseColor("#33FFFFFF"))
            )
        }

        val titulos = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(texto(activity, activity.getString(R.string.na_hora_titulo), 15f, Color.WHITE, Typeface.BOLD))
            addView(
                texto(
                    activity,
                    if (ligado) activity.getString(R.string.na_hora_ligado) else activity.getString(R.string.desligado),
                    13f,
                    if (ligado) verde else suave,
                    Typeface.NORMAL
                ).apply { setPadding(0, activity.emPx(2f), 0, 0) }
            )
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            alpha = if (ativo) 1f else 0.45f
            contentDescription = if (ligado) activity.getString(R.string.cd_desligar_na_hora) else activity.getString(R.string.cd_ligar_na_hora)
            background = comToque(activity, Color.TRANSPARENT, null, 10f)
            layoutParams = parametros(activity, topo = 16f)
            addView(titulos)
            addView(chave)
            setOnClickListener {
                PreferenciasDeAviso.definirAvisarNaHora(activity, !ligado)
                AgendadorDeAvisos.reagendarEmSegundoPlano(activity)
                montar(activity, container)
            }
        }
    }

    /** Para cada campeonato: avisar de todas as sessões, só das corridas ou de nenhuma. */
    private fun cartaoPorCampeonato(activity: Activity, container: LinearLayout, ativo: Boolean): View {
        val suave = activity.getColor(R.color.texto_suave)

        return cartao(activity).apply {
            // Esmaecido quando os avisos estão desligados; as escolhas continuam valendo.
            alpha = if (ativo) 1f else 0.45f

            addView(texto(activity, activity.getString(R.string.campeonato_intro), 13f, suave, Typeface.NORMAL))

            Categorias.todas.forEach { categoria ->
                val modoAtual = PreferenciasDeAviso.modoDaCategoria(activity, categoria.nome)

                val ponto = View(activity).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(categoria.cor))
                    }
                    layoutParams = LinearLayout.LayoutParams(activity.emPx(10f), activity.emPx(10f))
                }

                val nome = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = parametros(activity, topo = 16f, baixo = 8f)
                    addView(ponto)
                    addView(
                        texto(activity, categoria.nome, 15f, Color.WHITE, Typeface.BOLD)
                            .apply { setPadding(activity.emPx(10f), 0, 0, 0) }
                    )
                }

                addView(nome)
                addView(
                    seletorDeModo(activity, modoAtual, categoria.cor) { novo ->
                        PreferenciasDeAviso.definirModo(activity, categoria.nome, novo)
                        AgendadorDeAvisos.reagendarEmSegundoPlano(activity)
                        montar(activity, container)
                    }
                )
            }

            addView(
                texto(
                    activity,
                    activity.getString(R.string.campeonato_legenda),
                    12f,
                    suave,
                    Typeface.NORMAL
                ).apply {
                    setLineSpacing(0f, 1.15f)
                    layoutParams = parametros(activity, topo = 16f)
                }
            )
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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.emPx(40f)
            )

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
        val verde = activity.getColor(R.color.acento)

        return texto(activity, activity.getString(R.string.botao_teste), 15f, verde, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = comToque(activity, Color.TRANSPARENT, verde, 14f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.emPx(48f)
            ).apply { topMargin = activity.emPx(20f) }
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
            13f,
            activity.getColor(R.color.texto_suave),
            Typeface.NORMAL
        ).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = comToque(activity, Color.TRANSPARENT, null, 10f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.emPx(44f)
            ).apply { topMargin = activity.emPx(8f) }
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
