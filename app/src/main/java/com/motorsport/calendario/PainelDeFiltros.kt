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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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

    private val corAtiva = activity.getColor(R.color.acento)
    private val corDoFundo = activity.getColor(R.color.superficie)
    private val corDaBorda = activity.getColor(R.color.borda_suave)
    private val corSuave = activity.getColor(R.color.texto_suave)

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
        }

        desenhar()
        dialogo.show()
    }

    private fun montarRaiz(): View {
        val raio = activity.emPx(20f).toFloat()

        val raiz = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(corDoFundo)
                cornerRadii = floatArrayOf(raio, raio, raio, raio, 0f, 0f, 0f, 0f)
            }
            setPadding(activity.emPx(20f), activity.emPx(18f), activity.emPx(20f), activity.emPx(16f))
        }

        // Cabeçalho: título e "Limpar"
        val cabecalho = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        cabecalho.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.filtros_titulo)
                setTextColor(Color.WHITE)
                textSize = 20f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        cabecalho.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.filtros_limpar)
                setTextColor(corAtiva)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(activity.emPx(12f), activity.emPx(8f), activity.emPx(4f), activity.emPx(8f))
                isClickable = true
                isFocusable = true
                setOnClickListener { limpar() }
            }
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
            setTextColor(Color.parseColor("#0B0D0F"))
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = GradientDrawable().apply {
                setColor(corAtiva)
                cornerRadius = activity.emPx(14f).toFloat()
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { dialogo.dismiss() }
        }
        raiz.addView(
            botaoFechar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                activity.emPx(48f)
            ).apply { topMargin = activity.emPx(14f) }
        )

        return raiz
    }

    private fun limpar() {
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

        // Etapas finalizadas
        conteudo.addView(titulo(activity.getString(R.string.filtros_finalizadas_titulo)))
        conteudo.addView(linhaDeFinalizadas(animarDe))

        // País
        val paises = RegrasDeFiltro.paisesDisponiveis(base)
            .map { it to rotuloDoPais(it) }
            .sortedBy { Formatacao.normalizar(it.second) }

        if (paises.isNotEmpty()) {
            conteudo.addView(titulo(activity.getString(R.string.filtros_pais)))
            conteudo.addView(
                grupoDeOpcoes(
                    paises.map { (valor, rotulo) ->
                        Opcao(rotulo, valor in filtros.paises) {
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
            conteudo.addView(titulo(activity.getString(R.string.filtros_mes)))
            conteudo.addView(
                grupoDeOpcoes(
                    meses.map { chave ->
                        Opcao(rotuloDoMes(chave), chave in filtros.meses) {
                            val novos = if (chave in filtros.meses) filtros.meses - chave else filtros.meses + chave
                            mudar(filtros.copy(meses = novos))
                        }
                    }
                )
            )
        }

        val quantas = RegrasDeFiltro.aplicar(etapas, filtros, hoje).size
        botaoFechar.text = activity.getString(R.string.filtros_mostrar, quantas)
    }

    private class Opcao(val rotulo: String, val marcada: Boolean, val aoTocar: () -> Unit)

    private fun titulo(texto: String): View =
        TextView(activity).apply {
            text = texto.uppercase(Idioma.locale(activity))
            setTextColor(corSuave)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.12f
            setPadding(0, activity.emPx(18f), 0, activity.emPx(10f))
        }

    private fun linhaDeFinalizadas(animarDe: Boolean?): View {
        val ligado = filtros.mostrarFinalizadas

        val linha = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            background = fundoDeToque()
            contentDescription = activity.getString(R.string.filtros_finalizadas_opcao)
            setOnClickListener { mudar(filtros.copy(mostrarFinalizadas = !filtros.mostrarFinalizadas)) }
        }

        linha.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.filtros_finalizadas_opcao)
                setTextColor(Color.WHITE)
                textSize = 15f
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        linha.addView(InterruptorModerno(activity, ligado, animarDe))

        return linha
    }

    /** Opções em forma de cápsulas que quebram de linha; as marcadas ficam preenchidas. */
    private fun grupoDeOpcoes(opcoes: List<Opcao>): View {
        val grupo = LinhasQuebradas(activity).apply {
            espacoHorizontal = activity.emPx(8f)
            espacoVertical = activity.emPx(8f)
        }

        for (opcao in opcoes) {
            grupo.addView(
                TextView(activity).apply {
                    text = opcao.rotulo
                    textSize = 14f
                    maxLines = 1
                    gravity = Gravity.CENTER
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setPadding(activity.emPx(14f), 0, activity.emPx(14f), 0)
                    minHeight = activity.emPx(36f)
                    isClickable = true
                    isFocusable = true
                    setTextColor(if (opcao.marcada) Color.parseColor("#0B0D0F") else Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = activity.emPx(18f).toFloat()
                        if (opcao.marcada) {
                            setColor(corAtiva)
                        } else {
                            setColor(Color.TRANSPARENT)
                            setStroke(activity.emPx(1f).coerceAtLeast(1), corDaBorda)
                        }
                    }
                    setOnClickListener { opcao.aoTocar() }
                }
            )
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
