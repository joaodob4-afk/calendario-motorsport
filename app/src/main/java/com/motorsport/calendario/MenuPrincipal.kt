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
 * Ordem: alertar na hora de começar, o botão "Personalizar alertas" (antecedências e opções por
 * categoria ficam na página que ele abre), envio de teste e atalho da bateria. Só os botões e
 * o interruptor respondem ao toque.
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
    }

    fun montar(activity: Activity, container: LinearLayout) {
        container.removeAllViews()

        val selecionados = PreferenciasDeAviso.minutosSelecionados(activity)
        val naHora = PreferenciasDeAviso.avisarNaHora(activity)

        // Há alerta ligado se existir alguma antecedência escolhida ou o aviso da hora de começar.
        val ativo = naHora || selecionados.isNotEmpty()

        container.addView(cabecalho(activity))

        container.addView(cartaoNaHora(activity, container, naHora))

        container.addView(botaoPersonalizar(activity, selecionados))

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
                setColor(activity.getColor(R.color.azul_marinho_claro))
                cornerRadius = activity.emPx(8f).toFloat()
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
            12f
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
     * "Personalizar alertas": abre a página com as antecedências e as opções por categoria.
     * Mostra quantas antecedências estão marcadas.
     */
    private fun botaoPersonalizar(activity: Activity, selecionados: List<Int>): View {
        val extras = selecionados.size

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
            background = comToque(activity, Color.TRANSPARENT, activity.getColor(R.color.borda_suave), 8f)
            setPadding(activity.emPx(16f), 0, activity.emPx(14f), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.emPx(46f)
            ).apply { topMargin = activity.emPx(18f) }

            addView(rotulo)

            if (extras > 0) {
                addView(
                    texto(activity, "+$extras", 12f, corDoTextoSobreCor, Typeface.BOLD).apply {
                        gravity = Gravity.CENTER
                        minWidth = activity.emPx(24f)
                        setPadding(activity.emPx(7f), activity.emPx(2f), activity.emPx(7f), activity.emPx(2f))
                        background = forma(activity, activity.getColor(R.color.azul_destaque), null, 6f)
                    }
                )
            }

            setOnClickListener {
                // Transição padrão do Android, tanto ao abrir quanto ao sair.
                activity.startActivity(Intent(activity, PersonalizarAlertasActivity::class.java))
            }
        }
    }

    /** Aparece quando há avisos ligados, mas o Android bloqueia as notificações do app. */
    private fun cartaoDeBloqueio(activity: Activity): View =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            background = comToque(activity, Color.parseColor("#2A2113"), corDoAviso, 8f)
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
            background = comToque(activity, Color.TRANSPARENT, activity.getColor(R.color.borda_suave), 8f)
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
            background = comToque(activity, Color.TRANSPARENT, activity.getColor(R.color.borda_suave), 8f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.emPx(44f)
            ).apply { topMargin = activity.emPx(8f) }
            setOnClickListener { Notificacoes.abrirConfiguracoesDeBateria(activity) }
        }

    // --------------------------------------------------------------- ações

    // ------------------------------------------------------------- apoio

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
