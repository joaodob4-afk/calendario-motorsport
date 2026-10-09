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

class F2CalendarActivity : AppCompatActivity() {

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

        // ==========================
        // VOLTAR - TOPO ESQUERDO
        // ==========================

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
                    "FÓRMULA 2\n\n" +
                    "CALENDÁRIO 2026"

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

        for (
            evento in F2CalendarJson.carregar(this)
        ) {

            adicionarEtapa(
                layout,
                evento
            )
        }

        // ==========================
        // VOLTAR - FUNDO ESQUERDO
        // ==========================

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
        evento: F2Event
    ) {

        val bandeira =
            obterBandeira(
                evento.pais
            )

        val card =
            TextView(this).apply {

                text =
                    "🏁 ETAPA ${evento.etapa}\n\n" +
                    evento.circuito + "\n" +
                    "$bandeira ${evento.pais}\n\n" +
                    "📅 ${evento.inicio} — ${evento.fim}"

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

    private fun obterBandeira(
        pais: String
    ): String {

        return when (
            pais
                .lowercase()
                .trim()
        ) {

            "austrália",
            "australia" ->
                "🇦🇺"

            "bahrein",
            "bahrain" ->
                "🇧🇭"

            "arábia saudita",
            "arabia saudita",
            "saudi arabia" ->
                "🇸🇦"

            "japão",
            "japan" ->
                "🇯🇵"

            "china" ->
                "🇨🇳"

            "emirados árabes unidos",
            "emirados arabes unidos",
            "uae",
            "united arab emirates" ->
                "🇦🇪"

            "itália",
            "italia",
            "italy" ->
                "🇮🇹"

            "mônaco",
            "monaco" ->
                "🇲🇨"

            "espanha",
            "spain" ->
                "🇪🇸"

            "canadá",
            "canada" ->
                "🇨🇦"

            "áustria",
            "austria" ->
                "🇦🇹"

            "reino unido",
            "united kingdom",
            "uk" ->
                "🇬🇧"

            "bélgica",
            "belgica",
            "belgium" ->
                "🇧🇪"

            "hungria",
            "hungría",
            "hungary" ->
                "🇭🇺"

            "países baixos",
            "paises baixos",
            "netherlands" ->
                "🇳🇱"

            "azerbaijão",
            "azerbaijao",
            "azerbaijan" ->
                "🇦🇿"

            "singapura",
            "singapore" ->
                "🇸🇬"

            "méxico",
            "mexico" ->
                "🇲🇽"

            "brasil",
            "brazil" ->
                "🇧🇷"

            "catar",
            "qatar" ->
                "🇶🇦"

            "estados unidos",
            "usa",
            "united states" ->
                "🇺🇸"

            else ->
                "🌐"
        }
    }

    private fun abrirEtapa(
        evento: F2Event
    ) {

        val intent =
            Intent(
                this,
                F2Activity::class.java
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