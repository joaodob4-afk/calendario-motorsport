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
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView

/**
 * A "mini página" da seta do cabeçalho: uma janelinha logo abaixo do botão, que ocupa pouco da
 * tela, com as categorias que aparecem na página inicial. Cada linha tem uma caixa de marcar.
 *
 * Nenhuma marcada por dentro do app significa "todas" (é assim que fica gravado); na tela, todas
 * aparecem marcadas. Sempre sobra pelo menos uma categoria marcada.
 */
internal class MiniPaginaDeCategorias(
    private val activity: Activity,
    private val selecionadas: () -> Set<String>,
    private val aoMudar: (Set<String>) -> Unit,
    private val aoAbrirOuFechar: (aberta: Boolean) -> Unit
) {

    private var janela: PopupWindow? = null

    val aberta: Boolean get() = janela?.isShowing == true

    private val chaves: List<String> = Categorias.todas.map { Formatacao.normalizar(it.nome) }

    /** O que realmente aparece na página inicial: o conjunto vazio guardado quer dizer todas. */
    private fun efetivas(): Set<String> = selecionadas().ifEmpty { chaves.toSet() }

    fun alternar(ancora: View) {
        if (aberta) fechar() else abrir(ancora)
    }

    fun fechar() {
        val atual = janela ?: return
        val conteudo = atual.contentView

        conteudo.animate().cancel()
        conteudo.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(120)
            .withEndAction { if (atual.isShowing) atual.dismiss() }
            .start()
    }

    private fun abrir(ancora: View) {
        val tela = activity.resources.displayMetrics
        val largura = minOf(activity.emPx(280f), tela.widthPixels - activity.emPx(24f))
        val alturaMaxima = (tela.heightPixels * 0.55f).toInt()

        val lista = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val botaoTodas = TextView(activity)
        val contadorLinhas = ArrayList<(Boolean) -> Unit>()

        fun atualizarTodas() {
            val todasMarcadas = efetivas().size == chaves.size
            botaoTodas.visibility = if (todasMarcadas) View.INVISIBLE else View.VISIBLE
        }

        Categorias.todas.forEachIndexed { indice, categoria ->
            val chave = chaves[indice]
            val linha = linhaDaCategoria(categoria, chave) { marcada ->
                val atuais = efetivas()
                val novas = if (marcada) atuais + chave else atuais - chave

                when {
                    novas.isEmpty() -> false
                    else -> {
                        // Todas marcadas é o mesmo que "sem filtro": guarda vazio.
                        aoMudar(if (novas.size == chaves.size) emptySet() else novas)
                        atualizarTodas()
                        true
                    }
                }
            }
            lista.addView(linha.first)
            contadorLinhas.add(linha.second)
        }

        val rolagem = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(lista)
        }

        // Altura da lista: a que ela precisa, até o máximo (acima disso, rola).
        lista.measure(
            View.MeasureSpec.makeMeasureSpec(largura, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val alturaDaLista = minOf(lista.measuredHeight, alturaMaxima - activity.emPx(52f))

        val cabecalho = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(activity.emPx(16f), activity.emPx(4f), activity.emPx(6f), activity.emPx(4f))

            addView(
                TextView(activity).apply {
                    text = activity.getString(R.string.inicial_titulo).uppercase(Idioma.locale(activity))
                    setTextColor(activity.getColor(R.color.texto_suave))
                    textSize = 11f
                    letterSpacing = 0.12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
            )

            botaoTodas.apply {
                text = activity.getString(R.string.inicial_todas)
                setTextColor(activity.getColor(R.color.azul_destaque))
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(activity.emPx(12f), 0, activity.emPx(12f), 0)
                minHeight = activity.emPx(40f)
                isClickable = true
                isFocusable = true
                background = ripple(null, 8f)
                setOnClickListener {
                    aoMudar(emptySet())
                    contadorLinhas.forEach { marcar -> marcar(true) }
                    atualizarTodas()
                }
                comEfeitoDeToque(escala = 0.94f)
            }
            addView(botaoTodas)
        }

        val cartao = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(activity.getColor(R.color.superficie))
                setStroke(activity.emPx(1f).coerceAtLeast(1), activity.getColor(R.color.borda_suave))
                cornerRadius = activity.emPx(12f).toFloat()
            }
            elevation = activity.emPx(10f).toFloat()
            setPadding(0, activity.emPx(6f), 0, activity.emPx(6f))

            addView(cabecalho)
            addView(
                View(activity).apply {
                    setBackgroundColor(activity.getColor(R.color.borda_suave))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        activity.emPx(1f).coerceAtLeast(1)
                    ).apply { topMargin = activity.emPx(2f); bottomMargin = activity.emPx(2f) }
                }
            )
            addView(rolagem, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, alturaDaLista))
        }

        // A área em volta do cartão dá espaço para a sombra aparecer.
        val moldura = FrameLayout(activity).apply {
            setPadding(activity.emPx(8f), 0, activity.emPx(8f), activity.emPx(12f))
            addView(cartao)
        }

        atualizarTodas()

        val novaJanela = PopupWindow(moldura, largura + activity.emPx(16f), ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            setOnDismissListener {
                janela = null
                aoAbrirOuFechar(false)
            }
        }

        janela = novaJanela
        novaJanela.showAsDropDown(ancora, activity.emPx(4f), activity.emPx(4f), Gravity.END)
        aoAbrirOuFechar(true)

        // Entrada: cresce a partir do canto da seta, com um leve "pulo".
        moldura.alpha = 0f
        moldura.pivotX = (largura + activity.emPx(16f)).toFloat()
        moldura.pivotY = 0f
        moldura.scaleX = 0.88f
        moldura.scaleY = 0.88f
        moldura.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200)
            .setInterpolator(DecelerateInterpolator()).start()

        lista.entrarEmCascata(deslocamentoDp = 6f, passoMs = 22L, limite = 12)
    }

    private fun ripple(preenchimento: Int?, raioDp: Float): RippleDrawable =
        RippleDrawable(
            ColorStateList.valueOf(Color.parseColor("#334C8DFF")),
            preenchimento?.let { forma(it, raioDp) },
            forma(Color.WHITE, raioDp)
        )

    private fun forma(cor: Int, raioDp: Float) =
        GradientDrawable().apply {
            setColor(cor)
            cornerRadius = activity.emPx(raioDp).toFloat()
        }

    /**
     * Uma linha: faixa na cor da categoria, o nome e uma caixa quadrada de marcar. Devolve a linha
     * e uma função para marcar/desmarcar de fora (usada pelo botão "Todas").
     * [aoTocar] recebe o novo estado e responde se ele foi aceito.
     */
    private fun linhaDaCategoria(
        categoria: CategoriaInfo,
        chave: String,
        aoTocar: (Boolean) -> Boolean
    ): Pair<View, (Boolean) -> Unit> {
        val cor = Color.parseColor(categoria.cor)
        var marcada = chave in efetivas()

        val faixa = View(activity).apply {
            background = forma(cor, 2f)
            layoutParams = LinearLayout.LayoutParams(activity.emPx(4f), activity.emPx(24f))
                .apply { marginStart = activity.emPx(2f); marginEnd = activity.emPx(12f) }
        }

        val nome = TextView(activity).apply {
            text = categoria.nome
            setTextColor(activity.getColor(R.color.texto_principal))
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val marca = ImageView(activity).apply {
            setImageResource(R.drawable.ic_check)
            setColorFilter(Color.WHITE)
            setPadding(activity.emPx(2f), activity.emPx(2f), activity.emPx(2f), activity.emPx(2f))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val caixa = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(activity.emPx(22f), activity.emPx(22f))
            addView(marca, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        fun pintar(valor: Boolean) {
            caixa.background = GradientDrawable().apply {
                cornerRadius = activity.emPx(5f).toFloat()
                if (valor) {
                    setColor(activity.getColor(R.color.azul_destaque))
                } else {
                    setColor(Color.TRANSPARENT)
                    setStroke(activity.emPx(2f), activity.getColor(R.color.borda_suave))
                }
            }
            marca.visibility = if (valor) View.VISIBLE else View.INVISIBLE
        }

        val linha = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = activity.emPx(46f)
            setPadding(activity.emPx(14f), activity.emPx(4f), activity.emPx(16f), activity.emPx(4f))
            isClickable = true
            isFocusable = true
            background = ripple(null, 0f)

            addView(faixa)
            addView(nome)
            addView(caixa)

            pintar(marcada)
            contentDescription = activity.getString(
                if (marcada) R.string.cd_ocultar_categoria else R.string.cd_mostrar_categoria,
                categoria.nome
            )

            setOnClickListener {
                val novo = !marcada

                if (aoTocar(novo)) {
                    marcada = novo
                    pintar(marcada)
                    contentDescription = activity.getString(
                        if (marcada) R.string.cd_ocultar_categoria else R.string.cd_mostrar_categoria,
                        categoria.nome
                    )

                    if (marcada) {
                        caixa.scaleX = 0.5f
                        caixa.scaleY = 0.5f
                        caixa.animate().scaleX(1f).scaleY(1f).setDuration(300)
                            .setInterpolator(OvershootInterpolator(3f)).start()
                    }
                } else {
                    // Não dá para tirar a última: a linha "balança" para avisar.
                    animate().cancel()
                    animate().translationX(activity.emPx(6f).toFloat()).setDuration(50).withEndAction {
                        animate().translationX(-activity.emPx(4f).toFloat()).setDuration(70).withEndAction {
                            animate().translationX(0f).setDuration(60).start()
                        }.start()
                    }.start()
                }
            }
        }

        val definirDeFora: (Boolean) -> Unit = { valor ->
            if (valor != marcada) {
                marcada = valor
                pintar(valor)
                linha.contentDescription = activity.getString(
                    if (valor) R.string.cd_ocultar_categoria else R.string.cd_mostrar_categoria,
                    categoria.nome
                )
            }
        }

        return linha to definirDeFora
    }
}
