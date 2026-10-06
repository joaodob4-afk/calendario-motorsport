package com.motorsport.calendario

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnF1 = findViewById<TextView>(R.id.btnF1)
        val btnF2 = findViewById<TextView>(R.id.btnF2)
        val btnF3 = findViewById<TextView>(R.id.btnF3)
        val btnIndyCar = findViewById<TextView>(R.id.btnIndyCar)
        val btnFormulaE = findViewById<TextView>(R.id.btnFormulaE)

        btnF1.setOnClickListener {
            abrirCategoria("F1")
        }

        btnF2.setOnClickListener {
            abrirCategoria("F2")
        }

        btnF3.setOnClickListener {
            abrirCategoria("F3")
        }

        btnIndyCar.setOnClickListener {
            abrirCategoria("IndyCar")
        }

        btnFormulaE.setOnClickListener {
            abrirCategoria("Fórmula E")
        }
    }

    private fun abrirCategoria(categoria: String) {

        val tela = TextView(this)

        tela.text = """
            🏁 $categoria

            Calendário da categoria

            Carregando...
        """.trimIndent()

        tela.textSize = 22f
        tela.setTextColor(android.graphics.Color.WHITE)
        tela.setBackgroundColor(android.graphics.Color.rgb(16, 16, 16))
        tela.setPadding(30, 50, 30, 30)

        setContentView(tela)
    }
}
