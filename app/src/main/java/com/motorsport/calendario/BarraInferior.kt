package com.motorsport.calendario

import android.app.Activity
import androidx.annotation.StringRes
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

internal data class ItemDaBarra(
    /** Identifica o item (usado nos testes); o texto exibido vem de [rotuloRes]. */
    val rotulo: String,
    @StringRes val rotuloRes: Int,
    val icone: Int,
    /** false: o item ainda é só uma ideia e mostra "em breve" ao tocar. */
    val pronto: Boolean = false
)

/**
 * Barra fixa no pé da tela inicial.
 *
 * Os itens abaixo são ideias para testar o visual. Para experimentar outras, troque,
 * inclua ou remova linhas de [itens]: a barra se ajusta sozinha. Quando uma ideia virar
 * função de verdade, marque `pronto = true` e trate o toque em [aoTocar].
 */
internal object BarraInferior {

    val itens = listOf(
        ItemDaBarra("Início", R.string.barra_inicio, R.drawable.ic_nav_inicio, pronto = true),
        ItemDaBarra("Calendário", R.string.barra_calendario, R.drawable.ic_nav_agenda),
        ItemDaBarra("Classificação", R.string.barra_classificacao, R.drawable.ic_nav_resultados),
        ItemDaBarra("Mais", R.string.barra_mais, R.drawable.ic_nav_mais)
    )

    fun montar(activity: Activity, container: LinearLayout, selecionado: Int = 0) {
        container.removeAllViews()

        // Fundo azul-marinho: o item aberto fica branco, com uma pílula azul atrás do ícone.
        val corAtiva = android.graphics.Color.WHITE
        val corInativa = activity.getColor(R.color.azul_icone_inativo)

        itens.forEachIndexed { indice, item ->
            val cor = if (indice == selecionado) corAtiva else corInativa

            val icone = ImageView(activity).apply {
                setImageResource(item.icone)
                setColorFilter(cor)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                layoutParams = FrameLayout.LayoutParams(
                    activity.emPx(24f),
                    activity.emPx(24f),
                    Gravity.CENTER
                )
            }

            val pilula = FrameLayout(activity).apply {
                layoutParams = LinearLayout.LayoutParams(activity.emPx(56f), activity.emPx(30f))
                if (indice == selecionado) {
                    background = GradientDrawable().apply {
                        setColor(activity.getColor(R.color.azul_marinho_claro))
                        cornerRadius = activity.emPx(15f).toFloat()
                    }
                }
                addView(icone)
            }

            val rotulo = TextView(activity).apply {
                text = activity.getString(item.rotuloRes)
                setTextColor(cor)
                textSize = 12f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                maxLines = 1
                gravity = Gravity.CENTER
                setPadding(0, activity.emPx(2f), 0, 0)
            }

            val celula = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                contentDescription = activity.getString(item.rotuloRes)
                background = fundoDeToque()
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener { aoTocar(activity, item) }
                addView(pilula)
                addView(rotulo)
            }

            container.addView(celula)
        }
    }

    private fun aoTocar(activity: Activity, item: ItemDaBarra) {
        if (!item.pronto) {
            Toast.makeText(
                activity,
                activity.getString(R.string.barra_em_breve, activity.getString(item.rotuloRes)),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
