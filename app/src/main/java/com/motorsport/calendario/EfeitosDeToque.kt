package com.motorsport.calendario

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/**
 * Faz [alvo] encolher um pouco enquanto o dedo está sobre esta view e voltar com um leve
 * "quique" ao soltar. Não atrapalha o clique nem a rolagem (o toque segue para quem o trata).
 */
@SuppressLint("ClickableViewAccessibility")
internal fun View.comEfeitoDeToque(alvo: View = this, escala: Float = 0.94f) {
    setOnTouchListener { _, evento ->
        when (evento.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                alvo.animate()
                    .scaleX(escala)
                    .scaleY(escala)
                    .setStartDelay(0)
                    .setDuration(90)
                    .setInterpolator(DecelerateInterpolator())
                    .start()

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                alvo.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(0)
                    .setDuration(220)
                    .setInterpolator(OvershootInterpolator(2.5f))
                    .start()
        }
        false
    }
}

/** Aparece "estourando" de leve: usado ao marcar uma opção ou um botão. */
internal fun View.animarPop() {
    scaleX = 0.86f
    scaleY = 0.86f
    animate()
        .scaleX(1f)
        .scaleY(1f)
        .setStartDelay(0)
        .setDuration(280)
        .setInterpolator(OvershootInterpolator(3f))
        .start()
}
