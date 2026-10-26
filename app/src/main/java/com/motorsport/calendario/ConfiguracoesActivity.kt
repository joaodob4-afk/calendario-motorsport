package com.motorsport.calendario

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Configurações do app: aparência (padrão do sistema, claro ou escuro) e o formato de hora,
 * que segue o do aparelho. Para incluir uma configuração nova, crie um bloco e chame-o em [montar].
 */
class ConfiguracoesActivity : AppCompatActivity() {

    private class OpcaoDeTema(
        val modo: ModoDeTema,
        val icone: Int,
        val titulo: Int,
        val descricao: Int
    )

    private val opcoesDeTema = listOf(
        OpcaoDeTema(ModoDeTema.SISTEMA, R.drawable.ic_tema_sistema, R.string.tema_sistema, R.string.tema_sistema_desc),
        OpcaoDeTema(ModoDeTema.CLARO, R.drawable.ic_tema_sol, R.string.tema_claro, R.string.tema_claro_desc),
        OpcaoDeTema(ModoDeTema.ESCURO, R.drawable.ic_tema_lua, R.string.tema_escuro, R.string.tema_escuro_desc)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        montar()
    }

    /** A troca de tema é tratada aqui (uiMode no manifesto): refaz as views no lugar, sem piscar. */
    override fun onConfigurationChanged(novaConfiguracao: android.content.res.Configuration) {
        super.onConfigurationChanged(novaConfiguracao)
        montar()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /** Rolagem atual; guardada para a tela refeita voltar exatamente ao mesmo ponto. */
    private var rolagem: ScrollView? = null

    private fun montar() {
        val posicaoDaRolagem = rolagem?.scrollY ?: 0
        val marinho = getColor(R.color.azul_marinho)

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.fundo_tela))
        }
        setContentView(raiz)
        aplicarInsetsDasBarras()

        // O azul-marinho aparece atrás das barras do sistema, junto do cabeçalho.
        pintarBarrasDoSistema(marinho)

        raiz.addView(cabecalho())

        val corpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(emPx(16f), emPx(4f), emPx(16f), emPx(24f))
        }

        corpo.addView(tituloDeSecao(R.string.config_aparencia))
        corpo.addView(cartaoDeTemas())

        corpo.addView(tituloDeSecao(R.string.config_data))
        corpo.addView(cartaoDeData())

        corpo.addView(tituloDeSecao(R.string.config_hora))
        corpo.addView(cartaoDeHora())

        val novaRolagem = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(corpo)
        }
        rolagem = novaRolagem

        raiz.addView(
            novaRolagem,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        // Tocar numa opção refaz a tela; sem isto a lista voltava ao topo a cada toque.
        // O ponto é restaurado antes do primeiro desenho, então nada pisca nem "sobe".
        if (posicaoDaRolagem > 0) {
            novaRolagem.viewTreeObserver.addOnPreDrawListener(
                object : android.view.ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        novaRolagem.viewTreeObserver.removeOnPreDrawListener(this)
                        novaRolagem.scrollTo(0, posicaoDaRolagem)
                        return true
                    }
                }
            )
        }
    }

    // ---------------------------------------------------------------- blocos

    private fun cabecalho(): View {
        val voltar = ImageView(this).apply {
            setImageResource(R.drawable.ic_seta_voltar)
            background = this@ConfiguracoesActivity.getDrawable(R.drawable.bg_botao_icone)
            setPadding(emPx(12f), emPx(12f), emPx(12f), emPx(12f))
            isClickable = true
            isFocusable = true
            contentDescription = getString(R.string.cd_voltar)
            layoutParams = LinearLayout.LayoutParams(emPx(48f), emPx(48f)).apply { marginStart = emPx(8f) }
            setOnClickListener { finish() }
            comEfeitoDeToque(escala = 0.9f)
        }

        val titulo = TextView(this).apply {
            text = getString(R.string.config_titulo)
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

    private fun tituloDeSecao(textoRes: Int): View =
        TextView(this).apply {
            text = getString(textoRes).uppercase(Idioma.locale(this@ConfiguracoesActivity))
            setTextColor(getColor(R.color.texto_suave))
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.12f
            setPadding(emPx(4f), emPx(20f), 0, emPx(10f))
        }

    private fun cartaoDeTemas(): View {
        val atual = PreferenciasDeTema.atual(this)
        val cartao = cartao()

        opcoesDeTema.forEachIndexed { indice, opcao ->
            if (indice > 0) cartao.addView(divisor())

            val linha = linhaBase(opcao.icone, getString(opcao.titulo), getString(opcao.descricao))
            linha.addView(indicadorDeEscolha(opcao.modo == atual))
            tornarEscolhivel(linha, getString(opcao.titulo)) { escolher(opcao.modo) }
            cartao.addView(linha)
        }

        return cartao
    }

    /** Cada formato mostra a data de hoje escrita nele; "Padrão do aparelho" mostra o que o celular usaria. */
    private fun cartaoDeData(): View {
        val atual = PreferenciasDeData.atual(this)
        val cartao = cartao()
        val hoje = java.time.LocalDate.now()

        FormatoDeData.values().forEachIndexed { indice, formato ->
            if (indice > 0) cartao.addView(divisor())

            val amostra = FormatadorDeData.curta(hoje, PreferenciasDeData.resolver(this, formato))
            val titulo = getString(rotuloDoFormato(formato))
            val descricao = if (formato == FormatoDeData.SISTEMA) {
                getString(R.string.data_sistema_desc, amostra)
            } else {
                amostra
            }

            val linha = linhaBase(R.drawable.ic_calendario, titulo, descricao)
            linha.addView(indicadorDeEscolha(formato == atual))
            tornarEscolhivel(linha, titulo) { escolherData(formato) }
            cartao.addView(linha)
        }

        return cartao
    }

    private fun rotuloDoFormato(formato: FormatoDeData): Int =
        when (formato) {
            FormatoDeData.SISTEMA -> R.string.data_sistema
            FormatoDeData.DIA_MES -> R.string.data_dia_mes
            FormatoDeData.MES_DIA -> R.string.data_mes_dia
        }

    /** Deixa a linha tocável, com ondulação azul. */
    private fun tornarEscolhivel(linha: LinearLayout, descricao: String, aoTocar: () -> Unit) {
        linha.isClickable = true
        linha.isFocusable = true
        linha.contentDescription = descricao
        linha.background = RippleDrawable(
            ColorStateList.valueOf(Color.parseColor("#334C8DFF")),
            null,
            ColorDrawable(Color.WHITE)
        )
        linha.setOnClickListener { aoTocar() }
    }

    private fun cartaoDeHora(): View {
        val descricao = if (Hora.usa24Horas(this)) R.string.hora_segue_24 else R.string.hora_segue_12

        return cartao().apply {
            addView(linhaBase(R.drawable.ic_relogio, getString(R.string.hora_titulo), getString(descricao)))
        }
    }

    // ---------------------------------------------------------------- ações

    private fun escolher(modo: ModoDeTema) {
        if (PreferenciasDeTema.atual(this) == modo) return

        // Se o tema efetivo mudar, o Android refaz esta tela sozinho; senão, só atualizamos a marcação.
        PreferenciasDeTema.definir(this, modo)
        montar()
    }

    private fun escolherData(formato: FormatoDeData) {
        if (PreferenciasDeData.atual(this) == formato) return

        PreferenciasDeData.definir(this, formato)
        montar()
    }

    // ---------------------------------------------------------------- apoio

    private fun cartao(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = forma(getColor(R.color.superficie), getColor(R.color.borda_suave), 16f)
        }

    private fun divisor(): View =
        View(this).apply {
            setBackgroundColor(getColor(R.color.borda_suave))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                emPx(1f).coerceAtLeast(1)
            ).apply { marginStart = emPx(70f) }
        }

    /** Ícone num quadrado azul-marinho, título e descrição. Ao fim da linha cabe um indicador. */
    private fun linhaBase(iconeRes: Int, titulo: String, descricao: String): LinearLayout {
        val icone = ImageView(this).apply {
            setImageResource(iconeRes)
            setPadding(emPx(9f), emPx(9f), emPx(9f), emPx(9f))
            background = forma(getColor(R.color.azul_marinho_claro), null, 12f)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = LinearLayout.LayoutParams(emPx(40f), emPx(40f))
        }

        val textos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = emPx(14f); marginEnd = emPx(8f) }

            addView(
                TextView(context).apply {
                    text = titulo
                    setTextColor(getColor(R.color.texto_principal))
                    textSize = 16f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                }
            )
            addView(
                TextView(context).apply {
                    text = descricao
                    setTextColor(getColor(R.color.texto_suave))
                    textSize = 13f
                    setPadding(0, emPx(2f), 0, 0)
                }
            )
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = emPx(68f)
            setPadding(emPx(14f), emPx(10f), emPx(14f), emPx(10f))
            addView(icone)
            addView(textos)
        }
    }

    /** Círculo à direita da linha: preenchido de azul com um "✓" na opção escolhida. */
    private fun indicadorDeEscolha(marcado: Boolean): View {
        val tamanho = emPx(24f)

        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(tamanho, tamanho)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (marcado) {
                    setColor(getColor(R.color.azul_destaque))
                } else {
                    setColor(Color.TRANSPARENT)
                    setStroke(emPx(2f), getColor(R.color.borda_suave))
                }
            }
            if (marcado) {
                addView(
                    ImageView(context).apply {
                        setImageResource(R.drawable.ic_check)
                        layoutParams = FrameLayout.LayoutParams(emPx(16f), emPx(16f), Gravity.CENTER)
                    }
                )
            }
        }
    }

    private fun forma(preenchimento: Int, borda: Int?, raioDp: Float) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = emPx(raioDp).toFloat()
            setColor(preenchimento)
            if (borda != null) setStroke(emPx(1f).coerceAtLeast(1), borda)
        }
}
