package com.motorsport.calendario

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class FormulaECalendarActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    voltarParaTelaAnterior()
                }
            }
        )

        val eventos = FormulaECalendarJson.carregar(this)

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(emPx(20f), emPx(20f), emPx(20f), emPx(20f))

                setBackgroundColor(
                    getColor(R.color.fundo_tela)
                )
            }

        val voltarTopo =
            criarBotaoVoltar()

        val parametrosTopo =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosTopo.gravity =
            Gravity.LEFT

        parametrosTopo.setMargins(0, 0, 0, emPx(10f))

        layout.addView(
            voltarTopo,
            parametrosTopo
        )

        val titulo =
            TextView(this).apply {

                text =
                    getString(R.string.categoria_calendario, getString(R.string.formula_e), descreverTemporada(eventos))

                textSize = 27f

                setTextColor(
                    getColor(R.color.texto_principal)
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                gravity =
                    Gravity.CENTER

                letterSpacing = 0.04f

                setPadding(0, emPx(10f), 0, emPx(28f))
            }

        layout.addView(
            titulo
        )

        if (eventos.isEmpty()) {
            val aviso =
                TextView(this).apply {

                    text =
                        getString(R.string.calendario_nao_carregado, getString(R.string.nome_formula_e))

                    textSize = 17f

                    setTextColor(
                        getColor(R.color.texto_suave)
                    )

                    gravity =
                        Gravity.CENTER

                    setPadding(0, emPx(20f), 0, emPx(20f))
                }

            layout.addView(
                aviso
            )
        }

        for (evento in eventos) {
            adicionarEtapa(
                layout,
                evento
            )
        }

        val voltarFundo =
            criarBotaoVoltar()

        val parametrosFundo =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosFundo.gravity =
            Gravity.LEFT

        parametrosFundo.setMargins(0, emPx(24f), 0, emPx(20f))

        layout.addView(
            voltarFundo,
            parametrosFundo
        )

        val scroll =
            ScrollView(this).apply {
                addView(layout)
            }

        setContentView(
            scroll
        )
        aplicarInsetsDasBarras()
        pintarBarrasDoSistema(getColor(R.color.fundo_tela))
    }

    // Ex.: eventos de 2026 a 2027 -> "2026/27".
    private fun descreverTemporada(
        eventos: List<FormulaEEvent>
    ): String {
        val anoInicio = eventos.firstOrNull()?.inicio?.takeLast(4)
        val anoFim = eventos.lastOrNull()?.fim?.takeLast(4)

        if (anoInicio == null || anoFim == null) return ""

        return if (anoInicio == anoFim) {
            anoInicio
        } else {
            "$anoInicio/${anoFim.takeLast(2)}"
        }
    }

    private fun criarBotaoVoltar(): TextView {

        return TextView(this).apply {

            text =
                getString(R.string.voltar_botao)

            textSize = 15f

            setTextColor(
                getColor(R.color.texto_suave)
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            gravity =
                Gravity.LEFT

            setPadding(0, emPx(8f), 0, emPx(8f))

            isClickable = true
            isFocusable = true

            setOnClickListener {
                voltarParaTelaAnterior()
            }
        }
    }

    private fun voltarParaTelaAnterior() {

        finish()

        overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }

    private fun adicionarEtapa(
        layout: LinearLayout,
        evento: FormulaEEvent
    ) {

        val bandeira =
            obterBandeira(evento.pais)

        val datas =
            if (evento.inicio == evento.fim) {
                Idioma.data(this, evento.inicio)
            } else {
                "${Idioma.data(this, evento.inicio)} – ${Idioma.data(this, evento.fim)}"
            }

        val card =
            TextView(this).apply {

                text =
                    "⚡ ${Idioma.lugar(context, evento.circuito)} E-Prix\n\n" +
                    "$bandeira ${Idioma.lugar(context, evento.pais)}\n\n" +
                    "📅 $datas"

                textSize = 17f

                setTextColor(
                    getColor(R.color.texto_principal)
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                gravity =
                    Gravity.CENTER

                setPadding(emPx(20f), emPx(20f), emPx(20f), emPx(20f))

                background =
                    getDrawable(
                        R.drawable.card_arredondado
                    )

                isClickable = true
                isFocusable = true

                setOnClickListener {

                    abrirEtapa(
                        evento
                    )
                }
            }

        val parametros =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametros.setMargins(0, emPx(6f), 0, emPx(6f))

        layout.addView(
            card,
            parametros
        )
    }

    private fun obterBandeira(pais: String): String = Bandeiras.emoji(pais, "🏳️")


    private fun abrirEtapa(
        evento: FormulaEEvent
    ) {

        val intent =
            Intent(
                this,
                FormulaEActivity::class.java
            )

        intent.putExtra(
            "ETAPA",
            evento.etapa
        )

        startActivity(
            intent
        )

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }
}