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

                setPadding(
                    20,
                    20,
                    20,
                    20
                )

                setBackgroundColor(
                    Color.rgb(
                        7,
                        26,
                        45
                    )
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

        parametrosTopo.setMargins(
            0,
            0,
            0,
            10
        )

        layout.addView(
            voltarTopo,
            parametrosTopo
        )

        val titulo =
            TextView(this).apply {

                text =
                    "FÓRMULA E\n\n" +
                    "CALENDÁRIO ${descreverTemporada(eventos)}"

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

                setPadding(
                    0,
                    10,
                    0,
                    28
                )
            }

        layout.addView(
            titulo
        )

        if (eventos.isEmpty()) {
            val aviso =
                TextView(this).apply {

                    text =
                        "O calendário da Fórmula E ainda não foi carregado.\n\n" +
                        "Abra o app com internet e tente novamente."

                    textSize = 17f

                    setTextColor(
                        Color.LTGRAY
                    )

                    gravity =
                        Gravity.CENTER

                    setPadding(
                        0,
                        20,
                        0,
                        20
                    )
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

        parametrosFundo.setMargins(
            0,
            24,
            0,
            20
        )

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
                "‹  VOLTAR"

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

            setPadding(
                0,
                8,
                0,
                8
            )

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
                evento.inicio
            } else {
                "${evento.inicio} – ${evento.fim}"
            }

        val corridas =
            if (evento.sessoes.size > 1) {
                "\n\n🏁 Rodada dupla"
            } else {
                ""
            }

        val card =
            TextView(this).apply {

                text =
                    "⚡ ${evento.circuito} E-Prix\n\n" +
                    "$bandeira ${evento.pais}\n\n" +
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

                setPadding(
                    20,
                    20,
                    20,
                    20
                )

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

        parametros.setMargins(
            0,
            6,
            0,
            6
        )

        layout.addView(
            card,
            parametros
        )
    }

    private fun obterBandeira(pais: String): String {
        return when (pais.trim()) {
            "Arábia Saudita" -> "🇸🇦"
            "México" -> "🇲🇽"
            "Estados Unidos" -> "🇺🇸"
            "Brasil" -> "🇧🇷"
            "Alemanha" -> "🇩🇪"
            "Mônaco" -> "🇲🇨"
            "Reino Unido" -> "🇬🇧"
            "Holanda" -> "🇳🇱"
            "Espanha" -> "🇪🇸"
            "China" -> "🇨🇳"
            "Japão" -> "🇯🇵"
            "Itália" -> "🇮🇹"
            else -> "🏳️"
        }
    }

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