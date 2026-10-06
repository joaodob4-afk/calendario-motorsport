package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class F2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val etapaNumero = intent.getIntExtra("ETAPA", 1)

        val evento = F2Calendar.eventos.find {
            it.etapa == etapaNumero
        }

        if (evento == null) {
            finish()
            return
        }

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.BLACK)
        }

        val titulo = TextView(this).apply {
            text = "🏎️ FÓRMULA 2\n\nETAPA ${evento.etapa}"
            textSize = 26f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        conteudo.addView(titulo)

        val circuito = TextView(this).apply {
            text = "${evento.circuito}\n🇺🇳 ${evento.pais}"
            textSize = 21f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 10)
        }

        conteudo.addView(circuito)

        val periodo = TextView(this).apply {
            text = "📅 ${evento.inicio} → ${evento.fim}"
            textSize = 16f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        conteudo.addView(periodo)

        for (sessao in evento.sessoes) {

            val emoji = when {
                sessao.nome.contains("Treino") -> "🟢"
                sessao.nome.contains("Classificação") -> "🔵"
                sessao.nome.contains("Sprint") -> "🟡"
                sessao.nome.contains("Feature") -> "🔴"
                else -> "⚪"
            }

            val sessaoView = TextView(this).apply {
                text = "$emoji ${sessao.nome}\n\n" +
                        "📅 ${sessao.data}\n" +
                        "🕐 ${sessao.horario}"

                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setPadding(20, 20, 20, 20)
                setBackgroundColor(Color.DKGRAY)
            }

            val parametros = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            parametros.setMargins(0, 8, 0, 8)

            conteudo.addView(sessaoView, parametros)
        }

        val scrollView = ScrollView(this).apply {
            addView(conteudo)
        }

        setContentView(scrollView)
    }
}
