package com.motorsport.calendario

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Conteúdo do painel que abre pela direita da tela inicial (botão de coração): a lista das
 * categorias, cada uma com um coração para favoritar. A tela inicial mostra só as favoritas;
 * sem nenhuma favorita, mostra todas. Tocar na linha já atualiza a lista que está atrás.
 */
internal class MenuDeFiltros(
    private val activity: Activity,
    private val container: LinearLayout,
    private val favoritas: () -> Set<String>,
    private val aoMudar: (Set<String>) -> Unit
) {

    private companion object {
        val corDoCoracao: Int = Color.parseColor("#FF4D6D")
    }

    private val cartao = LinearLayout(activity)

    fun montar() {
        container.removeAllViews()

        container.addView(cabecalho())

        container.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.favoritas_dica)
                setTextColor(activity.getColor(R.color.texto_suave))
                textSize = 13f
                setLineSpacing(0f, 1.15f)
                setPadding(activity.emPx(2f), activity.emPx(18f), 0, activity.emPx(14f))
            }
        )

        cartao.apply {
            orientation = LinearLayout.VERTICAL
            background = forma(activity.getColor(R.color.superficie), activity.getColor(R.color.borda_suave), 16f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(cartao)

        desenhar()
    }

    /** Entrada em cascata: usada cada vez que o painel abre. */
    fun animarEntrada() {
        container.entrarEmCascata()
        cartao.entrarEmCascata(deslocamentoDp = 10f, passoMs = 35L, limite = 12)
    }

    private fun cabecalho(): View {
        val icone = ImageView(activity).apply {
            setImageResource(R.drawable.ic_coracao_cheio)
            setColorFilter(corDoCoracao)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setPadding(activity.emPx(9f), activity.emPx(9f), activity.emPx(9f), activity.emPx(9f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(activity.getColor(R.color.azul_marinho_claro))
                setStroke(activity.emPx(1f), activity.getColor(R.color.azul_marinho_borda))
            }
            layoutParams = LinearLayout.LayoutParams(activity.emPx(40f), activity.emPx(40f))
        }

        val texto = TextView(activity).apply {
            text = activity.getString(R.string.favoritas_titulo)
            setTextColor(activity.getColor(R.color.texto_principal))
            textSize = 22f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            isSingleLine = true
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = activity.emPx(12f) }
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(icone)
            addView(texto)
        }
    }

    private fun desenhar() {
        cartao.removeAllViews()

        Categorias.todas.forEachIndexed { indice, categoria ->
            if (indice > 0) cartao.addView(divisor())
            cartao.addView(linha(categoria))
        }
    }

    private fun divisor(): View =
        View(activity).apply {
            setBackgroundColor(activity.getColor(R.color.borda_suave))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                activity.emPx(1f).coerceAtLeast(1)
            ).apply {
                marginStart = activity.emPx(14f)
                marginEnd = activity.emPx(14f)
            }
        }

    /** Ponto na cor da categoria, o nome e o coração. Tocar em qualquer lugar da linha favorita. */
    private fun linha(categoria: CategoriaInfo): View {
        val chave = Formatacao.normalizar(categoria.nome)
        val cor = Color.parseColor(categoria.cor)

        val ponto = View(activity).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(cor)
            }
            layoutParams = LinearLayout.LayoutParams(activity.emPx(12f), activity.emPx(12f))
                .apply { marginStart = activity.emPx(8f); marginEnd = activity.emPx(14f) }
        }

        val nome = TextView(activity).apply {
            text = categoria.nome
            setTextColor(activity.getColor(R.color.texto_principal))
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val coracao = ImageView(activity).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setPadding(activity.emPx(8f), activity.emPx(8f), activity.emPx(8f), activity.emPx(8f))
            layoutParams = LinearLayout.LayoutParams(activity.emPx(40f), activity.emPx(40f))
        }

        fun pintar(favorita: Boolean) {
            coracao.setImageResource(if (favorita) R.drawable.ic_coracao_cheio else R.drawable.ic_coracao)
            coracao.setColorFilter(if (favorita) corDoCoracao else activity.getColor(R.color.texto_suave))
        }

        fun descricao(favorita: Boolean) = activity.getString(
            if (favorita) R.string.cd_desfavoritar_categoria else R.string.cd_favoritar_categoria,
            categoria.nome
        )

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = activity.emPx(58f)
            setPadding(activity.emPx(8f), activity.emPx(4f), activity.emPx(8f), activity.emPx(4f))
            isClickable = true
            isFocusable = true
            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#334C8DFF")),
                null,
                ColorDrawable(Color.WHITE)
            )

            addView(ponto)
            addView(nome)
            addView(coracao)

            var favorita = chave in favoritas()
            pintar(favorita)
            contentDescription = descricao(favorita)

            setOnClickListener {
                val atuais = favoritas()
                favorita = chave !in atuais
                aoMudar(if (favorita) atuais + chave else atuais - chave)

                pintar(favorita)
                contentDescription = descricao(favorita)

                // O coração "estoura" ao favoritar e murcha ao desfavoritar.
                coracao.animate().cancel()
                if (favorita) {
                    coracao.scaleX = 0.4f
                    coracao.scaleY = 0.4f
                    coracao.animate().scaleX(1f).scaleY(1f).setDuration(380)
                        .setInterpolator(OvershootInterpolator(3.2f)).start()
                } else {
                    coracao.animate().scaleX(0.7f).scaleY(0.7f).setDuration(90).withEndAction {
                        coracao.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
                    }.start()
                }
            }
            comEfeitoDeToque(escala = 0.98f)
        }
    }

    private fun forma(preenchimento: Int, borda: Int?, raioDp: Float) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = activity.emPx(raioDp).toFloat()
            setColor(preenchimento)
            if (borda != null) setStroke(activity.emPx(1f).coerceAtLeast(1), borda)
        }
}