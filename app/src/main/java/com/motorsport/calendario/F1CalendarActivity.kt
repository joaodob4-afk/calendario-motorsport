package com.motorsport.calendario

import android.content.Intent
import java.io.File
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class F1CalendarActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    voltarParaTelaAnterior()
                }
            }
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
                    Color.rgb(
                        7,
                        26,
                        45
                    )
                )
            }

        // ==========================
        // VOLTAR - TOPO ESQUERDO
        // ==========================

        val voltarTopo =
            criarBotaoVoltar()

        val parametrosTopo =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosTopo.gravity =
            Gravity.LEFT

        parametrosTopo.setMargins(
            0,
            0,
            0,
            10
        )

        layout.addView(
            voltarTopo,
            parametrosTopo
        )

        val titulo =
            TextView(this).apply {

                text =
                    "FÓRMULA 1\n\n" +
                    "CALENDÁRIO ${java.time.LocalDate.now().year}"

                textSize = 27f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                gravity =
                    Gravity.CENTER

                letterSpacing = 0.04f

                setPadding(
                    0,
                    10,
                    0,
                    28
                )
            }

        layout.addView(
            titulo
        )

        val carregando =
            TextView(this).apply {

                text =
                    "Carregando calendário..."

                textSize = 17f

                setTextColor(
                    Color.LTGRAY
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    20,
                    0,
                    20
                )
            }

        layout.addView(
            carregando
        )

        val scroll =
            ScrollView(this).apply {
                addView(layout)
            }

        setContentView(
            scroll
        )
        aplicarInsetsDasBarras()

        // Atualiza o cache em segundo plano; a tela pode abrir usando os dados salvos.
        F1CalendarApi.atualizar(this)

        carregarCalendario(
            layout,
            carregando
        )
    }

    private fun criarBotaoVoltar(): TextView {

        return TextView(this).apply {

            text =
                "‹  VOLTAR"

            textSize = 15f

            setTextColor(
                Color.rgb(
                    143,
                    166,
                    186
                )
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            gravity =
                Gravity.LEFT

            setPadding(
                0,
                8,
                0,
                8
            )

            isClickable = true
            isFocusable = true

            setOnClickListener {
                voltarParaTelaAnterior()
            }
        }
    }

    private fun voltarParaTelaAnterior() {

        finish()

        overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }

    private fun carregarCalendario(
        layout: LinearLayout,
        carregando: TextView
    ) {

        Thread {

            try {

                val corridas =
                    buscarCorridasF1()

                runOnUiThread {

                    layout.removeView(
                        carregando
                    )

                    for (
                        i in 0 until corridas.length()
                    ) {

                        val race =
                            corridas.getJSONObject(
                                i
                            )

                        adicionarEtapa(
                            layout,
                            race
                        )
                    }

                    // ==========================
                    // VOLTAR - FUNDO ESQUERDO
                    // ==========================

                    val voltarFundo =
                        criarBotaoVoltar()

                    val parametrosFundo =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                    parametrosFundo.gravity =
                        Gravity.LEFT

                    parametrosFundo.setMargins(
                        0,
                        24,
                        0,
                        20
                    )

                    layout.addView(
                        voltarFundo,
                        parametrosFundo
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {

                    carregando.text =
                        "Erro ao carregar calendário.\n\n" +
                        "Erro: " +
                        e.javaClass.simpleName

                    // Mesmo em caso de erro,
                    // mantém o botão no fundo.

                    val voltarFundo =
                        criarBotaoVoltar()

                    val parametrosFundo =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                    parametrosFundo.gravity =
                        Gravity.LEFT

                    parametrosFundo.setMargins(
                        0,
                        24,
                        0,
                        20
                    )

                    layout.addView(
                        voltarFundo,
                        parametrosFundo
                    )
                }
            }

        }.start()
    }

    private fun adicionarEtapa(
        layout: LinearLayout,
        race: JSONObject
    ) {

        val nome =
            race.optString(
                "raceName",
                "Grande Prêmio"
            )

        val circuito =
            race
                .optJSONObject("Circuit")
                ?.optString(
                    "circuitName",
                    "Circuito não informado"
                )
                ?: "Circuito não informado"

        val localizacao =
            race
                .optJSONObject("Circuit")
                ?.optJSONObject("Location")

        val cidade =
            localizacao
                ?.optString(
                    "local",
                    ""
                )
                ?: ""

        val pais =
            localizacao
                ?.optString(
                    "country",
                    ""
                )
                ?: ""

        val bandeira =
            obterBandeira(pais)

        val data =
            race.optString(
                "date",
                ""
            )

        val hora =
            race.optString(
                "time",
                ""
            )

        val dataHoraBrasil =
            converterParaBrasilia(
                data,
                hora
            )

        val card =
            TextView(this).apply {

                text =
                    "🏁 $nome\n\n" +
                    "📍 $circuito\n" +
                    "$bandeira $cidade • $pais\n\n" +
                    "📅 $dataHoraBrasil"

                textSize = 17f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    20,
                    20,
                    20,
                    20
                )

                background =
                    getDrawable(
                        R.drawable.rounded_card
                    )

                isClickable = true
                isFocusable = true

                setOnClickListener {

                    abrirEtapa(
                        race
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
            card,
            parametros
        )
    }

    private fun converterParaBrasilia(
        data: String,
        hora: String
    ): String {

        return try {

            val horarioUtc =
                OffsetDateTime.parse(
                    "${data}T$hora"
                )

            val brasilia =
                ZoneId.of(
                    "America/Sao_Paulo"
                )

            val horarioBrasil =
                horarioUtc.atZoneSameInstant(
                    brasilia
                )

            val formato =
                DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy - HH:mm"
                )

            horarioBrasil.format(
                formato
            )

        } catch (_: Exception) {

            if (data.isNotEmpty()) {

                try {

                    val dataFormatada =
                        java.time.LocalDate
                            .parse(data)
                            .format(
                                DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy"
                                )
                            )

                    dataFormatada

                } catch (_: Exception) {

                    data
                }

            } else {

                "Data não informada"
            }
        }
    }

    private fun obterBandeira(
        pais: String
    ): String {

        return when (
            pais.lowercase()
        ) {

            "australia" -> "🇦🇺"

            "japan" -> "🇯🇵"

            "bahrain" -> "🇧🇭"

            "saudi arabia" -> "🇸🇦"

            "usa" -> "🇺🇸"

            "united states" -> "🇺🇸"

            "italy" -> "🇮🇹"

            "monaco" -> "🇲🇨"

            "spain" -> "🇪🇸"

            "canada" -> "🇨🇦"

            "austria" -> "🇦🇹"

            "uk" -> "🇬🇧"

            "united kingdom" -> "🇬🇧"

            "belgium" -> "🇧🇪"

            "hungary" -> "🇭🇺"

            "netherlands" -> "🇳🇱"

            "azerbaijan" -> "🇦🇿"

            "singapore" -> "🇸🇬"

            "mexico" -> "🇲🇽"

            "brazil" -> "🇧🇷"

            "qatar" -> "🇶🇦"

            "uae" -> "🇦🇪"

            "united arab emirates" -> "🇦🇪"

            else -> "🏳️"
        }
    }

    private fun abrirEtapa(
        race: JSONObject
    ) {

        val intent =
            Intent(
                this,
                F1Activity::class.java
            )

        intent.putExtra(
            "RACE",
            race.toString()
        )

        startActivity(
            intent
        )

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun buscarCorridasF1(): JSONArray {
        // Primeiro tenta o JSON bruto salvo pela F1CalendarApi.
        // Isso mantém os detalhes das corridas disponíveis sem internet.
        val arquivoCache = File(filesDir, "f1_races_cache.json")
        if (arquivoCache.isFile) {
            try {
                val jsonCache = JSONObject(
                    arquivoCache.readText(Charsets.UTF_8)
                )
                val corridasCache = jsonCache
                    .getJSONObject("MRData")
                    .getJSONObject("RaceTable")
                    .getJSONArray("Races")

                if (corridasCache.length() >= 5) {
                    return corridasCache
                }
            } catch (_: Exception) {
                // Cache ausente/corrompido: tenta a rede abaixo.
            }
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(
                "https://api.jolpi.ca/ergast/f1/current/races/?limit=100"
            )
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty(
                "User-Agent",
                "CalendarioMotorsport/1.0"
            )
            connection.setRequestProperty("Accept", "application/json")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw java.io.IOException(
                    "Resposta HTTP ${connection.responseCode}"
                )
            }

            val resposta = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val corridas = JSONObject(resposta)
                .getJSONObject("MRData")
                .getJSONObject("RaceTable")
                .getJSONArray("Races")

            if (corridas.length() < 5) {
                throw java.io.IOException("Calendário recebido está incompleto")
            }

            // A atualização em segundo plano da F1CalendarApi busca a programação
            // detalhada; não substituímos esse cache com a resposta básica desta tela.

            return corridas
        } catch (erro: Exception) {
            // Se não houver cache nem internet, usa o calendário fixo
            // incluído no app para que a primeira abertura também funcione offline.
            return criarCalendarioReserva()
        } finally {
            connection?.disconnect()
        }
    }

    private fun criarCalendarioReserva(): JSONArray {
        data class EtapaReserva(
            val nome: String,
            val data: String,
            val circuitoId: String,
            val circuito: String,
            val cidade: String,
            val pais: String
        )

        val etapas = listOf(
            EtapaReserva("Australian Grand Prix", "2026-03-08", "albert_park", "Albert Park Grand Prix Circuit", "Melbourne", "Australia"),
            EtapaReserva("Chinese Grand Prix", "2026-03-15", "shanghai", "Shanghai International Circuit", "Shanghai", "China"),
            EtapaReserva("Japanese Grand Prix", "2026-03-29", "suzuka", "Suzuka International Racing Course", "Suzuka", "Japan"),
            EtapaReserva("Miami Grand Prix", "2026-05-03", "miami", "Miami International Autodrome", "Miami", "USA"),
            EtapaReserva("Canadian Grand Prix", "2026-05-24", "villeneuve", "Circuit Gilles-Villeneuve", "Montreal", "Canada"),
            EtapaReserva("Monaco Grand Prix", "2026-06-07", "monaco", "Circuit de Monaco", "Monte Carlo", "Monaco"),
            EtapaReserva("Spanish Grand Prix", "2026-06-14", "catalunya", "Circuit de Barcelona-Catalunya", "Barcelona", "Spain"),
            EtapaReserva("Austrian Grand Prix", "2026-06-28", "red_bull_ring", "Red Bull Ring", "Spielberg", "Austria"),
            EtapaReserva("British Grand Prix", "2026-07-05", "silverstone", "Silverstone Circuit", "Silverstone", "UK"),
            EtapaReserva("Belgian Grand Prix", "2026-07-19", "spa", "Circuit de Spa-Francorchamps", "Spa", "Belgium"),
            EtapaReserva("Hungarian Grand Prix", "2026-07-26", "hungaroring", "Hungaroring", "Budapest", "Hungary"),
            EtapaReserva("Dutch Grand Prix", "2026-08-23", "zandvoort", "Circuit Zandvoort", "Zandvoort", "Netherlands"),
            EtapaReserva("Italian Grand Prix", "2026-09-06", "monza", "Autodromo Nazionale Monza", "Monza", "Italy"),
            EtapaReserva("Spanish Grand Prix", "2026-09-13", "madring", "Madring", "Madrid", "Spain"),
            EtapaReserva("Azerbaijan Grand Prix", "2026-09-26", "baku", "Baku City Circuit", "Baku", "Azerbaijan"),
            EtapaReserva("Singapore Grand Prix", "2026-10-11", "marina_bay", "Marina Bay Street Circuit", "Marina Bay", "Singapore"),
            EtapaReserva("United States Grand Prix", "2026-10-25", "americas", "Circuit of The Americas", "Austin", "USA"),
            EtapaReserva("Mexico City Grand Prix", "2026-11-01", "rodriguez", "Autodromo Hermanos Rodriguez", "Mexico City", "Mexico"),
            EtapaReserva("Sao Paulo Grand Prix", "2026-11-08", "interlagos", "Autodromo Jose Carlos Pace", "Sao Paulo", "Brazil"),
            EtapaReserva("Las Vegas Grand Prix", "2026-11-21", "vegas", "Las Vegas Strip Circuit", "Las Vegas", "USA"),
            EtapaReserva("Qatar Grand Prix", "2026-11-29", "losail", "Lusail International Circuit", "Lusail", "Qatar"),
            EtapaReserva("Abu Dhabi Grand Prix", "2026-12-06", "yas_marina", "Yas Marina Circuit", "Abu Dhabi", "UAE")
        )

        val corridas = JSONArray()
        for ((indice, etapa) in etapas.withIndex()) {
            val localizacao = JSONObject()
                .put("locality", etapa.cidade)
                .put("country", etapa.pais)
            val circuito = JSONObject()
                .put("circuitId", etapa.circuitoId)
                .put("circuitName", etapa.circuito)
                .put("Location", localizacao)
            corridas.put(
                JSONObject()
                    .put("season", "2026")
                    .put("round", (indice + 1).toString())
                    .put("raceName", etapa.nome)
                    .put("Circuit", circuito)
                    .put("date", etapa.data)
            )
        }
        return corridas
    }
}