package com.motorsport.calendario

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

/**
 * Conteúdo do painel que abre pela direita da tela inicial (botão de filtros).
 *
 * Ordem: categorias (mostram só aquela categoria na lista), o interruptor "só favoritas" e a
 * lista de etapas, cada uma com uma estrela para favoritar. Os toques já atualizam a lista da
 * tela inicial. As partes são redesenhadas no lugar, sem refazer o painel inteiro, para a
 * rolagem não voltar ao topo a cada toque.
 */
internal class MenuDeFiltros(
    private val activity: Activity,
    private val container: LinearLayout,
    private val todasEtapas: () -> List<MainActivity.Etapa>,
    private val sessoes: () -> List<SessaoParaAviso>,
    private val filtros: () -> FiltrosDeEtapas,
    private val nomeDaEtapa: (MainActivity.Etapa) -> String,
    private val aoMudar: (FiltrosDeEtapas) -> Unit
) {

    private companion object {
        const val LINHAS_POR_LOTE = 24
        val corDaEstrela: Int = Color.parseColor("#FFC83D")
        val corDoTextoSobreCor: Int = Color.parseColor("#0B0D0F")
    }

    private val handler = Handler(Looper.getMainLooper())
    private val categorias = LinhasQuebradas(activity)
    private val cartaoDoInterruptor = LinearLayout(activity)
    private val tituloDasEtapas = TextView(activity)
    private val listaDeEtapas = LinearLayout(activity)

    /** Muda a cada redesenho da lista; lotes de um desenho antigo se cancelam ao ver que mudou. */
    private var geracao = 0

    private var estadoAnteriorDoInterruptor: Boolean? = null

    /** Monta o painel. Chamado ao criar a tela e quando as etapas terminam de carregar. */
    fun montar() {
        container.removeAllViews()

        container.addView(cabecalho())

        container.addView(titulo(activity.getString(R.string.filtros_categorias), topo = 22f))
        categorias.apply {
            espacoHorizontal = activity.emPx(8f)
            espacoVertical = activity.emPx(8f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(categorias)

        container.addView(titulo(activity.getString(R.string.filtros_favoritas_titulo), topo = 24f))
        cartaoDoInterruptor.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(activity.emPx(16f), activity.emPx(8f), activity.emPx(10f), activity.emPx(8f))
            background = forma(activity.getColor(R.color.superficie), activity.getColor(R.color.borda_suave), 16f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(cartaoDoInterruptor)

        tituloDasEtapas.apply {
            setTextColor(activity.getColor(R.color.texto_suave))
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.12f
            setPadding(activity.emPx(2f), activity.emPx(18f), 0, activity.emPx(10f))
        }
        container.addView(tituloDasEtapas)

        listaDeEtapas.orientation = LinearLayout.VERTICAL
        container.addView(
            listaDeEtapas,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        desenharCategorias()
        desenharInterruptor()
        desenharEtapas()
    }

    /** Refaz só a parte que depende do filtro atual (usado quando ele muda por fora do painel). */
    fun atualizar() {
        desenharCategorias()
        desenharInterruptor()
        desenharEtapas()
    }

    // ---------------------------------------------------------------- blocos

    private fun cabecalho(): View {
        val icone = ImageView(activity).apply {
            setImageResource(R.drawable.ic_filtro)
            setColorFilter(Color.WHITE)
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
            text = activity.getString(R.string.filtros_titulo)
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

    private fun titulo(texto: String, topo: Float): View =
        TextView(activity).apply {
            text = texto.uppercase(Idioma.locale(activity))
            setTextColor(activity.getColor(R.color.texto_suave))
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.12f
            setPadding(activity.emPx(2f), activity.emPx(topo), 0, activity.emPx(10f))
        }

    /** Uma cápsula por categoria; tocar escolhe só ela, tocar de novo volta a mostrar todas. */
    private fun desenharCategorias() {
        categorias.removeAllViews()

        val escolhida = Formatacao.normalizar(filtros().categoria)

        for (categoria in Categorias.todas) {
            val cor = Color.parseColor(categoria.cor)
            val marcada = escolhida.isNotEmpty() && Formatacao.normalizar(categoria.nome) == escolhida

            val capsula = TextView(activity).apply {
                text = categoria.nome
                textSize = 14f
                maxLines = 1
                gravity = Gravity.CENTER
                includeFontPadding = false
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                minHeight = activity.emPx(40f)
                isClickable = true
                isFocusable = true
                contentDescription = categoria.nome

                if (marcada) {
                    setPadding(activity.emPx(16f), 0, activity.emPx(16f), 0)
                    setTextColor(corDoTextoSobreCor)
                    background = GradientDrawable().apply {
                        setColor(cor)
                        cornerRadius = activity.emPx(20f).toFloat()
                    }
                } else {
                    setPadding(activity.emPx(12f), 0, activity.emPx(16f), 0)
                    setTextColor(activity.getColor(R.color.texto_principal))
                    background = GradientDrawable().apply {
                        setColor(ColorUtils.blendARGB(activity.getColor(R.color.superficie), cor, 0.12f))
                        cornerRadius = activity.emPx(20f).toFloat()
                        setStroke(
                            activity.emPx(1f).coerceAtLeast(1),
                            ColorUtils.blendARGB(activity.getColor(R.color.borda_suave), cor, 0.5f)
                        )
                    }
                    setCompoundDrawablesRelativeWithIntrinsicBounds(
                        GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(cor)
                            setSize(activity.emPx(10f), activity.emPx(10f))
                        },
                        null,
                        null,
                        null
                    )
                    compoundDrawablePadding = activity.emPx(8f)
                }

                setOnClickListener {
                    val novaCategoria = if (marcada) "" else categoria.nome
                    aoMudar(filtros().copy(categoria = novaCategoria))
                    desenharCategorias()
                    desenharEtapas()
                    if (!marcada) animarPop()
                }
                comEfeitoDeToque(escala = 0.94f)
            }

            categorias.addView(capsula)
        }
    }

    /** "Mostrar só favoritas": a tela inicial lista apenas as etapas com estrela. */
    private fun desenharInterruptor() {
        cartaoDoInterruptor.removeAllViews()

        val atual = filtros()
        val ligado = atual.somenteFavoritas
        val anterior = estadoAnteriorDoInterruptor
        estadoAnteriorDoInterruptor = ligado

        val textos = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = activity.emPx(8f) }
            addView(
                TextView(activity).apply {
                    text = activity.getString(R.string.filtros_so_favoritas)
                    setTextColor(activity.getColor(R.color.texto_principal))
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                }
            )
            addView(
                TextView(activity).apply {
                    text = activity.getString(R.string.filtros_so_favoritas_dica)
                    setTextColor(activity.getColor(R.color.texto_suave))
                    textSize = 12f
                    setPadding(0, activity.emPx(2f), 0, 0)
                }
            )
        }

        val interruptor = InterruptorModerno(
            activity,
            ligado,
            anterior,
            corLigado = activity.getColor(R.color.azul_destaque),
            corDaTrilha = activity.getColor(R.color.fundo_tela),
            corDoContorno = activity.getColor(R.color.borda_suave)
        ).apply {
            isClickable = true
            isFocusable = true
            contentDescription = activity.getString(R.string.filtros_so_favoritas)
            setOnClickListener {
                aoMudar(filtros().copy(somenteFavoritas = !ligado))
                desenharInterruptor()
            }
        }

        cartaoDoInterruptor.addView(textos)
        cartaoDoInterruptor.addView(interruptor)
    }

    private sealed class Linha {
        class Mes(val mes: YearMonth) : Linha()
        class Etapa(val etapa: MainActivity.Etapa) : Linha()
    }

    /**
     * A lista de etapas (já respeitando a categoria e as finalizadas escolhidas na tela inicial),
     * agrupada por mês. Criada em lotes pequenos para o painel abrir sem travar.
     */
    private fun desenharEtapas() {
        geracao++
        val minhaGeracao = geracao
        handler.removeCallbacksAndMessages(null)

        listaDeEtapas.removeAllViews()

        val etapas = RegrasDeFiltro.aplicar(
            todasEtapas(),
            filtros().copy(somenteFavoritas = false),
            LocalDate.now(),
            sessoes()
        )

        val favoritas = filtros().favoritas.size
        val titulo = activity.getString(R.string.filtros_etapas)
        tituloDasEtapas.text =
            (if (favoritas > 0) "$titulo · ★ $favoritas" else titulo).uppercase(Idioma.locale(activity))

        if (etapas.isEmpty()) {
            listaDeEtapas.addView(
                TextView(activity).apply {
                    text = activity.getString(R.string.lista_vazia)
                    setTextColor(activity.getColor(R.color.texto_suave))
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setPadding(0, activity.emPx(32f), 0, activity.emPx(32f))
                }
            )
            return
        }

        val linhas = ArrayList<Linha>()
        var mesAnterior: YearMonth? = null

        for (etapa in etapas) {
            val inicio = RegrasDeFiltro.inicio(etapa)

            if (inicio != null) {
                val mes = YearMonth.from(inicio)
                if (mes != mesAnterior) {
                    linhas.add(Linha.Mes(mes))
                    mesAnterior = mes
                }
            }

            linhas.add(Linha.Etapa(etapa))
        }

        var proxima = 0
        var cartaoAtual: LinearLayout? = null

        fun desenharLote() {
            if (minhaGeracao != geracao) return

            var criadas = 0

            while (proxima < linhas.size && criadas < LINHAS_POR_LOTE) {
                when (val linha = linhas[proxima]) {
                    is Linha.Mes -> {
                        listaDeEtapas.addView(rotuloDoMes(linha.mes, primeiro = listaDeEtapas.childCount == 0))
                        cartaoAtual = novoCartao().also { listaDeEtapas.addView(it) }
                    }

                    is Linha.Etapa -> {
                        val cartao = cartaoAtual ?: novoCartao().also {
                            listaDeEtapas.addView(it)
                            cartaoAtual = it
                        }

                        if (cartao.childCount > 0) cartao.addView(divisor())
                        cartao.addView(linhaDaEtapa(linha.etapa))
                    }
                }

                proxima++
                criadas++
            }

            if (proxima < linhas.size) handler.post { desenharLote() }
        }

        desenharLote()
    }

    private fun novoCartao(): LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = forma(activity.getColor(R.color.superficie), activity.getColor(R.color.borda_suave), 16f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
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

    private fun rotuloDoMes(mes: YearMonth, primeiro: Boolean): View {
        val locale = Idioma.locale(activity)
        val nome = mes.month.getDisplayName(TextStyle.FULL, locale).uppercase(locale)
        val ano = if (mes.year != LocalDate.now().year) " ${mes.year}" else ""

        return TextView(activity).apply {
            text = nome + ano
            setTextColor(activity.getColor(R.color.texto_suave))
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.14f
            setPadding(activity.emPx(4f), activity.emPx(if (primeiro) 0f else 14f), 0, activity.emPx(6f))
        }
    }

    /** Estrela, cor da categoria, nome do evento e datas. Tocar em qualquer lugar favorita. */
    private fun linhaDaEtapa(etapa: MainActivity.Etapa): View {
        val chave = RegrasDeFiltro.chaveDaEtapa(etapa)
        val cor = Color.parseColor(Categorias.corHex(etapa.categoria))
        val nome = nomeDaEtapa(etapa)

        val estrela = ImageView(activity).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setPadding(activity.emPx(8f), activity.emPx(8f), activity.emPx(8f), activity.emPx(8f))
            layoutParams = LinearLayout.LayoutParams(activity.emPx(40f), activity.emPx(40f))
        }

        fun pintar(favorita: Boolean) {
            estrela.setImageResource(if (favorita) R.drawable.ic_estrela_cheia else R.drawable.ic_estrela)
            estrela.setColorFilter(if (favorita) corDaEstrela else activity.getColor(R.color.texto_suave))
        }

        val faixa = View(activity).apply {
            background = GradientDrawable().apply {
                setColor(cor)
                cornerRadius = activity.emPx(2f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(activity.emPx(4f), activity.emPx(34f))
                .apply { marginStart = activity.emPx(4f); marginEnd = activity.emPx(10f) }
        }

        val textos = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

            addView(
                TextView(activity).apply {
                    text = nome
                    setTextColor(activity.getColor(R.color.texto_principal))
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }
            )
            addView(
                TextView(activity).apply {
                    text = "${Categorias.rotuloDoSelo(etapa.categoria)} · ${datasDaEtapa(etapa)}"
                    setTextColor(activity.getColor(R.color.texto_suave))
                    textSize = 12f
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(0, activity.emPx(1f), 0, 0)
                }
            )
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = activity.emPx(56f)
            setPadding(activity.emPx(6f), activity.emPx(6f), activity.emPx(6f), activity.emPx(6f))
            isClickable = true
            isFocusable = true
            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#334C8DFF")),
                null,
                ColorDrawable(Color.WHITE)
            )

            addView(faixa)
            addView(textos)
            addView(estrela)

            var favorita = chave in filtros().favoritas
            pintar(favorita)
            contentDescription = activity.getString(
                if (favorita) R.string.cd_desfavoritar else R.string.cd_favoritar,
                nome
            )

            setOnClickListener {
                val atuais = filtros().favoritas
                favorita = chave !in atuais
                val novas = if (favorita) atuais + chave else atuais - chave

                pintar(favorita)
                contentDescription = activity.getString(
                    if (favorita) R.string.cd_desfavoritar else R.string.cd_favoritar,
                    nome
                )
                if (favorita) estrela.animarPop()

                aoMudar(filtros().copy(favoritas = novas))
                atualizarContagem()
            }
        }
    }

    /** Só o número de favoritas no título da lista, sem refazer as linhas. */
    private fun atualizarContagem() {
        val favoritas = filtros().favoritas.size
        val titulo = activity.getString(R.string.filtros_etapas)

        tituloDasEtapas.text =
            (if (favoritas > 0) "$titulo · ★ $favoritas" else titulo).uppercase(Idioma.locale(activity))
    }

    /** "23/10" ou "23/10 – 25/10", no formato de data escolhido nas configurações. */
    private fun datasDaEtapa(etapa: MainActivity.Etapa): String {
        val inicio = RegrasDeFiltro.inicio(etapa) ?: return etapa.data
        val fim = RegrasDeFiltro.fim(etapa, inicio)

        val comeco = PreferenciasDeData.curta(activity, inicio)

        return if (fim.isAfter(inicio)) "$comeco – ${PreferenciasDeData.curta(activity, fim)}" else comeco
    }

    private fun forma(preenchimento: Int, borda: Int?, raioDp: Float) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = activity.emPx(raioDp).toFloat()
            setColor(preenchimento)
            if (borda != null) setStroke(activity.emPx(1f).coerceAtLeast(1), borda)
        }
}
