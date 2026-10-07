package com.motorsport.calendario

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private var categoriaAtual = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mostrarMenu()
    }

    private fun animarEntrada() {
        val tela =
            findViewById<android.view.View>(
                android.R.id.content
            )

        tela.translationX = 80f
        tela.alpha = 0f

        tela.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(280)
            .start()
    }

    private fun mostrarMenu() {
        setContentView(R.layout.activity_main)

        animarEntrada()

        findViewById<android.view.View>(R.id.btnF1).setOnClickListener {
            abrirCategoria("F1")
        }

        findViewById<android.view.View>(R.id.btnF2).setOnClickListener {
            abrirCategoria("F2")
        }

        findViewById<TextView>(R.id.btnF3).setOnClickListener {
            abrirCategoria("F3")
        }

        findViewById<TextView>(R.id.btnIndyCar).setOnClickListener {
            abrirCategoria("IndyCar")
        }

        findViewById<TextView>(R.id.btnFormulaE).setOnClickListener {
            abrirCategoria("Formula E")
        }
    }

    private fun abrirCategoria(categoria: String) {

        categoriaAtual = categoria

        setContentView(R.layout.activity_category)

        animarEntrada()

        val titulo =
            findViewById<TextView>(
                R.id.categoryTitle
            )

        val logo =
            findViewById<ImageView>(
                R.id.categoryLogo
            )

        when (categoria) {

            "F1" -> {
                logo.setImageResource(
                    R.drawable.logo_f1
                )
            }

            "F2" -> {
                logo.setImageResource(
                    R.drawable.logo_f2
                )
            }

            else -> {
                logo.setImageDrawable(null)
            }
        }

        val evento =
            findViewById<TextView>(
                R.id.nextEvent
            )

        val calendario =
            findViewById<TextView>(
                R.id.fullCalendarButton
            )

        val voltar =
            findViewById<TextView>(
                R.id.backButton
            )

        titulo.text =
            when (categoria) {

                "F1" -> "FÓRMULA 1"

                "F2" -> "FÓRMULA 2"

                "F3" -> "FÓRMULA 3"

                "IndyCar" -> "INDYCAR"

                else -> "FÓRMULA E"
            }

        voltar.setOnClickListener {

            mostrarMenu()

            overridePendingTransition(
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }

        if (categoria == "F1") {

            evento.setOnClickListener {
                abrirEventoF1()
            }
        }

        if (categoria == "F2") {

            evento.setOnClickListener {
                abrirEventoF2()
            }
        }

        calendario.setOnClickListener {

            when (categoriaAtual) {

                "F1" -> {

                    val intent =
                        Intent(
                            this,
                            F1CalendarActivity::class.java
                        )

                    startActivity(intent)

                    overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }

                "F2" -> {

                    val intent =
                        Intent(
                            this,
                            F2CalendarActivity::class.java
                        )

                    startActivity(intent)

                    overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }

                else -> {

                    evento.text =
                        "Programação completa"
                }
            }
        }

        when (categoria) {

            "F1" -> carregarF1(evento)

            "F2" -> carregarF2(evento)

            else -> {

                evento.text =
                    "Calendário automático\n\n" +
                    "Esta categoria será adicionada em breve.\n\n" +
                    "• Treinos\n" +
                    "• Classificação\n" +
                    "• Corrida\n" +
                    "• Horários de Brasília"
            }
        }
    }

    private fun carregarF1(evento: TextView) {

        Thread {

            try {

                val races =
                    buscarCorridasF1()

                val agora =
                    java.time.Instant.now()

                var proximaCorrida: JSONObject? =
                    null

                var proximaData: OffsetDateTime? =
                    null

                for (i in 0 until races.length()) {

                    val race =
                        races.getJSONObject(i)

                    val data =
                        race.getString("date")

                    val hora =
                        race.optString(
                            "time",
                            "00:00:00Z"
                        )

                    val horario =
                        OffsetDateTime.parse(
                            "${data}T${hora}"
                        )

                    if (
                        horario.toInstant()
                            .isAfter(agora)
                    ) {

                        proximaCorrida = race
                        proximaData = horario

                        break
                    }
                }

                if (proximaCorrida == null) {

                    runOnUiThread {

                        evento.text =
                            "Nenhuma corrida futura encontrada."
                    }

                    return@Thread
                }

                val brasilia =
                    ZoneId.of(
                        "America/Sao_Paulo"
                    )

                val formato =
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy - HH:mm"
                    )

                val horarioBrasilia =
                    proximaData!!.atZoneSameInstant(
                        brasilia
                    )

                val nome =
                    proximaCorrida!!
                        .getString("raceName")

                val dataCorrida =
                    horarioBrasilia.format(
                        formato
                    )

                val qualificacao =
                    proximaCorrida!!
                        .optJSONObject(
                            "Qualifying"
                        )

                var textoQualificacao =
                    ""

                if (qualificacao != null) {

                    val dataQualificacao =
                        qualificacao.getString(
                            "date"
                        )

                    val horaQualificacao =
                        qualificacao.getString(
                            "time"
                        )

                    val horarioQualificacao =
                        OffsetDateTime.parse(
                            "${dataQualificacao}T${horaQualificacao}"
                        )

                    val horarioBrasiliaQualificacao =
                        horarioQualificacao
                            .atZoneSameInstant(
                                brasilia
                            )

                    textoQualificacao =
                        "🔵 Classificação\n" +
                        "📅 ${
                            horarioBrasiliaQualificacao.format(
                                formato
                            )
                        }"
                }

                runOnUiThread {

                    evento.text =
                        "🏁 $nome\n\n" +
                        "🔴 Corrida\n" +
                        "📅 $dataCorrida\n\n" +
                        textoQualificacao +
                        "\n\n" +
                        "👆 TOQUE PARA VER OS DETALHES"
                }

            } catch (e: Exception) {

                runOnUiThread {

                    evento.text =
                        "Erro ao carregar calendário.\n\n" +
                        "Erro: " +
                        e.javaClass.simpleName
                }
            }

        }.start()
    }

    private fun abrirEventoF1() {

        val intent =
            Intent(
                this,
                F1Activity::class.java
            )

        startActivity(intent)

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun carregarF2(evento: TextView) {

        val hoje =
            java.time.LocalDate.now()

        var proximo: F2Event? =
            null

        for (item in F2Calendar.eventos) {

            val inicio =
                java.time.LocalDate.parse(
                    item.inicio,
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy"
                    )
                )

            if (!inicio.isBefore(hoje)) {

                proximo = item

                break
            }
        }

        if (proximo == null) {

            evento.text =
                "Temporada 2026 encerrada"

            return
        }

        evento.text =
            "🏁 Etapa ${proximo.etapa}\n\n" +
            "${proximo.circuito}\n" +
            "🇺🇳 ${proximo.pais}\n\n" +
            "📅 ${proximo.inicio} até ${proximo.fim}\n\n" +
            "👆 TOQUE PARA VER OS DETALHES"
    }

    private fun abrirEventoF2() {

        val hoje =
            java.time.LocalDate.now()

        var proximo: F2Event? =
            null

        for (item in F2Calendar.eventos) {

            val inicio =
                java.time.LocalDate.parse(
                    item.inicio,
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy"
                    )
                )

            if (!inicio.isBefore(hoje)) {

                proximo = item

                break
            }
        }

        if (proximo == null) return

        val intent =
            Intent(
                this,
                F2Activity::class.java
            )

        intent.putExtra(
            "ETAPA",
            proximo.etapa
        )

        startActivity(intent)

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun buscarCorridasF1(): org.json.JSONArray {

        val url =
            URL(
                "https://api.jolpi.ca/ergast/f1/current/races/"
            )

        val connection =
            url.openConnection()
                    as HttpURLConnection

        connection.requestMethod =
            "GET"

        connection.connectTimeout =
            15000

        connection.readTimeout =
            15000

        connection.setRequestProperty(
            "User-Agent",
            "CalendarioMotorsport/1.0"
        )

        val resposta =
            connection.inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }

        connection.disconnect()

        return JSONObject(resposta)
            .getJSONObject("MRData")
            .getJSONObject("RaceTable")
            .getJSONArray("Races")
    }
}