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

class IndyCarCalendarActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        // As telas de cada etapa ainda só têm o visual escuro: ficam escuras mesmo com o tema claro.
        delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
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

        val eventos = IndyCarCalendarJson.carregar(this)

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
                    getString(R.string.categoria_calendario, "INDYCAR", descreverTemporada(eventos))

                textSize = 27f

                setTextColor(
                    Color.WHITE
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
                        getString(R.string.calendario_nao_carregado, getString(R.string.nome_indycar))

                    textSize = 17f

                    setTextColor(
                        Color.LTGRAY
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
    }

    // Ex.: eventos de 2026 a 2027 -> "2026/27".
    private fun descreverTemporada(
        eventos: List<IndyCarEvent>
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
                Color.rgb(
                    143,
                    166,
                    186
                )
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
        evento: IndyCarEvent
    ) {

        val bandeira =
            obterBandeira(evento.pais)

        val datas =
            if (evento.inicio == evento.fim) {
                Idioma.data(this, evento.inicio)
            } else {
                "${Idioma.data(this, evento.inicio)} – ${Idioma.data(this, evento.fim)}"
            }

        val corridas = ""

        val localTexto =
            Idioma.lugar(this, if (evento.local.isNotBlank()) evento.local else evento.pais)

        val card =
            TextView(this).apply {

                text =
                    "🏎 ${evento.nome}\n\n" +
                    "$bandeira $localTexto\n\n" +
                    "📅 $datas" +
                    corridas

                textSize = 17f

                setTextColor(
                    Color.WHITE
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
                        R.drawable.rounded_card
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
        evento: IndyCarEvent
    ) {

        val intent =
            Intent(
                this,
                IndyCarActivity::class.java
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