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
 * "Personalizar alertas": página que desce do topo com todas as antecedências disponíveis.
 * Dá para marcar quantas quiser. A escolha é gravada e os alarmes refeitos exatamente como no
 * painel de alertas: aqui só muda o que é oferecido, não como os avisos são enviados.
 */
class PersonalizarAlertasActivity : AppCompatActivity() {

    private val corDoTextoSobreCor = Color.parseColor("#0E1012")

    /** Minutos até 45; a partir de uma hora, horas e dia. */
    private val minutos get() = PreferenciasDeAviso.todasAsOpcoes.filter { it < 60 }.sorted()
    private val horas get() = PreferenciasDeAviso.todasAsOpcoes.filter { it >= 60 }.sorted()

    private lateinit var grupoDeMinutos: LinhasQuebradas
    private lateinit var grupoDeHoras: LinhasQuebradas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        montar()
    }

    /** A troca de tema é tratada aqui (uiMode no manifesto): refaz as views no lugar. */
    override fun onConfigurationChanged(novaConfiguracao: android.content.res.Configuration) {
        super.onConfigurationChanged(novaConfiguracao)
        montar()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.parada, R.anim.subir_saida)
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

        grupoDeMinutos = novoGrupo()
        grupoDeHoras = novoGrupo()

        val corpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(emPx(16f), emPx(4f), emPx(16f), emPx(24f))
            addView(tituloDeSecao(R.string.personalizar_minutos))
            addView(grupoDeMinutos)
            addView(tituloDeSecao(R.string.personalizar_horas))
            addView(grupoDeHoras)
        }

        raiz.addView(
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                addView(corpo)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        desenhar()
    }

    private fun cabecalho(): View {
        val voltar = ImageView(this).apply {
            setImageResource(R.drawable.ic_chevron)
            rotation = 180f
            background = getDrawable(R.drawable.bg_botao_icone)
            setPadding(emPx(10f), emPx(10f), emPx(10f), emPx(10f))
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

    private fun novoGrupo() =
        LinhasQuebradas(this).apply {
            espacoHorizontal = emPx(8f)
            espacoVertical = emPx(8f)
        }

    /** Refaz só as cápsulas (a rolagem da página não se mexe). */
    private fun desenhar() {
        val selecionados = PreferenciasDeAviso.minutosSelecionados(this)

        preencher(grupoDeMinutos, minutos, selecionados)
        preencher(grupoDeHoras, horas, selecionados)
    }

    private fun preencher(grupo: LinhasQuebradas, opcoes: List<Int>, selecionados: List<Int>) {
        grupo.removeAllViews()

        for (opcao in opcoes) {
            val marcada = opcao in selecionados

            grupo.addView(
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
                        aplicar(novos)
                        if (!marcada) animarPop()
                    }
                    comEfeitoDeToque(escala = 0.94f)
                }
            )
        }
    }

    /** Salva, pede permissão de notificação se preciso e refaz os alarmes (igual ao painel). */
    private fun aplicar(minutos: Collection<Int>) {
        PreferenciasDeAviso.definirSelecionados(this, minutos)

        if (minutos.isNotEmpty() && Notificacoes.precisaPedirPermissao(this)) {
            Notificacoes.pedirPermissao(this)
        }

        AgendadorDeAvisos.reagendarEmSegundoPlano(this)
        desenhar()
    }

    private fun forma(preenchimento: Int, borda: Int?) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = emPx(22f).toFloat()
            setColor(preenchimento)
            if (borda != null) setStroke(emPx(1f).coerceAtLeast(1), borda)
        }
}
