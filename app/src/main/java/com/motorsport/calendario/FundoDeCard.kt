package com.motorsport.calendario

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * Fundo do card da etapa: cantos arredondados, borda fina, degradê da esquerda para a direita
 * e a faixa da categoria. A faixa é desenhada recortada pelo próprio contorno do card, então
 * acompanha as curvas dos cantos e fica colada na borda, sem emenda.
 */
internal class FundoDeCard(
    private val corDaFaixa: Int,
    private val corInicial: Int,
    private val corFinal: Int,
    private val corDaBorda: Int,
    private val raio: Float,
    private val larguraDaFaixa: Float,
    private val larguraDaBorda: Float
) : Drawable() {

    private val externo = Path()
    private val interno = Path()
    private val areaExterna = RectF()
    private val areaInterna = RectF()

    private val pincelDaBorda = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = corDaBorda }
    private val pincelDoFundo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pincelDaFaixa = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = corDaFaixa }

    override fun onBoundsChange(limites: Rect) {
        super.onBoundsChange(limites)

        areaExterna.set(limites)
        areaInterna.set(
            limites.left + larguraDaBorda,
            limites.top + larguraDaBorda,
            limites.right - larguraDaBorda,
            limites.bottom - larguraDaBorda
        )

        externo.reset()
        externo.addRoundRect(areaExterna, raio, raio, Path.Direction.CW)

        val raioInterno = (raio - larguraDaBorda).coerceAtLeast(0f)
        interno.reset()
        interno.addRoundRect(areaInterna, raioInterno, raioInterno, Path.Direction.CW)

        pincelDoFundo.shader = LinearGradient(
            areaInterna.left, 0f, areaInterna.right, 0f,
            corInicial, corFinal, Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(externo, pincelDaBorda)
        canvas.drawPath(interno, pincelDoFundo)

        val salvo = canvas.save()
        canvas.clipPath(externo)
        canvas.drawRect(
            areaExterna.left,
            areaExterna.top,
            areaExterna.left + larguraDaFaixa,
            areaExterna.bottom,
            pincelDaFaixa
        )
        canvas.restoreToCount(salvo)
    }

    override fun setAlpha(alpha: Int) {
        pincelDaBorda.alpha = alpha
        pincelDoFundo.alpha = alpha
        pincelDaFaixa.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(filtro: ColorFilter?) {
        pincelDaBorda.colorFilter = filtro
        pincelDoFundo.colorFilter = filtro
        pincelDaFaixa.colorFilter = filtro
        invalidateSelf()
    }

    @Deprecated("Exigido pela classe Drawable")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
