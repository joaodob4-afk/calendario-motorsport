package com.motorsport.calendario

import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.TextView

/**
 * Faz o texto caber na largura e no número de linhas que o TextView tem, em qualquer tela e com
 * qualquer escala de fonte do sistema: escolhe o maior tamanho (entre [aplicar].minSp e maxSp)
 * em que o texto mede no máximo `maxLinhas` linhas. Se nem o menor tamanho couber, o texto é
 * cortado com "…" em vez de estourar o card.
 *
 * Mede com StaticLayout (o mesmo mecanismo do TextView), por isso funciona também com mais de
 * uma linha e com altura wrap_content, onde o autosize nativo falha. A largura precisa ser
 * definida pelo layout (match_parent ou 0dp com peso), não pelo conteúdo.
 */
internal object AjusteDeTexto {
    private const val PASSO_SP = 1f

    fun aplicar(view: TextView, minSp: Float, maxSp: Float, maxLinhas: Int) {
        view.maxLines = maxLinhas
        view.ellipsize = TextUtils.TruncateAt.END

        // Recalcula quando a largura mudar (rotação, tela dividida, mudança de fonte).
        (view.getTag(R.id.tag_ajuste_texto) as? View.OnLayoutChangeListener)
            ?.let { view.removeOnLayoutChangeListener(it) }
        val ouvinte = View.OnLayoutChangeListener { _, esq, _, dir, _, esqAntes, _, dirAntes, _ ->
            if (dir - esq != dirAntes - esqAntes) ajustar(view, minSp, maxSp, maxLinhas)
        }
        view.addOnLayoutChangeListener(ouvinte)
        view.setTag(R.id.tag_ajuste_texto, ouvinte)

        ajustar(view, minSp, maxSp, maxLinhas)
    }

    private fun ajustar(view: TextView, minSp: Float, maxSp: Float, maxLinhas: Int) {
        val largura = view.width - view.paddingLeft - view.paddingRight
        val texto = view.text
        // Sem largura ainda (view não medida): o ouvinte de layout chama de novo depois.
        if (largura <= 0 || texto.isNullOrEmpty()) return

        try {
            var sp = maxSp
            while (sp > minSp && linhas(view, texto, sp, largura) > maxLinhas) sp -= PASSO_SP
            sp = sp.coerceAtLeast(minSp)

            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, view.resources.displayMetrics)
            if (view.textSize != px) view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        } catch (e: Exception) {
            registrarDescarte("AjusteDeTexto.ajustar", e)
        }
    }

    private fun linhas(view: TextView, texto: CharSequence, sp: Float, largura: Int): Int {
        val tinta = TextPaint(view.paint)
        tinta.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, view.resources.displayMetrics)
        return StaticLayout.Builder.obtain(texto, 0, texto.length, tinta, largura)
            .setIncludePad(view.includeFontPadding)
            .setLineSpacing(view.lineSpacingExtra, view.lineSpacingMultiplier)
            .build()
            .lineCount
    }
}
