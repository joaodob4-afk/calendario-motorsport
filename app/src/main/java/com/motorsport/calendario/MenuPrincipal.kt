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
import android.widget.LinearLayout
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
 * Cada função de seção acrescenta um bloco ao painel. Para incluir uma função nova no
 * futuro, crie uma seção nova e chame-a em [montar].
 */
internal object MenuPrincipal {

    fun montar(activity: Activity, container: LinearLayout) {
        container.removeAllViews()

        container.addView(
            texto(activity, "Menu", 26f, Color.WHITE, Typeface.BOLD)
        )

        secaoAlertas(activity, container)

        container.addView(
            texto(
                activity,
                "Mais funções em breve.",
                14f,
                activity.getColor(R.color.texto_suave),
                Typeface.NORMAL
            ).apply { setPadding(0, activity.emPx(28f), 0, 0) }
        )
    }

    /** Escolha de quanto tempo antes das sessões o usuário quer ser avisado. */
    private fun secaoAlertas(activity: Activity, container: LinearLayout) {
        container.addView(
            texto(
                activity,
                "ALERTAS",
                12f,
                activity.getColor(R.color.texto_suave),
                Typeface.NORMAL
            ).apply {
                letterSpacing = 0.14f
                setPadding(0, activity.emPx(28f), 0, activity.emPx(12f))
            }
        )

        container.addView(texto(activity, "Avisar antes das sessões", 18f, Color.WHITE, Typeface.BOLD))
        container.addView(
            texto(
                activity,
                "Escolha com quanto tempo de antecedência você quer ser lembrado.",
                14f,
                activity.getColor(R.color.texto_suave),
                Typeface.NORMAL
            ).apply { setPadding(0, activity.emPx(4f), 0, activity.emPx(10f)) }
        )

        val atual = PreferenciasDeAviso.minutosAntes(activity)
        val verde = activity.getColor(R.color.acento)

        (PreferenciasDeAviso.opcoes + PreferenciasDeAviso.DESATIVADO).forEach { minutos ->
            val marcada = minutos == atual

            val marcador = View(activity).apply {
                layoutParams = LinearLayout.LayoutParams(activity.emPx(20f), activity.emPx(20f))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setStroke(activity.emPx(2f), if (marcada) verde else activity.getColor(R.color.texto_suave))
                    setColor(if (marcada) verde else Color.TRANSPARENT)
                }
            }

            val rotulo = if (minutos == PreferenciasDeAviso.DESATIVADO) {
                "Não avisar"
            } else {
                PreferenciasDeAviso.descricao(minutos)
            }

            val linha = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(activity.emPx(6f), activity.emPx(13f), activity.emPx(6f), activity.emPx(13f))
                isClickable = true
                isFocusable = true
                background = fundoDeToque()
                setOnClickListener {
                    PreferenciasDeAviso.definir(activity, minutos)

                    if (minutos != PreferenciasDeAviso.DESATIVADO &&
                        Notificacoes.precisaPedirPermissao(activity)
                    ) {
                        Notificacoes.pedirPermissao(activity)
                    }

                    AgendadorDeAvisos.reagendarEmSegundoPlano(activity)
                    montar(activity, container) // redesenha com a nova escolha marcada
                }
                addView(marcador)
                addView(
                    texto(
                        activity,
                        rotulo,
                        17f,
                        Color.WHITE,
                        if (marcada) Typeface.BOLD else Typeface.NORMAL
                    ).apply { setPadding(activity.emPx(14f), 0, 0, 0) }
                )
            }

            container.addView(linha)
        }

        if (atual != PreferenciasDeAviso.DESATIVADO) {
            secaoTipoDeSessao(activity, container)
        }

        // Escolheu um aviso, mas as notificações estão bloqueadas no aparelho.
        if (atual != PreferenciasDeAviso.DESATIVADO && !Notificacoes.permitidas(activity)) {
            container.addView(
                linhaDeAcao(
                    activity,
                    "As notificações estão bloqueadas. Toque para ativar nas configurações.",
                    Color.parseColor("#FFB74D")
                ) {
                    if (Notificacoes.precisaPedirPermissao(activity)) {
                        Notificacoes.pedirPermissao(activity)
                    } else {
                        Notificacoes.abrirConfiguracoes(activity)
                    }
                }
            )
        }

        container.addView(
            linhaDeAcao(activity, "Enviar notificação de teste", verde) {
                when {
                    Notificacoes.precisaPedirPermissao(activity) ->
                        Notificacoes.pedirPermissao(activity)

                    !Notificacoes.permitidas(activity) ->
                        Notificacoes.abrirConfiguracoes(activity)

                    else -> Notificacoes.mostrarTeste(activity)
                }
            }
        )
    }

    /** Escolha entre avisar de todas as sessões ou só das corridas. */
    private fun secaoTipoDeSessao(activity: Activity, container: LinearLayout) {
        container.addView(
            texto(activity, "Quais sessões avisar", 18f, Color.WHITE, Typeface.BOLD)
                .apply { setPadding(0, activity.emPx(28f), 0, activity.emPx(6f)) }
        )

        val apenasCorridas = PreferenciasDeAviso.apenasCorridas(activity)

        listOf(false to "Todas as sessões", true to "Apenas as corridas").forEach { (soCorridas, rotulo) ->
            container.addView(
                linhaDeOpcao(activity, rotulo, soCorridas == apenasCorridas) {
                    PreferenciasDeAviso.definirApenasCorridas(activity, soCorridas)
                    AgendadorDeAvisos.reagendarEmSegundoPlano(activity)
                    montar(activity, container)
                }
            )
        }

        container.addView(
            texto(
                activity,
                if (apenasCorridas) {
                    "Sprints e corridas principais. Treinos e classificações ficam de fora."
                } else {
                    "Treinos, classificações e corridas."
                },
                13f,
                activity.getColor(R.color.texto_suave),
                Typeface.NORMAL
            ).apply { setPadding(activity.emPx(6f), activity.emPx(4f), 0, 0) }
        )
    }

    /** Linha com marcador redondo, igual às opções de antecedência. */
    private fun linhaDeOpcao(
        activity: Activity,
        rotulo: String,
        marcada: Boolean,
        aoTocar: () -> Unit
    ): LinearLayout {
        val verde = activity.getColor(R.color.acento)

        val marcador = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams(activity.emPx(20f), activity.emPx(20f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(activity.emPx(2f), if (marcada) verde else activity.getColor(R.color.texto_suave))
                setColor(if (marcada) verde else Color.TRANSPARENT)
            }
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(activity.emPx(6f), activity.emPx(13f), activity.emPx(6f), activity.emPx(13f))
            isClickable = true
            isFocusable = true
            background = fundoDeToque()
            setOnClickListener { aoTocar() }
            addView(marcador)
            addView(
                texto(
                    activity,
                    rotulo,
                    17f,
                    Color.WHITE,
                    if (marcada) Typeface.BOLD else Typeface.NORMAL
                ).apply { setPadding(activity.emPx(14f), 0, 0, 0) }
            )
        }
    }

    /** Linha de texto clicável, com o mesmo efeito de toque das opções. */
    private fun linhaDeAcao(
        activity: Activity,
        rotulo: String,
        cor: Int,
        aoTocar: () -> Unit
    ) = texto(activity, rotulo, 15f, cor, Typeface.BOLD).apply {
        setPadding(activity.emPx(6f), activity.emPx(16f), activity.emPx(6f), activity.emPx(16f))
        isClickable = true
        isFocusable = true
        background = fundoDeToque()
        setOnClickListener { aoTocar() }
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
