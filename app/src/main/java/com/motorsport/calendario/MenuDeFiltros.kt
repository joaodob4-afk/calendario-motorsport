package com.motorsport.calendario

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.animation.ValueAnimator
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.ColorUtils
import android.widget.FrameLayout
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
    private val contagem = TextView(activity)
    private val limpar = TextView(activity)

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
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(cartao)

        limpar.apply {
            text = activity.getString(R.string.favoritas_limpar)
            setTextColor(activity.getColor(R.color.azul_destaque))
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isClickable = true
            isFocusable = true
            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#334C8DFF")),
                null,
                forma(Color.WHITE, null, 8f)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                activity.emPx(46f)
            ).apply { topMargin = activity.emPx(12f) }
            setOnClickListener {
                aoMudar(emptySet())
                desenhar()
                agendarNovosAvisos()
            }
            comEfeitoDeToque(escala = 0.97f)
        }
        container.addView(limpar)

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
                setColor(activity.getColor(R.color.azul_marinho_claro))
                cornerRadius = activity.emPx(8f).toFloat()
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

        contagem.apply {
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(activity.emPx(10f), activity.emPx(4f), activity.emPx(10f), activity.emPx(4f))
            background = GradientDrawable().apply {
                setColor(corDoCoracao)
                cornerRadius = activity.emPx(6f).toFloat()
            }
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(icone)
            addView(texto)
            addView(contagem)
        }
    }

    private fun desenhar() {
        cartao.removeAllViews()

        Categorias.todas.forEach { categoria -> cartao.addView(linha(categoria)) }

        atualizarContagem()
    }

    /** "2/10" ao lado do título e o botão de limpar só quando há favoritas. */
    private fun atualizarContagem() {
        val quantas = favoritas().size

        contagem.text = "$quantas/${Categorias.todas.size}"
        contagem.visibility = if (quantas > 0) View.VISIBLE else View.GONE
        limpar.visibility = if (quantas > 0) View.VISIBLE else View.GONE
    }

    private fun rotuloDoModo(modo: ModoDeAviso): String =
        activity.getString(
            when (modo) {
                ModoDeAviso.TODAS -> R.string.modo_todas
                ModoDeAviso.CORRIDAS -> R.string.modo_so_corridas
                ModoDeAviso.NENHUMA -> R.string.modo_nenhuma
            }
        )

    /** Botão só com o ícone: sem círculo e sem borda; ao tocar, uma ondulação retangular. */
    private fun botaoDeIcone(): ImageView =
        ImageView(activity).apply {
            setPadding(activity.emPx(8f), activity.emPx(8f), activity.emPx(8f), activity.emPx(8f))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(activity.emPx(40f), activity.emPx(40f))
            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#334C8DFF")),
                null,
                forma(Color.WHITE, null, 8f)
            )
            comEfeitoDeToque(escala = 0.88f)
        }

    /**
     * Um cartão por categoria, com tudo dela num lugar só: faixa na cor da categoria, o nome, o
     * que ela avisa e dois botões. O coração acompanha a categoria (ela entra na lista da tela
     * inicial); o sino muda o aviso: todas as sessões, só corridas ou nenhuma. Favoritada, o
     * cartão ganha um tom da cor da categoria (a troca é animada).
     */
    private fun linha(categoria: CategoriaInfo): View {
        val chave = Formatacao.normalizar(categoria.nome)
        val cor = Color.parseColor(categoria.cor)

        val corNormal = activity.getColor(R.color.superficie)
        val bordaNormal = activity.getColor(R.color.borda_suave)
        val corFavorita = ColorUtils.blendARGB(corNormal, cor, 0.16f)
        val bordaFavorita = ColorUtils.blendARGB(bordaNormal, cor, 0.85f)

        val fundo = GradientDrawable().apply {
            cornerRadius = activity.emPx(12f).toFloat()
        }

        fun aplicarCores(mistura: Float) {
            fundo.setColor(ColorUtils.blendARGB(corNormal, corFavorita, mistura))
            fundo.setStroke(
                activity.emPx(1f).coerceAtLeast(1),
                ColorUtils.blendARGB(bordaNormal, bordaFavorita, mistura)
            )
        }

        val faixa = View(activity).apply {
            background = GradientDrawable().apply {
                setColor(cor)
                cornerRadius = activity.emPx(2f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(activity.emPx(4f), activity.emPx(36f))
                .apply { marginStart = activity.emPx(4f); marginEnd = activity.emPx(14f) }
        }

        val nome = TextView(activity).apply {
            text = categoria.nome
            setTextColor(activity.getColor(R.color.texto_principal))
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 1
        }

        val aviso = TextView(activity).apply {
            setTextColor(activity.getColor(R.color.texto_suave))
            textSize = 12f
            maxLines = 1
            setPadding(0, activity.emPx(2f), 0, 0)
        }

        val textos = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(nome)
            addView(aviso)
        }

        val sino = botaoDeIcone().apply { setImageResource(R.drawable.ic_sino) }
        val coracao = botaoDeIcone()

        fun pintarCoracao(favorita: Boolean) {
            coracao.setImageResource(if (favorita) R.drawable.ic_coracao_cheio else R.drawable.ic_coracao)
            coracao.setColorFilter(if (favorita) corDoCoracao else activity.getColor(R.color.texto_suave))
            coracao.contentDescription = activity.getString(
                if (favorita) R.string.cd_desfavoritar_categoria else R.string.cd_favoritar_categoria,
                categoria.nome
            )
        }

        /** Texto do aviso e cor do sino, de acordo com o que a categoria realmente avisa agora. */
        fun pintarAviso() {
            val silenciada = PreferenciasDeAviso.silenciadaPorFavoritas(activity, categoria.nome)
            val modo = PreferenciasDeAviso.modoSalvoDaCategoria(activity, categoria.nome)
            val avisa = !silenciada && modo != ModoDeAviso.NENHUMA

            aviso.text = if (silenciada) {
                activity.getString(R.string.favoritas_silenciada)
            } else {
                activity.getString(R.string.favoritas_avisos, rotuloDoModo(modo))
            }
            sino.setColorFilter(
                if (avisa) activity.getColor(R.color.azul_destaque) else activity.getColor(R.color.texto_suave)
            )
            sino.alpha = if (silenciada) 0.5f else 1f
            sino.contentDescription = activity.getString(
                R.string.cd_alternar_aviso,
                categoria.nome,
                rotuloDoModo(modo)
            )
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = activity.emPx(64f)
            setPadding(activity.emPx(10f), activity.emPx(6f), activity.emPx(6f), activity.emPx(6f))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.emPx(8f) }

            var favorita = chave in favoritas()
            aplicarCores(if (favorita) 1f else 0f)
            background = fundo

            addView(faixa)
            addView(textos)
            addView(sino)
            addView(coracao)

            pintarCoracao(favorita)
            pintarAviso()

            sino.setOnClickListener {
                val proximo = when (PreferenciasDeAviso.modoSalvoDaCategoria(activity, categoria.nome)) {
                    ModoDeAviso.TODAS -> ModoDeAviso.CORRIDAS
                    ModoDeAviso.CORRIDAS -> ModoDeAviso.NENHUMA
                    ModoDeAviso.NENHUMA -> ModoDeAviso.TODAS
                }

                PreferenciasDeAviso.definirModo(activity, categoria.nome, proximo)
                pintarAviso()
                agendarNovosAvisos()
                sino.animarPop()
            }

            coracao.setOnClickListener {
                val atuais = favoritas()
                favorita = chave !in atuais

                // Favoritar já liga o aviso das corridas, se a categoria não avisava nada de especial.
                if (favorita &&
                    (!PreferenciasDeAviso.modoFoiEscolhido(activity, categoria.nome) ||
                        PreferenciasDeAviso.modoSalvoDaCategoria(activity, categoria.nome) == ModoDeAviso.NENHUMA)
                ) {
                    PreferenciasDeAviso.definirModo(activity, categoria.nome, ModoDeAviso.CORRIDAS)
                }

                // Os favoritos são gravados na hora: "só favoritas" lê daqui para saber quem avisa.
                aoMudar(if (favorita) atuais + chave else atuais - chave)

                pintarCoracao(favorita)
                pintarAviso()
                atualizarContagem()
                agendarNovosAvisos()

                // O tom do cartão aparece (ou some) suavemente.
                ValueAnimator.ofFloat(if (favorita) 0f else 1f, if (favorita) 1f else 0f).apply {
                    duration = 260
                    addUpdateListener { aplicarCores(it.animatedValue as Float) }
                    start()
                }

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
        }
    }

    private val handlerDosAvisos = Handler(Looper.getMainLooper())
    private val reagendarAvisos = Runnable { AgendadorDeAvisos.reagendarEmSegundoPlano(activity) }

    /** Refaz os alarmes um instante depois do último toque (vários toques seguidos viram um só). */
    private fun agendarNovosAvisos() {
        handlerDosAvisos.removeCallbacks(reagendarAvisos)
        handlerDosAvisos.postDelayed(reagendarAvisos, 700)
    }

    private fun forma(preenchimento: Int, borda: Int?, raioDp: Float) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = activity.emPx(raioDp).toFloat()
            setColor(preenchimento)
            if (borda != null) setStroke(activity.emPx(1f).coerceAtLeast(1), borda)
        }
}
