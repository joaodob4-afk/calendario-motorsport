package com.motorsport.calendario

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.HorizontalScrollView
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Rolagem horizontal que "encaixa" em um card por vez (como um ViewPager), mas deixando
 * o começo do próximo card aparecendo. Não precisa de nenhuma biblioteca extra.
 *
 * [passo] é a distância, em pixels, de um card até o seguinte (largura + espaço).
 * [paginas] é a quantidade de cards. [aoMudarPagina] avisa qual card está em foco.
 */
class CarrosselDestaque @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : HorizontalScrollView(context, attrs) {

    var passo: Int = 0
    var paginas: Int = 0
    var aoMudarPagina: ((Int) -> Unit)? = null

    private var paginaAvisada = -1
    private var tratouFling = false

    /** Em vez de deixar o fling correr solto, termina sempre no começo de um card. */
    override fun fling(velocityX: Int) {
        if (passo <= 0) {
            super.fling(velocityX)
            return
        }

        tratouFling = true
        val posicao = scrollX / passo.toFloat()
        val alvo = if (velocityX > 0) ceil(posicao).toInt() else floor(posicao).toInt()
        irParaPagina(alvo)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) tratouFling = false

        val resultado = super.onTouchEvent(ev)

        val soltou = ev.actionMasked == MotionEvent.ACTION_UP ||
            ev.actionMasked == MotionEvent.ACTION_CANCEL
        if (soltou && !tratouFling && passo > 0) {
            irParaPagina((scrollX / passo.toFloat()).roundToInt())
        }

        return resultado
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        if (passo <= 0 || paginas <= 0) return
        val pagina = (l / passo.toFloat()).roundToInt().coerceIn(0, paginas - 1)
        if (pagina != paginaAvisada) {
            paginaAvisada = pagina
            aoMudarPagina?.invoke(pagina)
        }
    }

    /** Volta ao primeiro card (usado quando a lista de destaques é refeita). */
    fun reiniciar() {
        paginaAvisada = -1
        scrollTo(0, 0)
    }

    private fun irParaPagina(pagina: Int) {
        val ultima = (paginas - 1).coerceAtLeast(0)
        smoothScrollTo(pagina.coerceIn(0, ultima) * passo, 0)
    }
}
