package com.motorsport.calendario

import android.content.Context
import android.view.ViewGroup

/** Linhas de itens que quebram para a linha de baixo quando não cabem (como um parágrafo). */
internal class LinhasQuebradas(context: Context) : ViewGroup(context) {

    var espacoHorizontal = 0
    var espacoVertical = 0

    override fun onMeasure(larguraSpec: Int, alturaSpec: Int) {
        val larguraMaxima = MeasureSpec.getSize(larguraSpec)
        var x = 0
        var y = 0
        var alturaDaLinha = 0

        for (i in 0 until childCount) {
            val filho = getChildAt(i)
            if (filho.visibility == GONE) continue

            measureChild(
                filho,
                MeasureSpec.makeMeasureSpec(larguraMaxima, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )

            val w = filho.measuredWidth
            val h = filho.measuredHeight

            if (x > 0 && x + w > larguraMaxima) {
                x = 0
                y += alturaDaLinha + espacoVertical
                alturaDaLinha = 0
            }

            x += w + espacoHorizontal
            alturaDaLinha = maxOf(alturaDaLinha, h)
        }

        setMeasuredDimension(
            resolveSize(larguraMaxima, larguraSpec),
            resolveSize(y + alturaDaLinha, alturaSpec)
        )
    }

    override fun onLayout(mudou: Boolean, esquerda: Int, topo: Int, direita: Int, base: Int) {
        val larguraMaxima = direita - esquerda
        var x = 0
        var y = 0
        var alturaDaLinha = 0

        for (i in 0 until childCount) {
            val filho = getChildAt(i)
            if (filho.visibility == GONE) continue

            val w = filho.measuredWidth
            val h = filho.measuredHeight

            if (x > 0 && x + w > larguraMaxima) {
                x = 0
                y += alturaDaLinha + espacoVertical
                alturaDaLinha = 0
            }

            filho.layout(x, y, x + w, y + h)
            x += w + espacoHorizontal
            alturaDaLinha = maxOf(alturaDaLinha, h)
        }
    }
}
