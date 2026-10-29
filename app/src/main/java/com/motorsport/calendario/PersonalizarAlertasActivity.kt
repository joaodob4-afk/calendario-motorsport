package com.motorsport.calendario

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * "Personalizar alertas": página aberta pelo painel de notificações. Tem "avisar só das
 * categorias da página inicial", as antecedências (dá para marcar quantas quiser) e, por
 * categoria, o sino que escolhe o que avisar: todas as sessões, só corridas ou nenhuma. As
 * escolhas são gravadas e os alarmes refeitos como sempre; aqui só muda onde se escolhe, não
 * como os avisos são enviados.
 * A abertura e a saída usam a transição padrão do Android.
 */
class PersonalizarAlertasActivity : AppCompatActivity() {

    private val corDoTextoSobreCor = Color.parseColor("#0E1012")

    private lateinit var corpo: LinearLayout
    private lateinit var gradeDeAntecedencias: LinearLayout
    private lateinit var contagem: TextView
    private lateinit var cartaoDasFavoritas: LinearLayout
    private lateinit var secaoDeCategorias: LinearLayout

    /** Antecedência recém-marcada: a cápsula refeita "estoura" para confirmar o toque. */
    private var recemMarcada: Int? = null

    private companion object {
        /** Colunas da grade de antecedências; 12 opções formam 3 linhas iguais de 4. */
        const val COLUNAS = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        montar()

        // Só na abertura: numa troca de tema a página é refeita no lugar, sem animar de novo.
        if (savedInstanceState == null) {
            corpo.entrarEmCascata()
        }
    }

    /** A troca de tema é tratada aqui (uiMode no manifesto): refaz as views no lugar. */
    override fun onConfigurationChanged(novaConfiguracao: android.content.res.Configuration) {
        super.onConfigurationChanged(novaConfiguracao)
        montar()
    }

