package com.motorsport.calendario

import android.content.Intent
import android.os.Bundle
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

    private fun mostrarMenu() {
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.btnF1).setOnClickListener {
            abrirCategoria("F1")
        }

        findViewById<TextView>(R.id.btnF2).setOnClickListener {
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

        val titulo =
            findViewById<TextView>(R.id.categoryTitle)

        val evento =
            findViewById<TextView>(R.id.nextEvent)

        val sessao =
            findViewById<TextView>(R.id.nextSession)

        val calendario =
            findViewById<TextView>(R.id.fullCalendarButton)

        val voltar =
            findViewById<TextView>(R.id.backButton)

        titulo.text = when (categoria) {
            "F1" -> "🏎️ FÓRMULA 1"
            "F2" -> "🏎️ FÓRMULA 2"
            "F3" -> "🏎️ FÓRMULA 3"
            "IndyCar" -> "🏁 INDYCAR"
            else -> "⚡ FÓRMULA E"
        }

        voltar.setOnClickListener {
            mostrarMenu()
        }

        calendario.setOnClickListener {

            when (categoriaAtual) {

                "F1" -> carregarCalendarioF1(
                    evento,
                    sessao
                )

                "F2" -> abrirDetalhesF2()

                else -> {
                    evento.text =
                        "Programação completa"

                    sessao.text =
                        "Esta categoria será adicionada em breve."
                }
            }
        }

        when (categoria) {

            "F1" -> carregarF1(
                evento,
                sessao
            )

            "F2" -> carregarF2(
                evento,
                sessao
            )

            else -> {

                evento.text =
                    "Calendário automático"

                sessao.text =
                    "Esta categoria será adicionada em breve.\n\n" +
                    "• Treinos\n" +
                    "• Classificação\n" +
                    "• Corrida\n" +
                    "• Horários de Brasília"
            }
        }
    }

    private fun carregarF1(
        evento: TextView,
        sessao: TextView
    ) {

        Thread {

            try {

                val races =
                    buscarCorridasF1()

                val agora =
                    java.time.Instant.now()

                val brasilia =
                    ZoneId.of("America/Sao_Paulo")

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

                        proximaCorrida =
                            race

                        proximaData =
                            horario

                        break
                    }
                }

                if (proximaCorrida == null) {

                    runOnUiThread {

                        evento.text =
                            "Nenhuma corrida futura encontrada."

                        sessao.text = ""
                    }

                    return@Thread
                }

                val formato =
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy - HH:mm"
                    )

                val horarioBrasilia =
                    proximaData!!
                        .atZoneSameInstant(
                            brasilia
                        )

                val nome =
                    proximaCorrida!!
                        .getString(
                            "raceName"
                        )

                val dataCorrida =
                    horarioBrasilia.format(
                        formato
                    )

                runOnUiThread {

                    evento.text =
                        "🏁 $nome\n\n" +
                        "🔴 Corrida\n" +
                        "📅 $dataCorrida"

                    sessao.text = ""
                }

            } catch (e: Exception) {

                runOnUiThread {

                    evento.text =
                        "Erro ao carregar calendário."

                    sessao.text =
                        "Erro: " +
                        e.javaClass.simpleName
                }
            }

        }.start()
    }

    private fun carregarF2(
        evento: TextView,
        sessao: TextView
    ) {

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

            sessao.text = ""

            return
        }

        evento.text =
            "🏁 Etapa ${proximo.etapa}\n\n" +
            "${proximo.circuito}\n" +
            "🇺🇳 ${proximo.pais}\n\n" +
            "📅 ${proximo.inicio} até ${proximo.fim}"

        // Quadrante do meio vazio
        sessao.text = ""
    }

    private fun abrirDetalhesF2() {

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
            return
        }

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
    }

    private fun buscarCorridasF1():
        org.json.JSONArray {

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

    private fun carregarCalendarioF1(
        evento: TextView,
        sessao: TextView
    ) {

        evento.text =
            "🏎️ FÓRMULA 1"

        sessao.text =
            "Carregando programação..."

        Thread {

            try {

                val races =
                    buscarCorridasF1()

                val brasilia =
                    ZoneId.of(
                        "America/Sao_Paulo"
                    )

                val formato =
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy - HH:mm"
                    )

                val texto =
                    StringBuilder()

                texto.append(
                    "📅 PROGRAMAÇÃO COMPLETA\n\n"
                )

                texto.append(
                    "Todos os horários em Brasília\n\n"
                )

                for (i in 0 until races.length()) {

                    val race =
                        races.getJSONObject(i)

                    texto.append(
                        "━━━━━━━━━━━━━━━━━━━━\n\n"
                    )

                    texto.append("🏁 ")

                    texto.append(
                        race.getString(
                            "raceName"
                        )
                    )

                    texto.append("\n\n")

                    adicionarSessao(
                        texto,
                        race,
                        "FirstPractice",
                        "🟢 TREINO LIVRE 1",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "SecondPractice",
                        "🟢 TREINO LIVRE 2",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "ThirdPractice",
                        "🟢 TREINO LIVRE 3",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "SprintQualifying",
                        "🟡 CLASSIFICAÇÃO SPRINT",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "Sprint",
                        "🟡 SPRINT",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "Qualifying",
                        "🔵 CLASSIFICAÇÃO",
                        brasilia,
                        formato
                    )

                    adicionarSessao(
                        texto,
                        race,
                        "date",
                        "🔴 CORRIDA",
                        brasilia,
                        formato
                    )

                    texto.append("\n")
                }

                runOnUiThread {

                    evento.text =
                        texto.toString()

                    sessao.text = ""
                }

            } catch (e: Exception) {

                runOnUiThread {

                    evento.text =
                        "Erro ao carregar programação."

                    sessao.text =
                        "Erro: " +
                        e.javaClass.simpleName
                }
            }

        }.start()
    }

    private fun adicionarSessao(
        texto: StringBuilder,
        race: JSONObject,
        campo: String,
        nome: String,
        brasilia: ZoneId,
        formato: DateTimeFormatter
    ) {

        try {

            val data: String

            val hora: String

            if (campo == "date") {

                data =
                    race.getString(
                        "date"
                    )

                hora =
                    race.optString(
                        "time",
                        "00:00:00Z"
                    )

            } else {

                val sessao =
                    race.getJSONObject(
                        campo
                    )

                data =
                    sessao.getString(
                        "date"
                    )

                hora =
                    sessao.getString(
                        "time"
                    )
            }

            val horarioUtc =
                OffsetDateTime.parse(
                    "${data}T${hora}"
                )

            val horarioBrasilia =
                horarioUtc.atZoneSameInstant(
                    brasilia
                )

            texto.append(nome)

            texto.append("\n")

            texto.append("📅 ")

            texto.append(
                horarioBrasilia.format(
                    formato
                )
            )

            texto.append("\n\n")

        } catch (_: Exception) {
        }
    }
}
