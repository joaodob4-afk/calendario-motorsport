package com.motorsport.calendario

import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

/** Fundo com efeito de toque (ondulação verde) para linhas clicáveis. */
internal fun fundoDeToque(): RippleDrawable =
    RippleDrawable(
        ColorStateList.valueOf(Color.parseColor("#3339FF14")),
        null,
        ColorDrawable(Color.WHITE)
    )

private fun Activity.dp(valor: Float): Int = (valor * resources.displayMetrics.density).toInt()

private fun fundoArredondado(activity: Activity, raioDp: Float): GradientDrawable =
    GradientDrawable().apply {
        setColor(Color.parseColor("#0D1B0E"))
        cornerRadius = activity.dp(raioDp).toFloat()
        setStroke(activity.dp(1f).coerceAtLeast(1), activity.getColor(R.color.verde_borda))
    }

private fun texto(
    activity: Activity,
    conteudo: String,
    tamanhoSp: Float,
    cor: Int,
    negrito: Boolean = false
) = TextView(activity).apply {
    text = conteudo
    setTextColor(cor)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, tamanhoSp)
    typeface = Typeface.create(
        "sans-serif-condensed",
        if (negrito) Typeface.BOLD else Typeface.NORMAL
    )
}

/**
 * Menu aberto pelo botão no canto superior esquerdo da tela inicial.
 * Para incluir uma opção nova, acrescente um item em [itens].
 */
internal object MenuPrincipal {

    private class Item(
        val titulo: String,
        val detalhe: String,
        val aoTocar: () -> Unit
    )

    private fun itens(activity: Activity): List<Item> = listOf(
        Item(
            titulo = "Avisar antes das sessões",
            detalhe = PreferenciasDeAviso.descricao(PreferenciasDeAviso.minutosAntes(activity))
        ) { escolherTempoDeAviso(activity) }
    )

    fun abrir(activity: Activity, ancora: View) {
        val painel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val margem = activity.dp(6f)
            setPadding(margem, margem, margem, margem)
            background = fundoArredondado(activity, 16f)
        }

        val janela = PopupWindow(painel, activity.dp(270f), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        janela.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        janela.elevation = activity.dp(10f).toFloat()

        itens(activity).forEach { item ->
            val linha = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(activity.dp(14f), activity.dp(12f), activity.dp(14f), activity.dp(12f))
                isClickable = true
                isFocusable = true
                background = fundoDeToque()
                setOnClickListener {
                    janela.dismiss()
                    item.aoTocar()
                }
                addView(texto(activity, item.titulo, 17f, Color.WHITE, negrito = true))
                addView(texto(activity, item.detalhe, 13f, activity.getColor(R.color.cinza_texto)))
            }
            painel.addView(linha)
        }

        janela.showAsDropDown(ancora, 0, activity.dp(6f))
    }

    /** Caixa com as opções de antecedência do aviso: 1 hora até 15 minutos, ou desativado. */
    fun escolherTempoDeAviso(activity: Activity) {
        val dialogo = Dialog(activity)
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val atual = PreferenciasDeAviso.minutosAntes(activity)
        val verde = activity.getColor(R.color.verde_neon)

        val raiz = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val margem = activity.dp(20f)
            setPadding(margem, margem, margem, activity.dp(12f))
            background = fundoArredondado(activity, 20f)
        }

        raiz.addView(texto(activity, "Avisar antes das sessões", 22f, Color.WHITE, negrito = true))
        raiz.addView(
            texto(
                activity,
                "Quanto tempo antes você quer ser lembrado?",
                14f,
                activity.getColor(R.color.cinza_texto)
            ).apply { setPadding(0, activity.dp(4f), 0, activity.dp(12f)) }
        )

        val escolhas = PreferenciasDeAviso.opcoes + PreferenciasDeAviso.DESATIVADO

        escolhas.forEach { minutos ->
            val marcada = minutos == atual

            val marcador = View(activity).apply {
                layoutParams = LinearLayout.LayoutParams(activity.dp(20f), activity.dp(20f))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setStroke(activity.dp(2f), verde)
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
                setPadding(activity.dp(6f), activity.dp(13f), activity.dp(6f), activity.dp(13f))
                isClickable = true
                isFocusable = true
                background = fundoDeToque()
                setOnClickListener {
                    PreferenciasDeAviso.definir(activity, minutos)
                    dialogo.dismiss()
                }
                addView(marcador)
                addView(
                    texto(activity, rotulo, 17f, Color.WHITE, negrito = marcada).apply {
                        setPadding(activity.dp(14f), 0, 0, 0)
                    }
                )
            }

            raiz.addView(linha)
        }

        dialogo.setContentView(raiz)
        dialogo.show()
        dialogo.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((activity.resources.displayMetrics.widthPixels * 0.88f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}
