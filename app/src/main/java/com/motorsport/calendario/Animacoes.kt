package com.motorsport.calendario

import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator

/**
 * Os filhos entram um depois do outro, subindo e aparecendo. Cada um termina na transparência
 * que já tinha (um cartão esmaecido continua esmaecido).
 */
internal fun ViewGroup.entrarEmCascata(deslocamentoDp: Float = 14f, passoMs: Long = 45L, limite: Int = 14) {
    val densidade = resources.displayMetrics.density

    for (i in 0 until minOf(childCount, limite)) {
        val filho = getChildAt(i)
        val alvo = filho.alpha

        filho.animate().cancel()
        filho.alpha = 0f
        filho.translationY = deslocamentoDp * densidade
        filho.animate()
            .alpha(alvo)
            .translationY(0f)
            .setStartDelay(i * passoMs)
            .setDuration(280L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}
