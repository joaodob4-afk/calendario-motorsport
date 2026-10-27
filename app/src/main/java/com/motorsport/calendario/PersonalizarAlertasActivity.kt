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
 * "Personalizar alertas": página aberta pelo painel de notificações. Tem as antecedências
 * (dá para marcar quantas quiser) e, por categoria, o que avisar: todas as sessões, só as
 * corridas ou nenhuma. As escolhas são gravadas e os alarmes refeitos como sempre; aqui só
 * muda onde se escolhe, não como os avisos são enviados.
 * A abertura e a saída usam a transição padrão do Android.
 */
class PersonalizarAlertasActivity : AppCompatActivity() {

    private val corDoTextoSobreCor = Color.parseColor("#0E1012")

    private lateinit var grupoDeAntecedencias: LinhasQuebradas
    private lateinit var secaoDeCategorias: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        montar()

        // Só na abertura: numa troca de tema a página é refeita no lugar, sem animar de novo.
        if (savedInstanceState == null) {
            (grupoDeAntecedencias.parent as? ViewGroup)?.entrarEmCascata()
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

        grupoDeAntecedencias = LinhasQuebradas(this).apply {
            espacoHorizontal = emPx(8f)
            espacoVertical = emPx(8f)
        }
        secaoDeCategorias = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val corpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(emPx(16f), emPx(4f), emPx(16f), emPx(24f))
            addView(tituloDeSecao(R.string.personalizar_antecedencia))
            addView(grupoDeAntecedencias)
            addView(tituloDeSecao(R.string.por_categoria_titulo))
            addView(secaoDeCategorias)
        }

        raiz.addView(
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                addView(corpo)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

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

    private fun tituloDeSecao(textoRes: Int): View =
        TextView(this).apply {
            text = getString(textoRes).uppercase(Idioma.locale(this@PersonalizarAlertasActivity))
            setTextColor(getColor(R.color.texto_suave))
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.12f
            setPadding(emPx(4f), emPx(20f), 0, emPx(10f))
        }

    /** Refaz só as cápsulas de antecedência (a rolagem da página não se mexe). */
    private fun desenharAntecedencias() {
        val selecionados = PreferenciasDeAviso.minutosSelecionados(this)

        grupoDeAntecedencias.removeAllViews()

        for (opcao in PreferenciasDeAviso.todasAsOpcoes.sorted()) {
            val marcada = opcao in selecionados

            grupoDeAntecedencias.addView(
                TextView(this).apply {
                    text = rotuloCurtoDaAntecedencia(this@PersonalizarAlertasActivity, opcao)
                    textSize = 15f
                    maxLines = 1
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    minHeight = emPx(44f)
                    minWidth = emPx(72f)
                    setPadding(emPx(18f), 0, emPx(18f), 0)
                    isClickable = true
                    isFocusable = true
                    contentDescription = descricaoDaAntecedencia(this@PersonalizarAlertasActivity, opcao)
                    setTextColor(if (marcada) corDoTextoSobreCor else getColor(R.color.texto_principal))
                    background = RippleDrawable(
                        ColorStateList.valueOf(Color.parseColor("#334C8DFF")),
                        forma(
                            if (marcada) getColor(R.color.azul_destaque) else getColor(R.color.superficie),
                            if (marcada) null else getColor(R.color.borda_suave)
                        ),
                        forma(Color.WHITE, null)
                    )
                    setOnClickListener {
                        val novos = if (marcada) selecionados - opcao else selecionados + opcao
                        aplicarAntecedencias(novos)
                        if (!marcada) animarPop()
                    }
                    comEfeitoDeToque(escala = 0.94f)
                }
            )
        }
    }

    /** Lista de categorias, uma por linha, com Todas, Corridas e Nenhuma ao lado. */
    private fun desenharCategorias() {
        secaoDeCategorias.removeAllViews()

        val ativo = PreferenciasDeAviso.avisarNaHora(this) ||
            PreferenciasDeAviso.minutosSelecionados(this).isNotEmpty()

        secaoDeCategorias.addView(
            MenuPrincipal.cartaoDeCategorias(this, ativo) { desenharCategorias() }
        )
        secaoDeCategorias.addView(MenuPrincipal.legendaDosModos(this))
    }

    /** Salva, pede permissão de notificação se preciso e refaz os alarmes (igual ao painel). */
    private fun aplicarAntecedencias(minutos: Collection<Int>) {
        PreferenciasDeAviso.definirSelecionados(this, minutos)

        if (minutos.isNotEmpty() && Notificacoes.precisaPedirPermissao(this)) {
            Notificacoes.pedirPermissao(this)
        }

        AgendadorDeAvisos.reagendarEmSegundoPlano(this)
        desenharAntecedencias()
        desenharCategorias()
    }

    private fun forma(preenchimento: Int, borda: Int?) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = emPx(22f).toFloat()
            setColor(preenchimento)
            if (borda != null) setStroke(emPx(1f).coerceAtLeast(1), borda)
        }
}
