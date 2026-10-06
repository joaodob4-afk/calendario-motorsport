package com.motorsport.calendario

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class F2CalendarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
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
                    Color.rgb(11, 11, 15)
                )
            }

        val titulo =
            TextView(this).apply {

                text =
                    "FÓRMULA 2\n\n" +
                    "CALENDÁRIO 2026"

                textSize = 27f

                setTextColor(Color.WHITE)

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                gravity = Gravity.CENTER

                letterSpacing = 0.04f

                setPadding(
                    0,
                    10,
                    0,
                    28
                )
            }

        layout.addView(titulo)

        for (
            evento in F2Calendar.eventos
        ) {

            val botao =
                TextView(this).apply {

                    text =
                        "🏁  ETAPA ${evento.etapa}\n\n" +
                        "${evento.circuito}\n" +
                        "🇺🇳 ${evento.pais}\n\n" +
                        "📅 ${evento.inicio} → ${evento.fim}"

                    textSize = 17f

                    setTextColor(Color.WHITE)

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        20,
                        20,
                        20,
                        20
                    )

                    setBackgroundColor(
                        Color.rgb(36, 36, 43)
                    )

                    isClickable = true
                    isFocusable = true

                    setOnClickListener {

                        val intent =
                            Intent(
                                this@F2CalendarActivity,
                                F2Activity::class.java
                            )

                        intent.putExtra(
                            "ETAPA",
                            evento.etapa
                        )

                        startActivity(intent)

                        overridePendingTransition(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
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
                botao,
                parametros
            )
        }

        val voltar =
            Button(this).apply {

                text = "VOLTAR"
                textSize = 15f

                setOnClickListener {
                    finish()
                }
            }

        val parametrosVoltar =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosVoltar.setMargins(
            0,
            24,
            0,
            20
        )

        layout.addView(
            voltar,
            parametrosVoltar
        )

        val scrollView =
            ScrollView(this).apply {
                addView(layout)
            }

        setContentView(scrollView)
    }
}