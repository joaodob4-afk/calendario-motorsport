package com.motorsport.calendario

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Interruptor no estilo moderno: trilha arredondada que fica num verde vivo quando ligada e
 * uma bolinha que desliza. A área de toque é a própria view (um pouco maior que o desenho).
 *
 * [animarDe] é o estado de antes do toque: o painel é refeito a cada mudança, então a nova
 * view começa nesse estado e desliza até o atual assim que aparece na tela.
 */
internal class InterruptorModerno(
    context: Context,
    val ligado: Boolean,
    private val animarDe: Boolean? = null
) : View(context) {

    private val densidade = resources.displayMetrics.density
    private val corLigado = Color.parseColor("#1ED760")
    private val corDaTrilha = Color.parseColor("#2B3035")
    private val corDoContorno = Color.parseColor("#3A3F45")
    private val corDaBolinhaApagada = Color.parseColor("#9AA0A6")

    private val trilha = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = corDaTrilha }
    private val contorno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = corDoContorno
        strokeWidth = densidade
    }
    private val ligadoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = corLigado }
    private val bolinha = Paint(Paint.ANTI_ALIAS_FLAG)
    private val area = RectF()
    private val avaliadorDeCor = ArgbEvaluator()

    /** 0 = desligado, 1 = ligado. Começa no estado anterior, se houver. */
    private var progresso = if (animarDe ?: ligado) 1f else 0f
    private var animador: ValueAnimator? = null

    private fun dp(valor: Float) = valor * densidade

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val alvo = if (ligado) 1f else 0f

        if (animarDe != null && animarDe != ligado) {
            animador = ValueAnimator.ofFloat(progresso, alvo).apply {
                duration = 180L
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    progresso = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            progresso = alvo
        }
    }

    override fun onDetachedFromWindow() {
        animador?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(dp(56f).toInt(), dp(36f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val largura = dp(48f)
        val altura = dp(28f)
        val esquerda = (width - largura) / 2f
        val topo = (height - altura) / 2f
        val raio = altura / 2f

        area.set(esquerda, topo, esquerda + largura, topo + altura)

        canvas.drawRoundRect(area, raio, raio, trilha)

        contorno.alpha = ((1f - progresso) * 255).toInt()
        canvas.drawRoundRect(area, raio, raio, contorno)

        ligadoPaint.alpha = (progresso * 255).toInt()
        canvas.drawRoundRect(area, raio, raio, ligadoPaint)

        val margem = dp(3f)
        val raioDaBolinha = raio - margem
        val xEsquerda = area.left + margem + raioDaBolinha
        val xDireita = area.right - margem - raioDaBolinha

        bolinha.color = avaliadorDeCor.evaluate(progresso, corDaBolinhaApagada, Color.WHITE) as Int
        canvas.drawCircle(
            xEsquerda + (xDireita - xEsquerda) * progresso,
            area.centerY(),
            raioDaBolinha,
            bolinha
        )
    }
}
