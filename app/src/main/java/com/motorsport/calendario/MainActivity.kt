package com.motorsport.calendario

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private var categoriaAtual = ""

    data class Etapa(
        val categoria: String,
        val pais: String,
        val circuito: String,
        val data: String
    )

    private lateinit var listaEtapas: LinearLayout

    private val handlerPesquisa =
        Handler(Looper.getMainLooper())

    private var pesquisaRunnable:
            Runnable? = null

    private val todasEtapas = listOf(

        // F1 2026
        Etapa("F1", "Austrália", "Melbourne", "06–08 MAR"),
        Etapa("F1", "Japão", "Suzuka", "27–29 MAR"),
        Etapa("F1", "Bahrein", "Sakhir", "10–12 ABR"),
        Etapa("F1", "Arábia Saudita", "Jeddah", "17–19 ABR"),
        Etapa("F1", "Estados Unidos", "Miami", "01–03 MAI"),
        Etapa("F1", "Canadá", "Montreal", "22–24 MAI"),
        Etapa("F1", "Mônaco", "Monte Carlo", "05–07 JUN"),
        Etapa("F1", "Espanha", "Barcelona", "12–14 JUN"),
        Etapa("F1", "Áustria", "Spielberg", "26–28 JUN"),
        Etapa("F1", "Reino Unido", "Silverstone", "03–05 JUL"),
        Etapa("F1", "Bélgica", "Spa-Francorchamps", "17–19 JUL"),
        Etapa("F1", "Hungria", "Budapeste", "24–26 JUL"),
        Etapa("F1", "Holanda", "Zandvoort", "21–23 AGO"),
        Etapa("F1", "Itália", "Monza", "04–06 SET"),
        Etapa("F1", "Azerbaijão", "Baku", "18–20 SET"),
        Etapa("F1", "Singapura", "Marina Bay", "09–11 OUT"),
        Etapa("F1", "Estados Unidos", "Austin", "23–25 OUT"),
        Etapa("F1", "México", "Cidade do México", "30 OUT–01 NOV"),
        Etapa("F1", "Brasil", "Interlagos", "06–08 NOV"),
        Etapa("F1", "Estados Unidos", "Las Vegas", "20–22 NOV"),
        Etapa("F1", "Catar", "Lusail", "27–29 NOV"),
        Etapa("F1", "Abu Dhabi", "Yas Marina", "04–06 DEZ"),

        // F2 2026
        Etapa("F2", "Austrália", "Melbourne", "06–08 MAR"),
        Etapa("F2", "Mônaco", "Monte Carlo", "04–07 JUN"),
        Etapa("F2", "Espanha", "Barcelona", "12–14 JUN"),
        Etapa("F2", "Áustria", "Spielberg", "26–28 JUN"),
        Etapa("F2", "Reino Unido", "Silverstone", "03–05 JUL"),
        Etapa("F2", "Bélgica", "Spa-Francorchamps", "17–19 JUL"),
        Etapa("F2", "Hungria", "Budapeste", "24–26 JUL"),
        Etapa("F2", "Itália", "Monza", "04–06 SET"),
        Etapa("F2", "Espanha", "Madrid", "11–13 SET"),

        // F3 2026
        Etapa("F3", "Austrália", "Melbourne", "06–08 MAR"),
        Etapa("F3", "Mônaco", "Monte Carlo", "04–07 JUN"),
        Etapa("F3", "Espanha", "Barcelona", "12–14 JUN"),
        Etapa("F3", "Áustria", "Spielberg", "26–28 JUN"),
        Etapa("F3", "Reino Unido", "Silverstone", "03–05 JUL"),
        Etapa("F3", "Bélgica", "Spa-Francorchamps", "17–19 JUL"),
        Etapa("F3", "Hungria", "Budapeste", "24–26 JUL"),
        Etapa("F3", "Itália", "Monza", "04–06 SET"),
        Etapa("F3", "Espanha", "Madrid", "11–13 SET")
    )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    if (
                        categoriaAtual.isNotEmpty()
                    ) {

                        categoriaAtual = ""

                        mostrarMenu()

                        overridePendingTransition(
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        )

                    } else {

                        finish()
                    }
                }
            }
        )

        mostrarMenu()
    }

    override fun onDestroy() {

        pesquisaRunnable?.let {
            handlerPesquisa.removeCallbacks(it)
        }

        super.onDestroy()
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

        setContentView(
            R.layout.activity_main
        )

        animarEntrada()

        listaEtapas =
            findViewById(
                R.id.listaEtapas
            )

        findViewById<android.view.View>(
            R.id.btnF1
        ).setOnClickListener {

            abrirCategoria("F1")
        }

        findViewById<android.view.View>(
            R.id.btnF2
        ).setOnClickListener {

            abrirCategoria("F2")
        }

        findViewById<android.view.View>(
            R.id.btnF3
        ).setOnClickListener {

            abrirCategoria("F3")
        }

        findViewById<android.view.View>(
            R.id.btnIndyCar
        ).setOnClickListener {

            abrirCategoria("IndyCar")
        }

        findViewById<android.view.View>(
            R.id.btnFormulaE
        ).setOnClickListener {

            abrirCategoria("Formula E")
        }

        configurarBusca()

        mostrarEtapas(
            todasEtapas
        )
    }

    private fun configurarBusca() {

        val busca =
            findViewById<EditText>(
                R.id.searchEtapa
            )

        busca.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    pesquisaRunnable?.let {
                        handlerPesquisa.removeCallbacks(
                            it
                        )
                    }

                    val texto =
                        s?.toString()
                            ?.trim()
                            ?.lowercase()
                            ?: ""

                    pesquisaRunnable =
                        Runnable {

                            if (
                                texto.isEmpty()
                            ) {

                                mostrarEtapas(
                                    todasEtapas
                                )

                                return@Runnable
                            }

                            val filtradas =
                                todasEtapas.filter {

                                    it.categoria
                                        .lowercase()
                                        .contains(
                                            texto
                                        ) ||

                                    it.pais
                                        .lowercase()
                                        .contains(
                                            texto
                                        ) ||

                                    it.circuito
                                        .lowercase()
                                        .contains(
                                            texto
                                        ) ||

                                    it.data
                                        .lowercase()
                                        .contains(
                                            texto
                                        )
                                }

                            mostrarEtapas(
                                filtradas
                            )
                        }

                    handlerPesquisa.postDelayed(
                        pesquisaRunnable!!,
                        250
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    private fun mostrarEtapas(
        etapas: List<Etapa>
    ) {

        listaEtapas.removeAllViews()

        val inflater =
            LayoutInflater.from(this)

        if (
            etapas.isEmpty()
        ) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhuma etapa encontrada."

            vazio.setTextColor(
                android.graphics.Color.WHITE
            )

            vazio.textSize = 15f

            vazio.gravity =
                android.view.Gravity.CENTER

            vazio.setPadding(
                0,
                30,
                0,
                30
            )

            listaEtapas.addView(
                vazio
            )

            return
        }

        for (
            etapa in etapas
        ) {

            val item =
                inflater.inflate(
                    R.layout.item_etapa,
                    listaEtapas,
                    false
                )

            item.findViewById<TextView>(
                R.id.itemCategoria
            ).text =
                etapa.categoria

            item.findViewById<TextView>(
                R.id.itemPais
            ).text =
                "${bandeiraPais(etapa.pais)} ${etapa.pais}"

            item.findViewById<TextView>(
                R.id.itemCircuito
            ).text =
                etapa.circuito

            item.findViewById<TextView>(
                R.id.itemData
            ).text =
                etapa.data

            item.setOnClickListener {

                when (
                    etapa.categoria
                ) {

                    "F1" -> {

                        abrirEventoF1PelaEtapa(
                            etapa
                        )
                    }

                    "F2" -> {

                        abrirEventoF2PelaEtapa(
                            etapa
                        )
                    }

                    "F3" -> {

                        abrirEventoF3PelaEtapa(
                            etapa
                        )
                    }
                }
            }

            listaEtapas.addView(
                item
            )
        }
    }

    private fun bandeiraPais(
        pais: String
    ): String {

        return when (pais) {

            "Austrália" -> "🇦🇺"
            "Japão" -> "🇯🇵"
            "Bahrein" -> "🇧🇭"
            "Arábia Saudita" -> "🇸🇦"
            "Estados Unidos" -> "🇺🇸"
            "Canadá" -> "🇨🇦"
            "Mônaco" -> "🇲🇨"
            "Espanha" -> "🇪🇸"
            "Áustria" -> "🇦🇹"
            "Reino Unido" -> "🇬🇧"
            "Bélgica" -> "🇧🇪"
            "Hungria" -> "🇭🇺"
            "Holanda" -> "🇳🇱"
            "Itália" -> "🇮🇹"
            "Azerbaijão" -> "🇦🇿"
            "Singapura" -> "🇸🇬"
            "México" -> "🇲🇽"
            "Brasil" -> "🇧🇷"
            "Catar" -> "🇶🇦"
            "Abu Dhabi" -> "🇦🇪"

            else -> "🌐"
        }
    }

    private fun abrirCategoria(
        categoria: String
    ) {

        categoriaAtual =
            categoria

        setContentView(
            R.layout.activity_category
        )

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

            "F3" -> {
                logo.setImageResource(
                    R.drawable.logo_f3
                )
            }

            "IndyCar" -> {
                logo.setImageResource(
                    R.drawable.logo_indycar
                )
            }

            "Formula E" -> {
                logo.setImageResource(
                    R.drawable.logo_formulae
                )
            }

            else -> {
                logo.setImageDrawable(
                    null
                )
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

                "F1" ->
                    "FÓRMULA 1"

                "F2" ->
                    "FÓRMULA 2"

                "F3" ->
                    "FÓRMULA 3"

                "IndyCar" ->
                    "INDYCAR"

                else ->
                    "FÓRMULA E"
            }

        voltar.setOnClickListener {

            categoriaAtual = ""

            mostrarMenu()

            overridePendingTransition(
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }

        if (
            categoria == "F1"
        ) {

            evento.setOnClickListener {
                abrirEventoF1()
            }
        }

        if (
            categoria == "F2"
        ) {

            evento.setOnClickListener {
                abrirEventoF2()
            }
        }

        if (
            categoria == "F3"
        ) {

            evento.setOnClickListener {
                abrirEventoF3()
            }
        }

        calendario.setOnClickListener {

            when (
                categoriaAtual
            ) {

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

                "F3" -> {

                    val intent =
                        Intent(
                            this,
                            F3CalendarActivity::class.java
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

        when (
            categoria
        ) {

            "F1" ->
                carregarF1(evento)

            "F2" ->
                carregarF2(evento)

            "F3" ->
                carregarF3(evento)

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

    private fun carregarF1(
        evento: TextView
    ) {

        Thread {

            try {

                val races =
                    buscarCorridasF1()

                val agora =
                    java.time.Instant.now()

                var proximaCorrida:
                        JSONObject? =
                    null

                var proximaData:
                        OffsetDateTime? =
                    null

                for (
                    i in 0 until races.length()
                ) {

                    val race =
                        races.getJSONObject(
                            i
                        )

                    val data =
                        race.getString(
                            "date"
                        )

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

                if (
                    proximaCorrida == null
                ) {

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

                val qualificacao =
                    proximaCorrida!!
                        .optJSONObject(
                            "Qualifying"
                        )

                var textoQualificacao =
                    ""

                if (
                    qualificacao != null
                ) {

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
                            horarioBrasiliaQualificacao
                                .format(formato)
                        }"
                }

                runOnUiThread {

                    evento.text =
                        "🏁 $nome\n\n" +
                        "🔴 Corrida\n" +
                        "📅 $dataCorrida\n\n" +
                        textoQualificacao +
                        "\n\n" +
                        "TOQUE PARA VER OS DETALHES"
                }

            } catch (
                e: Exception
            ) {

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

    private fun abrirEventoF1PelaEtapa(
        etapa: Etapa
    ) {

        val intent =
            Intent(
                this,
                F1Activity::class.java
            )

        intent.putExtra(
            "PAIS",
            etapa.pais
        )

        intent.putExtra(
            "CIRCUITO",
            etapa.circuito
        )

        startActivity(intent)

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun carregarF2(
        evento: TextView
    ) {

        val hoje =
            java.time.LocalDate.now()

        var proximo:
                F2Event? =
            null

        for (
            item in F2Calendar.eventos
        ) {

            val inicio =
                java.time.LocalDate.parse(
                    item.inicio,
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy"
                    )
                )

            if (
                !inicio.isBefore(hoje)
            ) {

                proximo =
                    item

                break
            }
        }

        if (
            proximo == null
        ) {

            evento.text =
                "Temporada 2026 encerrada"

            return
        }

        evento.text =
            "🏁 Etapa ${proximo.etapa}\n\n" +
            "${proximo.circuito}\n" +
            "🇺🇳 ${proximo.pais}\n\n" +
            "📅 ${proximo.inicio} até ${proximo.fim}\n\n" +
            "TOQUE PARA VER OS DETALHES"
    }

    private fun abrirEventoF2() {

        val hoje =
            java.time.LocalDate.now()

        var proximo:
                F2Event? =
            null

        for (
            item in F2Calendar.eventos
        ) {

            val inicio =
                java.time.LocalDate.parse(
                    item.inicio,
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy"
                    )
                )

            if (
                !inicio.isBefore(hoje)
            ) {

                proximo =
                    item

                break
            }
        }

        if (
            proximo == null
        ) return

        abrirEventoF2Item(
            proximo
        )
    }

    private fun abrirEventoF2PelaEtapa(
        etapa: Etapa
    ) {

        var encontrado:
                F2Event? =
            null

        for (
            item in F2Calendar.eventos
        ) {

            if (
                item.circuito.equals(
                    etapa.circuito,
                    ignoreCase = true
                )
            ) {

                encontrado =
                    item

                break
            }
        }

        if (
            encontrado != null
        ) {

            abrirEventoF2Item(
                encontrado
            )

        } else {

            abrirCategoria("F2")
        }
    }

    private fun abrirEventoF2Item(
        evento: F2Event
    ) {

        val intent =
            Intent(
                this,
                F2Activity::class.java
            )

        intent.putExtra(
            "ETAPA",
            evento.etapa
        )

        startActivity(intent)

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun carregarF3(
        evento: TextView
    ) {

        val hoje =
            java.time.LocalDate.now()

        var proximo:
                F3Event? =
            null

        for (
            item in F3Calendar.eventos
        ) {

            val inicio =
                java.time.LocalDate.parse(
                    item.inicio,
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy"
                    )
                )

            if (
                !inicio.isBefore(hoje)
            ) {

                proximo =
                    item

                break
            }
        }

        if (
            proximo == null
        ) {

            evento.text =
                "Temporada 2026 encerrada"

            return
        }

        evento.text =
            "🏁 Etapa ${proximo.etapa}\n\n" +
            "${proximo.circuito}\n" +
            "🇺🇳 ${proximo.pais}\n\n" +
            "📅 ${proximo.inicio} até ${proximo.fim}\n\n" +
            "TOQUE PARA VER OS DETALHES"
    }

    private fun abrirEventoF3() {

        val hoje =
            java.time.LocalDate.now()

        var proximo:
                F3Event? =
            null

        for (
            item in F3Calendar.eventos
        ) {

            val inicio =
                java.time.LocalDate.parse(
                    item.inicio,
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy"
                    )
                )

            if (
                !inicio.isBefore(hoje)
            ) {

                proximo =
                    item

                break
            }
        }

        if (
            proximo == null
        ) return

        abrirEventoF3Item(
            proximo
        )
    }

    private fun abrirEventoF3PelaEtapa(
        etapa: Etapa
    ) {

        var encontrado:
                F3Event? =
            null

        for (
            item in F3Calendar.eventos
        ) {

            if (
                item.circuito.equals(
                    etapa.circuito,
                    ignoreCase = true
                )
            ) {

                encontrado =
                    item

                break
            }
        }

        if (
            encontrado != null
        ) {

            abrirEventoF3Item(
                encontrado
            )

        } else {

            abrirCategoria("F3")
        }
    }

    private fun abrirEventoF3Item(
        evento: F3Event
    ) {

        val intent =
            Intent(
                this,
                F3Activity::class.java
            )

        intent.putExtra(
            "ETAPA",
            evento.etapa
        )

        startActivity(intent)

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
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

        return JSONObject(
            resposta
        )
            .getJSONObject(
                "MRData"
            )
            .getJSONObject(
                "RaceTable"
            )
            .getJSONArray(
                "Races"
            )
    }
}