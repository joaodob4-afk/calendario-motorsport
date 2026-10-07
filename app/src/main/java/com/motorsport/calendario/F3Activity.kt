package com.motorsport.calendario

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class F3Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_category)

        val backButton = findViewById<TextView>(R.id.backButton)
        val categoryTitle = findViewById<TextView>(R.id.categoryTitle)
        val nextEvent = findViewById<TextView>(R.id.nextEvent)
        val fullCalendarButton = findViewById<TextView>(R.id.fullCalendarButton)

        categoryTitle.text = "FÓRMULA 3"

        val hoje = System.currentTimeMillis()

        val proximoEvento = F3Calendar.eventos.firstOrNull {
            val partes = it.inicio.split("/")
            if (partes.size == 3) {
                val dataEvento = "${partes[2]}-${partes[1]}-${partes[0]}"
                true
            } else {
                false
            }
        }

        if (proximoEvento != null) {
            nextEvent.text =
                "ETAPA ${proximoEvento.etapa}\n" +
                "${proximoEvento.circuito} • ${proximoEvento.pais}\n\n" +
                "${proximoEvento.inicio} — ${proximoEvento.fim}"
        } else {
            nextEvent.text = "Temporada encerrada"
        }

        backButton.setOnClickListener {
            finish()
        }

        fullCalendarButton.setOnClickListener {
            val calendario = F3Calendar.eventos.joinToString("\n\n") { evento ->
                "ETAPA ${evento.etapa} — ${evento.circuito}\n" +
                "${evento.pais}\n" +
                "${evento.inicio} — ${evento.fim}"
            }

            nextEvent.text = calendario
        }
    }
}