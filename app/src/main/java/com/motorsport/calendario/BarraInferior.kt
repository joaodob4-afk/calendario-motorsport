package com.motorsport.calendario

import android.app.Activity
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

internal data class ItemDaBarra(
    val rotulo: String,
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
        ItemDaBarra("Início", R.drawable.ic_nav_inicio, pronto = true),
        ItemDaBarra("Calendário", R.drawable.ic_nav_agenda),
        ItemDaBarra("Classificação", R.drawable.ic_nav_resultados),
        ItemDaBarra("Mais", R.drawable.ic_nav_mais)
    )

    fun montar(activity: Activity, container: LinearLayout, selecionado: Int = 0) {
        container.removeAllViews()

        val corAtiva = activity.getColor(R.color.acento)
        val corInativa = activity.getColor(R.color.texto_suave)

        itens.forEachIndexed { indice, item ->
            val cor = if (indice == selecionado) corAtiva else corInativa

            val icone = ImageView(activity).apply {
                setImageResource(item.icone)
                setColorFilter(cor)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                layoutParams = LinearLayout.LayoutParams(activity.emPx(26f), activity.emPx(26f))
            }

            val rotulo = TextView(activity).apply {
                text = item.rotulo
                setTextColor(cor)
                textSize = 12f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                maxLines = 1
                gravity = Gravity.CENTER
                setPadding(0, activity.emPx(3f), 0, 0)
            }

            val celula = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                contentDescription = item.rotulo
                background = fundoDeToque()
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener { aoTocar(activity, item) }
                addView(icone)
                addView(rotulo)
            }

            container.addView(celula)
        }
    }

    private fun aoTocar(activity: Activity, item: ItemDaBarra) {
        if (!item.pronto) {
            Toast.makeText(activity, "${item.rotulo}: em breve", Toast.LENGTH_SHORT).show()
        }
    }
}
