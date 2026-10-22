package com.motorsport.calendario

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

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

/** Rolagem que nunca passa de [alturaMaxima], mesmo com o conteúdo maior que a tela. */
private class RolagemLimitada(context: Context, private val alturaMaxima: Int) : ScrollView(context) {
    override fun onMeasure(larguraSpec: Int, alturaSpec: Int) {
        super.onMeasure(
            larguraSpec,
            MeasureSpec.makeMeasureSpec(alturaMaxima, MeasureSpec.AT_MOST)
        )
    }
}

/**
 * Painel de filtros da tela inicial: sobe do pé da tela com as opções de etapas finalizadas,
 * país e mês. Cada toque já atualiza a lista que está atrás do painel; o botão do pé só fecha.
 */
internal class PainelDeFiltros(
    private val activity: Activity,
    private val todasEtapas: () -> List<MainActivity.Etapa>,
    private var filtros: FiltrosDeEtapas,
    private val rotuloDoPais: (String) -> String,
    private val aoMudar: (FiltrosDeEtapas) -> Unit
) {

    private val hoje: LocalDate = LocalDate.now()
    private val dialogo = Dialog(activity)
    private val conteudo = LinearLayout(activity)
    private val botaoFechar = TextView(activity)
    private val botaoLimpar = TextView(activity)

    /** Opção tocada por último: ao redesenhar, só ela "estoura" (animação de marcar). */
    private var ultimaChave: String? = null

    private val corAtiva = activity.getColor(R.color.acento)
    private val corDoFundo = activity.getColor(R.color.superficie)
    private val corDaBorda = activity.getColor(R.color.borda_suave)
    private val corSuave = activity.getColor(R.color.texto_suave)
    private val corEscura = Color.parseColor("#0B0D0F")
    private val corDoCartao = Color.parseColor("#262B30")

    private class Opcao(
        val chave: String,
        val rotulo: String,
        val marcada: Boolean,
        val aoTocar: () -> Unit
    )

    /** Fecha o painel (usado quando a tela é destruída com ele aberto). */
    fun fechar() {
        if (dialogo.isShowing) dialogo.dismiss()
    }

    fun mostrar() {
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogo.setContentView(montarRaiz())

        dialogo.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setWindowAnimations(R.style.AnimacaoPainelFiltros)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.6f)
        }

        desenhar()
        dialogo.show()
    }

    private fun montarRaiz(): View {
        val raio = activity.emPx(28f).toFloat()
        val baseDeBaixo = activity.emPx(20f)

        val raiz = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(corDoFundo)
                cornerRadii = floatArrayOf(raio, raio, raio, raio, 0f, 0f, 0f, 0f)
            }
            setPadding(activity.emPx(20f), activity.emPx(10f), activity.emPx(20f), baseDeBaixo)
        }

        // Acima da barra de navegação do sistema (gestos ou botões)
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { v, insets ->
            val barra = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, baseDeBaixo + barra)
            insets
        }

        // Alça no topo, indicando que é um painel que sobe da base
        raiz.addView(
            View(activity).apply {
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#4A5158"))
                    cornerRadius = activity.emPx(2f).toFloat()
                }
            },
            LinearLayout.LayoutParams(activity.emPx(40f), activity.emPx(4f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = activity.emPx(8f)
            }
        )

        // Cabeçalho: título e "Limpar" (área de toque de 48dp)
        val cabecalho = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        cabecalho.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.filtros_titulo)
                setTextColor(Color.WHITE)
                textSize = 22f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        botaoLimpar.apply {
            text = activity.getString(R.string.filtros_limpar)
            setTextColor(corAtiva)
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            minWidth = activity.emPx(72f)
            minHeight = activity.emPx(48f)
            setPadding(activity.emPx(12f), 0, 0, 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { limpar() }
            comEfeitoDeToque(escala = 0.92f)
        }
        cabecalho.addView(
            botaoLimpar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        raiz.addView(cabecalho)

        // Opções (rolam quando não cabem na tela)
        val altura = (activity.resources.displayMetrics.heightPixels * 0.55f).toInt()
        val rolagem = RolagemLimitada(activity, altura).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        conteudo.orientation = LinearLayout.VERTICAL
        rolagem.addView(
            conteudo,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        raiz.addView(
            rolagem,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Botão do pé: mostra quantas etapas ficaram na lista e fecha o painel
        botaoFechar.apply {
            gravity = Gravity.CENTER
            setTextColor(corEscura)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = GradientDrawable().apply {
                setColor(corAtiva)
                cornerRadius = activity.emPx(16f).toFloat()
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { dialogo.dismiss() }
            comEfeitoDeToque(escala = 0.97f)
        }
        raiz.addView(
            botaoFechar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                activity.emPx(52f)
            ).apply { topMargin = activity.emPx(16f) }
        )

        return raiz
    }

    private fun limpar() {
        ultimaChave = null
        mudar(filtros.copy(mostrarFinalizadas = false, paises = emptySet(), meses = emptySet()))
    }

    private fun mudar(novos: FiltrosDeEtapas) {
        val antes = filtros.mostrarFinalizadas
        filtros = RegrasDeFiltro.podar(todasEtapas(), novos, hoje)
        aoMudar(filtros)
        desenhar(animarDe = antes)
    }

    /** Refaz as opções com o estado atual dos filtros. */
    private fun desenhar(animarDe: Boolean? = null) {
        conteudo.removeAllViews()

        val etapas = todasEtapas()
        val base = RegrasDeFiltro.base(etapas, filtros, hoje)

        // "Limpar" só vale quando há algo para limpar
        val temFiltro = filtros.quantosAtivos > 0
        botaoLimpar.isEnabled = temFiltro
        botaoLimpar.animate().alpha(if (temFiltro) 1f else 0.35f).setStartDelay(0).setDuration(150).start()

        // Etapas finalizadas
        conteudo.addView(titulo(activity.getString(R.string.filtros_finalizadas_titulo), 0, primeiro = true))
        conteudo.addView(cartaoDeFinalizadas(animarDe))

        // País
        val paises = RegrasDeFiltro.paisesDisponiveis(base)
            .map { it to rotuloDoPais(it) }
            .sortedBy { Formatacao.normalizar(it.second) }

        if (paises.isNotEmpty()) {
            conteudo.addView(titulo(activity.getString(R.string.filtros_pais), filtros.paises.size))
            conteudo.addView(
                grupoDeOpcoes(
                    paises.map { (valor, rotulo) ->
                        val chave = "pais:$valor"
                        Opcao(chave, rotulo, valor in filtros.paises) {
                            ultimaChave = chave
                            val novos = if (valor in filtros.paises) filtros.paises - valor else filtros.paises + valor
                            mudar(filtros.copy(paises = novos))
                        }
                    }
                )
            )
        }

        // Mês
        val meses = RegrasDeFiltro.mesesDisponiveis(base)

        if (meses.isNotEmpty()) {
            conteudo.addView(titulo(activity.getString(R.string.filtros_mes), filtros.meses.size))
            conteudo.addView(
                grupoDeOpcoes(
                    meses.map { mes ->
                        val chave = "mes:$mes"
                        Opcao(chave, rotuloDoMes(mes), mes in filtros.meses) {
                            ultimaChave = chave
                            val novos = if (mes in filtros.meses) filtros.meses - mes else filtros.meses + mes
                            mudar(filtros.copy(meses = novos))
                        }
                    }
                )
            )
        }

        ultimaChave = null

        val quantas = RegrasDeFiltro.aplicar(etapas, filtros, hoje).size
        botaoFechar.text = activity.getString(R.string.filtros_mostrar, quantas)
    }

    /** Título de seção em caixa alta; mostra "· 2" quando há opções marcadas nela. */
    private fun titulo(texto: String, marcadas: Int, primeiro: Boolean = false): View =
        TextView(activity).apply {
            val completo = if (marcadas > 0) "$texto · $marcadas" else texto
            text = completo.uppercase(Idioma.locale(activity))
            setTextColor(if (marcadas > 0) corAtiva else corSuave)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.12f
            setPadding(activity.emPx(2f), activity.emPx(if (primeiro) 14f else 22f), 0, activity.emPx(10f))
        }

    /** Cartão com o texto e o interruptor de "mostrar etapas finalizadas"; tocar em qualquer lugar liga/desliga. */
    private fun cartaoDeFinalizadas(animarDe: Boolean?): View {
        val ligado = filtros.mostrarFinalizadas

        val cartao = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = activity.emPx(64f)
            setPadding(activity.emPx(16f), activity.emPx(10f), activity.emPx(16f), activity.emPx(10f))
            background = GradientDrawable().apply {
                setColor(corDoCartao)
                cornerRadius = activity.emPx(16f).toFloat()
            }
            isClickable = true
            isFocusable = true
            contentDescription = activity.getString(R.string.filtros_finalizadas_opcao)
            setOnClickListener { mudar(filtros.copy(mostrarFinalizadas = !filtros.mostrarFinalizadas)) }
            comEfeitoDeToque(escala = 0.98f)
        }

        val textos = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.filtros_finalizadas_opcao)
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
        )
        textos.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.filtros_finalizadas_dica)
                setTextColor(corSuave)
                textSize = 12f
                setPadding(0, activity.emPx(2f), 0, 0)
            }
        )

        cartao.addView(textos, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        cartao.addView(InterruptorModerno(activity, ligado, animarDe))

        return cartao
    }

    /** Opções em forma de cápsulas que quebram de linha; as marcadas ficam preenchidas, com um "✓". */
    private fun grupoDeOpcoes(opcoes: List<Opcao>): View {
        val grupo = LinhasQuebradas(activity).apply {
            espacoHorizontal = activity.emPx(8f)
            espacoVertical = activity.emPx(8f)
        }

        for (opcao in opcoes) {
            val capsula = TextView(activity).apply {
                text = opcao.rotulo
                textSize = 14f
                maxLines = 1
                gravity = Gravity.CENTER
                includeFontPadding = false
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(activity.emPx(16f), 0, activity.emPx(16f), 0)
                minHeight = activity.emPx(40f)
                isClickable = true
                isFocusable = true
                setTextColor(if (opcao.marcada) corEscura else Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = activity.emPx(20f).toFloat()
                    if (opcao.marcada) {
                        setColor(corAtiva)
                    } else {
                        setColor(Color.TRANSPARENT)
                        setStroke(activity.emPx(1f).coerceAtLeast(1), corDaBorda)
                    }
                }
                if (opcao.marcada) {
                    setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
                    compoundDrawablePadding = activity.emPx(6f)
                }
                setOnClickListener { opcao.aoTocar() }
                comEfeitoDeToque(escala = 0.94f)
            }

            if (opcao.marcada && opcao.chave == ultimaChave) capsula.animarPop()

            grupo.addView(capsula)
        }

        return grupo
    }

    /** "Novembro"; com o ano ("Janeiro 2027") quando não é o ano atual. */
    private fun rotuloDoMes(chave: String): String {
        val locale = Idioma.locale(activity)
        val mes = YearMonth.parse(chave)
        val nome = mes.month.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.titlecase(locale) }

        return if (mes.year != hoje.year) "$nome ${mes.year}" else nome
    }
}
