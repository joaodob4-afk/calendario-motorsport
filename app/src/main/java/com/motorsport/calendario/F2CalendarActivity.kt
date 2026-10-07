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

        // Entrada: direita → esquerda
        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )

        // Botão/gesto Voltar do Android
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    finish()

                    // Saída: esquerda → direita
                    overridePendingTransition(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
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

        val voltar =
            TextView(this).apply {

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
                    Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    20
                )

                setOnClickListener {

                    finish()

                    overridePendingTransition(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                }
            }

        layout.addView(
            voltar
        )

        for (
            evento in F2Calendar.eventos
        ) {

            adicionarEtapa(
                layout,
                evento
            )
        }

        val scroll =
            ScrollView(this).apply {
                addView(layout)
            }

        setContentView(
            scroll
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

        // Entrada nos detalhes: direita → esquerda
        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }
}