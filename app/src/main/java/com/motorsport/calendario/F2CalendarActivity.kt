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

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.BLACK)
        }

        val titulo = TextView(this).apply {
            text = "🏎️ FÓRMULA 2\n\nCALENDÁRIO 2026"
            textSize = 26f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        layout.addView(titulo)

        for (evento in F2Calendar.eventos) {

            val botao = Button(this).apply {

                text =
                    "🏁 ETAPA ${evento.etapa}\n" +
                    "${evento.circuito}\n" +
                    "🇺🇳 ${evento.pais}"

                textSize = 17f
                setTextColor(Color.WHITE)

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setPadding(
                    16,
                    16,
                    16,
                    16
                )

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

        val voltar = Button(this).apply {

            text = "VOLTAR"
            textSize = 16f

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
            24
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
