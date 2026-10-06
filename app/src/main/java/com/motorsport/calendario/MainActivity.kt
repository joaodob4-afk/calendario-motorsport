package com.motorsport.calendario

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            val texto = TextView(this)

            texto.text = """
                Erro ao iniciar o aplicativo.

                ${e.javaClass.simpleName}

                ${e.message}
            """.trimIndent()

            texto.textSize = 18f
            texto.setPadding(30, 30, 30, 30)

            setContentView(texto)
        }
    }
}
