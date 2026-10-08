package com.motorsport.calendario

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.concurrent.thread

class F1Activity : AppCompatActivity() {

    private lateinit var tituloEvento: TextView
    private lateinit var circuito: TextView
    private lateinit var localizacao: TextView
    private lateinit var sessoes: TextView
    private lateinit var countdown: TextView

    private val handler = Handler(Looper.getMainLooper())

    private var corridaSelecionada: JSONObject? = null
    private var roundSelecionado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_f1)

        tituloEvento = findViewById(R.id.tituloEvento)
        circuito = findViewById(R.id.circuito)
        localizacao = findViewById(R.id.localizacao)
        sessoes = findViewById(R.id.sessoes)
        countdown = findViewById(R.id.countdown)

        roundSelecionado = intent.getStringExtra("ROUND_F1")

        carregarEvento()
    }

    private fun carregarEvento() {

        thread {

            try {

                val url =
                    URL("https://api.jolpi.ca/ergast/f1/current/races/")

                val json =
                    url.openConnection().getInputStream()
                        .bufferedReader()
                        .use { it.readText() }

                val races = JSONObject(json)
                    .getJSONObject("MRData")
                    .getJSONObject("RaceTable")
                    .getJSONArray("Races")

                corridaSelecionada = null

                // Procura exatamente a etapa escolhida
                if (roundSelecionado != null) {

                    for (i in 0 until races.length()) {

                        val race = races.getJSONObject(i)

                        if (race.optString("round") == roundSelecionado) {

                            corridaSelecionada = race
                            break
                        }
                    }
                }

                // Se não encontrar, usa o próximo evento
                if (corridaSelecionada == null) {

                    val hoje = System.currentTimeMillis()

                    for (i in 0 until races.length()) {

                        val race = races.getJSONObject(i)

                        val data = race.optString("date")

                        try {

                            val sdf =
                                SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.US
                                )

                            val dataCorrida =
                                sdf.parse(data)?.time ?: 0L

                            if (dataCorrida >= hoje) {

                                corridaSelecionada = race
                                break
                            }

                        } catch (_: Exception) {
                        }
                    }
                }

                runOnUiThread {

                    if (corridaSelecionada != null) {
                        mostrarEvento(corridaSelecionada!!)
                    } else {
                        tituloEvento.text = "FÓRMULA 1"
                        circuito.text = "Evento não encontrado"
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    tituloEvento.text = "FÓRMULA 1"
                    circuito.text = "Erro ao carregar evento"
                }
            }
        }
    }

    private fun mostrarEvento(race: JSONObject) {

        val nome =
            race.optString("raceName")

        val round =
            race.optString("round")

        val circuitoObj =
            race.optJSONObject("Circuit")

        val nomeCircuito =
            circuitoObj?.optString("circuitName")
                ?: ""

        val cidade =
            circuitoObj
                ?.optJSONObject("Location")
                ?.optString("locality")
                ?: ""

        val pais =
            circuitoObj
                ?.optJSONObject("Location")
                ?.optString("country")
                ?: ""

        tituloEvento.text =
            if (roundSelecionado != null) {
                "FÓRMULA 1\n\nPROGRAMAÇÃO DA ETAPA"
            } else {
                "FÓRMULA 1\n\nPRÓXIMO EVENTO"
            }

        circuito.text =
            "$nome\n\n$nomeCircuito"

        localizacao.text =
            "${obterBandeira(pais)} $cidade, $pais"

        sessoes.text = ""

        adicionarSessao(
            race,
            "FirstPractice",
            "1º Treino"
        )

        adicionarSessao(
            race,
            "SecondPractice",
            "2º Treino"
        )

        adicionarSessao(
            race,
            "ThirdPractice",
            "3º Treino"
        )

        adicionarSessao(
            race,
            "SprintQualifying",
            "Classificação Sprint"
        )

        adicionarSessao(
            race,
            "Sprint",
            "Sprint"
        )

        adicionarSessao(
            race,
            "Qualifying",
            "Classificação"
        )

        adicionarSessao(
            race,
            "Race",
            "Corrida"
        )

        atualizarCountdown()
    }

    private fun adicionarSessao(
        race: JSONObject,
        nomeJson: String,
        nomeSessao: String
    ) {

        val sessao =
            race.optJSONObject(nomeJson)
                ?: return

        val data =
            sessao.optString("date")

        val hora =
            sessao.optString("time")

        if (data.isEmpty()) return

        val dataFormatada =
            formatarDataHora(data, hora)

        if (sessoes.text.isNotEmpty()) {
            sessoes.append("\n\n")
        }

        sessoes.append(
            "$nomeSessao\n$dataFormatada"
        )
    }

    private fun formatarDataHora(
        data: String,
        hora: String
    ): String {

        return try {

            val entrada =
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss'Z'",
                    Locale.US
                )

            entrada.timeZone =
                TimeZone.getTimeZone("UTC")

            val saida =
                SimpleDateFormat(
                    "dd/MM • HH:mm",
                    Locale("pt", "BR")
                )

            val textoHora =
                if (hora.isEmpty()) {
                    "00:00:00Z"
                } else {
                    hora
                }

            val date =
                entrada.parse(
                    "$data $textoHora"
                )

            saida.timeZone =
                TimeZone.getTimeZone(
                    "America/Sao_Paulo"
                )

            saida.format(date!!)
        } catch (_: Exception) {
            "$data • $hora"
        }
    }

    private fun atualizarCountdown() {

        val race =
            corridaSelecionada
                ?: return

        val sessao =
            race.optJSONObject("Race")
                ?: race

        val data =
            sessao.optString("date")

        val hora =
            sessao.optString("time")

        if (data.isEmpty()) return

        try {

            val sdf =
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss'Z'",
                    Locale.US
                )

            sdf.timeZone =
                TimeZone.getTimeZone("UTC")

            val textoHora =
                if (hora.isEmpty()) {
                    "00:00:00Z"
                } else {
                    hora
                }

            val dataEvento =
                sdf.parse(
                    "$data $textoHora"
                )?.time ?: return

            val agora =
                System.currentTimeMillis()

            val diferenca =
                dataEvento - agora

            if (diferenca <= 0) {

                countdown.text =
                    "EVENTO EM ANDAMENTO"

            } else {

                val dias =
                    diferenca / (1000 * 60 * 60 * 24)

                val horas =
                    (diferenca / (1000 * 60 * 60)) % 24

                val minutos =
                    (diferenca / (1000 * 60)) % 60

                countdown.text =
                    "COMEÇA EM ${dias}D ${horas}H ${minutos}MIN"
            }

        } catch (_: Exception) {
        }

        handler.postDelayed(
            {
                atualizarCountdown()
            },
            60_000
        )
    }

    private fun obterBandeira(
        pais: String
    ): String {

        return when (pais.lowercase(Locale.US)) {

            "australia" -> "🇦🇺"
            "japan" -> "🇯🇵"
            "bahrain" -> "🇧🇭"
            "saudi arabia" -> "🇸🇦"
            "usa" -> "🇺🇸"
            "canada" -> "🇨🇦"
            "monaco" -> "🇲🇨"
            "spain" -> "🇪🇸"
            "austria" -> "🇦🇹"
            "united kingdom" -> "🇬🇧"
            "belgium" -> "🇧🇪"
            "hungary" -> "🇭🇺"
            "netherlands" -> "🇳🇱"
            "italy" -> "🇮🇹"
            "azerbaijan" -> "🇦🇿"
            "singapore" -> "🇸🇬"
            "mexico" -> "🇲🇽"
            "brazil" -> "🇧🇷"
            "qatar" -> "🇶🇦"
            "united arab emirates" -> "🇦🇪"
            "china" -> "🇨🇳"

            else -> "🏁"
        }
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }
}