package com.motorsport.calendario

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class F3CalendarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            20,
            20,
            20,
            20
        )

        layout.setBackgroundColor(
            android.graphics.Color.rgb(
                7,
                26,
                45
            )
        )

        val voltar = TextView(this)

        voltar.text = "‹  VOLTAR"

        voltar.setTextColor(
            android.graphics.Color.rgb(
                143,
                166,
                186
            )
        )

        voltar.textSize = 15f

        voltar.setPadding(
            0,
            10,
            0,
            25
        )

        voltar.setOnClickListener {
            finish()
        }

        layout.addView(voltar)

        val titulo = TextView(this)

        titulo.text = "CALENDÁRIO FÓRMULA 3"

        titulo.setTextColor(
            android.graphics.Color.WHITE
        )

        titulo.textSize = 26f

        titulo.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        titulo.gravity =
            android.view.Gravity.CENTER

        titulo.setPadding(
            0,
            0,
            0,
            25
        )

        layout.addView(titulo)

        for (evento in F3Calendar.eventos) {

            val card = TextView(this)

            card.text =
                "ETAPA ${evento.etapa}\n\n" +
                "${evento.circuito}\n" +
                "${evento.pais}\n\n" +
                "📅 ${evento.inicio} até ${evento.fim}\n\n" +
                "🏁 Sessões: ${evento.sessoes.size}"

            card.setTextColor(
                android.graphics.Color.WHITE
            )

            card.textSize = 16f

            card.setPadding(
                20,
                20,
                20,
                20
            )

            card.setBackgroundResource(
                R.drawable.rounded_card
            )

            val parametros =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            parametros.setMargins(
                0,
                0,
                0,
                16
            )

            layout.addView(
                card,
                parametros
            )
        }

        setContentView(layout)
    }
}