    private fun montar() {
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.fundo_tela))
        }
        setContentView(raiz)
        aplicarInsetsDasBarras()
        pintarBarrasDoSistema(getColor(R.color.azul_marinho))

        raiz.addView(cabecalho())

        gradeDeAntecedencias = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(emPx(8f), emPx(8f), emPx(8f), emPx(8f))
            background = cartao()
        }
        cartaoDasFavoritas = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(emPx(16f), emPx(12f), emPx(12f), emPx(12f))
            background = cartao()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = emPx(4f) }
        }

        contagem = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(corDoTextoSobreCor)
            gravity = Gravity.CENTER
            setPadding(emPx(10f), emPx(3f), emPx(10f), emPx(3f))
            background = GradientDrawable().apply {
                setColor(getColor(R.color.azul_destaque))
                cornerRadius = emPx(6f).toFloat()
            }
        }

        secaoDeCategorias = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        corpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(emPx(16f), emPx(4f), emPx(16f), emPx(28f))
            addView(cartaoDasFavoritas)
            addView(tituloDaPrimeiraSecao())
            addView(gradeDeAntecedencias)
            addView(tituloDeSecao(R.string.personalizar_categorias))
            addView(secaoDeCategorias)
        }

        raiz.addView(
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                addView(corpo)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        desenharFavoritas()
        desenharAntecedencias()
        desenharCategorias()
    }

    private fun cabecalho(): View {
        val voltar = ImageView(this).apply {
            setImageResource(R.drawable.ic_seta_voltar)
            background = getDrawable(R.drawable.bg_botao_icone)
            setPadding(emPx(12f), emPx(12f), emPx(12f), emPx(12f))
            isClickable = true
            isFocusable = true
            contentDescription = getString(R.string.cd_voltar)
            layoutParams = LinearLayout.LayoutParams(emPx(48f), emPx(48f)).apply { marginStart = emPx(8f) }
            setOnClickListener { finish() }
            comEfeitoDeToque(escala = 0.9f)
        }

        val titulo = TextView(this).apply {
            text = getString(R.string.personalizar_alertas)
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = emPx(12f) }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(getColor(R.color.azul_marinho))
            setPadding(0, emPx(6f), 0, emPx(6f))
            addView(voltar)
            addView(titulo)
        }
    }

    /** "ALERTA POR ANTECEDÊNCIA" e, à direita, quantas estão marcadas. */
    private fun tituloDaPrimeiraSecao(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                tituloDeSecao(R.string.personalizar_antecedencia).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
            )
            addView(contagem)
        }

    private fun cartao() =
        GradientDrawable().apply {
            setColor(getColor(R.color.superficie))
            setStroke(emPx(1f).coerceAtLeast(1), getColor(R.color.borda_suave))
            cornerRadius = emPx(12f).toFloat()
        }

    private fun tituloDeSecao(textoRes: Int): TextView =
        TextView(this).apply {
            text = getString(textoRes).uppercase(Idioma.locale(this@PersonalizarAlertasActivity))
            setTextColor(getColor(R.color.texto_suave))
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.12f
            setPadding(emPx(4f), emPx(20f), 0, emPx(10f))
        }

    /** Refaz só a grade de antecedências (a rolagem da página não se mexe). */
    private fun desenharAntecedencias() {
        val selecionados = PreferenciasDeAviso.minutosSelecionados(this)
        val opcoes = PreferenciasDeAviso.todasAsOpcoes.sorted()

        gradeDeAntecedencias.removeAllViews()

        contagem.visibility = if (selecionados.isEmpty()) View.GONE else View.VISIBLE
        contagem.text = getString(R.string.personalizar_marcadas, selecionados.size)

        opcoes.chunked(COLUNAS).forEach { linha ->
            val linhaView = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

            repeat(COLUNAS) { coluna ->
                val opcao = linha.getOrNull(coluna)
                linhaView.addView(
                    if (opcao != null) capsula(opcao, opcao in selecionados, selecionados) else espaco()
                )
            }

            gradeDeAntecedencias.addView(linhaView)
        }
    }

    private fun parametrosDaCelula() =
        LinearLayout.LayoutParams(0, emPx(52f), 1f).apply {
            setMargins(emPx(4f), emPx(4f), emPx(4f), emPx(4f))
        }

    private fun espaco(): View = View(this).apply { layoutParams = parametrosDaCelula() }

    private fun capsula(opcao: Int, marcada: Boolean, selecionados: List<Int>): View =
        TextView(this).apply {
            text = rotuloCurtoDaAntecedencia(this@PersonalizarAlertasActivity, opcao)
            textSize = 15f
            maxLines = 1
            gravity = Gravity.CENTER
            includeFontPadding = false
            typeface = Typeface.create("sans-serif-medium", if (marcada) Typeface.BOLD else Typeface.NORMAL)
            layoutParams = parametrosDaCelula()
            isClickable = true
            isFocusable = true
            contentDescription = descricaoDaAntecedencia(this@PersonalizarAlertasActivity, opcao)
            setTextColor(if (marcada) corDoTextoSobreCor else getColor(R.color.texto_principal))
            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#334C8DFF")),
                forma(
                    if (marcada) getColor(R.color.azul_destaque) else getColor(R.color.fundo_tela),
                    if (marcada) null else getColor(R.color.borda_suave)
                ),
                forma(Color.WHITE, null)
            )
            setOnClickListener {
                val novos = if (marcada) selecionados - opcao else selecionados + opcao
                recemMarcada = if (marcada) null else opcao
                aplicarAntecedencias(novos)
            }
            comEfeitoDeToque(escala = 0.93f)

            if (opcao == recemMarcada) {
                recemMarcada = null
                post { animarPop() }
            }
        }

    /** Estado do interruptor antes do toque; faz a bolinha deslizar quando o cartão é refeito. */
    private var favoritasAntes: Boolean? = null

    /** "Avisar só das favoritas": as outras categorias ficam sem avisos, sem perder o que foi escolhido. */
    private fun desenharFavoritas() {
        cartaoDasFavoritas.removeAllViews()

        val ligado = PreferenciasDeAviso.avisarSoFavoritas(this)
        val anterior = favoritasAntes
        favoritasAntes = ligado

        val textos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = emPx(8f) }
            addView(
                TextView(this@PersonalizarAlertasActivity).apply {
                    text = getString(R.string.avisar_so_favoritas)
                    setTextColor(getColor(R.color.texto_principal))
                    textSize = 16f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
            )
            addView(
                TextView(this@PersonalizarAlertasActivity).apply {
                    text = getString(R.string.avisar_so_favoritas_dica)
                    setTextColor(getColor(R.color.texto_suave))
                    textSize = 13f
                    setPadding(0, emPx(2f), 0, 0)
                }
            )
        }

        val interruptor = InterruptorModerno(
            this,
            ligado,
            anterior,
            corLigado = getColor(R.color.azul_destaque),
            corDaTrilha = getColor(R.color.fundo_tela),
            corDoContorno = getColor(R.color.borda_suave)
        ).apply {
            isClickable = true
            isFocusable = true
            contentDescription = getString(R.string.avisar_so_favoritas)
            setOnClickListener {
                PreferenciasDeAviso.definirAvisarSoFavoritas(this@PersonalizarAlertasActivity, !ligado)
                AgendadorDeAvisos.reagendarEmSegundoPlano(this@PersonalizarAlertasActivity)
                desenharFavoritas()
                desenharCategorias()
            }
        }

        cartaoDasFavoritas.addView(textos)
        cartaoDasFavoritas.addView(interruptor)
    }

    private fun rotuloDoModo(modo: ModoDeAviso): String =
        getString(
            when (modo) {
                ModoDeAviso.TODAS -> R.string.modo_todas
                ModoDeAviso.CORRIDAS -> R.string.modo_so_corridas
                ModoDeAviso.NENHUMA -> R.string.modo_nenhuma
            }
        )

    /**
     * Uma linha por categoria, num cartão só: faixa na cor dela, o nome, o que ela avisa e o sino.
     * Tocar no sino troca o aviso: todas as sessões, só corridas ou nenhuma.
     */
    private fun desenharCategorias() {
        secaoDeCategorias.removeAllViews()

        val lista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cartao()
        }

        Categorias.todas.forEachIndexed { indice, categoria ->
            if (indice > 0) {
                lista.addView(
                    View(this).apply {
                        setBackgroundColor(getColor(R.color.borda_suave))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            emPx(1f).coerceAtLeast(1)
                        ).apply { marginStart = emPx(16f); marginEnd = emPx(16f) }
                    }
                )
            }
            lista.addView(linhaDaCategoria(categoria))
        }

        secaoDeCategorias.addView(lista)
        secaoDeCategorias.addView(
            TextView(this).apply {
                text = getString(R.string.campeonato_legenda)
                setTextColor(getColor(R.color.texto_suave))
                textSize = 12f
                setLineSpacing(0f, 1.15f)
                setPadding(emPx(4f), emPx(10f), emPx(4f), 0)
            }
        )
    }

    private fun linhaDaCategoria(categoria: CategoriaInfo): View {
        val cor = Color.parseColor(categoria.cor)

        val faixa = View(this).apply {
            background = GradientDrawable().apply {
                setColor(cor)
                cornerRadius = emPx(2f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(emPx(4f), emPx(34f))
                .apply { marginStart = emPx(4f); marginEnd = emPx(14f) }
        }

        val nome = TextView(this).apply {
            text = categoria.nome
            setTextColor(getColor(R.color.texto_principal))
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 1
        }

        val aviso = TextView(this).apply {
            setTextColor(getColor(R.color.texto_suave))
            textSize = 12f
            maxLines = 1
            setPadding(0, emPx(2f), 0, 0)
        }

        val sino = ImageView(this).apply {
            setImageResource(R.drawable.ic_sino)
            setPadding(emPx(8f), emPx(8f), emPx(8f), emPx(8f))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(emPx(44f), emPx(44f))
            background = RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#334C8DFF")),
                null,
                forma(Color.WHITE, null)
            )
            comEfeitoDeToque(escala = 0.88f)
        }

        /** Texto do aviso e cor do sino, de acordo com o que a categoria realmente avisa agora. */
        fun pintar() {
            val silenciada = PreferenciasDeAviso.silenciadaPorFavoritas(this, categoria.nome)
            val modo = PreferenciasDeAviso.modoSalvoDaCategoria(this, categoria.nome)
            val avisa = !silenciada && modo != ModoDeAviso.NENHUMA

            aviso.text = if (silenciada) {
                getString(R.string.favoritas_silenciada)
            } else {
                getString(R.string.favoritas_avisos, rotuloDoModo(modo))
            }
            sino.setColorFilter(if (avisa) getColor(R.color.azul_destaque) else getColor(R.color.texto_suave))
            sino.alpha = if (silenciada) 0.5f else 1f
            sino.contentDescription = getString(R.string.cd_alternar_aviso, categoria.nome, rotuloDoModo(modo))
        }

        val textos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(nome)
            addView(aviso)
        }

        sino.setOnClickListener {
            val proximo = when (PreferenciasDeAviso.modoSalvoDaCategoria(this, categoria.nome)) {
                ModoDeAviso.TODAS -> ModoDeAviso.CORRIDAS
                ModoDeAviso.CORRIDAS -> ModoDeAviso.NENHUMA
                ModoDeAviso.NENHUMA -> ModoDeAviso.TODAS
            }

            PreferenciasDeAviso.definirModo(this, categoria.nome, proximo)
            AgendadorDeAvisos.reagendarEmSegundoPlano(this)
            pintar()
            sino.animarPop()
        }

        pintar()

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = emPx(64f)
            setPadding(emPx(10f), emPx(6f), emPx(8f), emPx(6f))
            addView(faixa)
            addView(textos)
            addView(sino)
        }
    }

    /** Salva, pede permissão de notificação se preciso e refaz os alarmes (igual ao painel). */
    private fun aplicarAntecedencias(minutos: Collection<Int>) {
        PreferenciasDeAviso.definirSelecionados(this, minutos)

        if (minutos.isNotEmpty() && Notificacoes.precisaPedirPermissao(this)) {
            Notificacoes.pedirPermissao(this)
        }

        AgendadorDeAvisos.reagendarEmSegundoPlano(this)
        desenharAntecedencias()
    }

    private fun forma(preenchimento: Int, borda: Int?) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = emPx(8f).toFloat()
            setColor(preenchimento)
            if (borda != null) setStroke(emPx(1f).coerceAtLeast(1), borda)
        }
}
