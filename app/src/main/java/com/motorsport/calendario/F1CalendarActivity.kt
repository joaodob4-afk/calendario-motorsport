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
        // As telas de cada etapa ainda só têm o visual escuro: ficam escuras mesmo com o tema claro.
        delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
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

                setPadding(emPx(20f), emPx(20f), emPx(20f), emPx(20f))

                setBackgroundColor(
                    getColor(R.color.fundo_tela)
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

        parametrosTopo.setMargins(0, 0, 0, emPx(10f))

        layout.addView(
            voltarTopo,
            parametrosTopo
        )

        val titulo =
            TextView(this).apply {

                text =
                    getString(R.string.f1_calendario_titulo, java.time.LocalDate.now().year)

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

                setPadding(0, emPx(10f), 0, emPx(28f))
            }

        layout.addView(
            titulo
        )

        val carregando =
            TextView(this).apply {

                text =
                    getString(R.string.f1_carregando_calendario)

                textSize = 17f

                setTextColor(
                    Color.LTGRAY
                )

                gravity =
                    Gravity.CENTER

                setPadding(0, emPx(20f), 0, emPx(20f))
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
                getString(R.string.voltar_botao)

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

            setPadding(0, emPx(8f), 0, emPx(8f))

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
                    if (isFinishing || isDestroyed) return@runOnUiThread

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

                    parametrosFundo.setMargins(0, emPx(24f), 0, emPx(20f))

                    layout.addView(
                        voltarFundo,
                        parametrosFundo
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    carregando.text =
                        getString(R.string.f1_erro_calendario, e.javaClass.simpleName)

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

                    parametrosFundo.setMargins(0, emPx(24f), 0, emPx(20f))

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
                getString(R.string.grande_premio_nome)
            )

        val circuito =
            race
                .optJSONObject("Circuit")
                ?.optString(
                    "circuitName",
                    getString(R.string.circuito_nao_informado)
                )
                ?: getString(R.string.circuito_nao_informado)

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

                setPadding(emPx(20f), emPx(20f), emPx(20f), emPx(20f))

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

        parametros.setMargins(0, emPx(6f), 0, emPx(6f))

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
                ZoneId.systemDefault()

            val horarioBrasil =
                horarioUtc.atZoneSameInstant(
                    brasilia
                )

            val formato =
                DateTimeFormatter.ofPattern(getString(R.string.formato_data_hora), Idioma.locale(this))

            horarioBrasil.format(
                formato
            )

        } catch (e: Exception) {
registrarDescarte("F1CalendarActivity.converterParaBrasilia", e)

            if (data.isNotEmpty()) {

                try {

                    val dataFormatada =
                        java.time.LocalDate
                            .parse(data)
                            .format(
                                DateTimeFormatter.ofPattern(getString(R.string.formato_data), Idioma.locale(this))
                            )

                    dataFormatada

                } catch (e: Exception) {
registrarDescarte("F1CalendarActivity.converterParaBrasilia", e)

                    data
                }

            } else {

                getString(R.string.data_nao_informada)
            }
        }
    }

    private fun obterBandeira(pais: String): String = Bandeiras.emoji(pais, "🏳️")


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

                if (corridasCache.length() > 0) {
                    return corridasCache
                }
            } catch (e: Exception) {
                registrarFalha("F1CalendarActivity.buscarCorridasF1", e)
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

            if (corridas.length() == 0) {
                throw java.io.IOException("Calendário recebido não contém etapas")
            }

            // A atualização em segundo plano da F1CalendarApi busca a programação
            // detalhada; não substituímos esse cache com a resposta básica desta tela.

            return corridas
        } catch (e: Exception) {
            registrarFalha("F1CalendarActivity.buscarCorridasF1", e)
            // Sem cache nem rede, não inventa um calendário desatualizado.
            return JSONArray()
        } finally {
            connection?.disconnect()
        }
    }

}