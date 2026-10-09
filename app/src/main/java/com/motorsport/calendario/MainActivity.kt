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
        val data: String,
        val dataInicio: String = "",
        val circuitId: String = ""
    )

    private lateinit var listaEtapas: LinearLayout

    private val handlerPesquisa =
        Handler(Looper.getMainLooper())

    private var pesquisaRunnable: Runnable? = null

    private val handlerProximaEtapa =
        Handler(Looper.getMainLooper())

    private val atualizadorProximaEtapa =
        object : Runnable {

            override fun run() {
                if (categoriaAtual.isEmpty()) {
                    atualizarProximaEtapa()
                }

                handlerProximaEtapa.postDelayed(
                    this,
                    60000
                )
            }
        }

    private val etapasFixas = listOf(

        // F1 2026
        Etapa("F1", "Austrália", "Melbourne", "06–08 MAR", "2026-03-06", "albert_park"),
        Etapa("F1", "China", "Xangai", "13–15 MAR", "2026-03-13", "shanghai"),
        Etapa("F1", "Japão", "Suzuka", "27–29 MAR", "2026-03-27", "suzuka"),
        Etapa("F1", "Estados Unidos", "Miami", "01–03 MAI", "2026-05-01", "miami"),
        Etapa("F1", "Canadá", "Montreal", "22–24 MAI", "2026-05-22", "villeneuve"),
        Etapa("F1", "Mônaco", "Monte Carlo", "05–07 JUN", "2026-06-05", "monaco"),
        Etapa("F1", "Espanha", "Barcelona", "12–14 JUN", "2026-06-12", "catalunya"),
        Etapa("F1", "Áustria", "Spielberg", "26–28 JUN", "2026-06-26", "red_bull_ring"),
        Etapa("F1", "Reino Unido", "Silverstone", "03–05 JUL", "2026-07-03", "silverstone"),
        Etapa("F1", "Bélgica", "Spa-Francorchamps", "17–19 JUL", "2026-07-17", "spa"),
        Etapa("F1", "Hungria", "Budapeste", "24–26 JUL", "2026-07-24", "hungaroring"),
        Etapa("F1", "Holanda", "Zandvoort", "21–23 AGO", "2026-08-21", "zandvoort"),
        Etapa("F1", "Itália", "Monza", "04–06 SET", "2026-09-04", "monza"),
        Etapa("F1", "Espanha", "Madrid", "11–13 SET", "2026-09-11", "madring"),
        Etapa("F1", "Azerbaijão", "Baku", "24–26 SET", "2026-09-24", "baku"),
        Etapa("F1", "Singapura", "Marina Bay", "09–11 OUT", "2026-10-09", "marina_bay"),
        Etapa("F1", "Estados Unidos", "Austin", "23–25 OUT", "2026-10-23", "americas"),
        Etapa("F1", "México", "Cidade do México", "30 OUT–01 NOV", "2026-10-30", "rodriguez"),
        Etapa("F1", "Brasil", "Interlagos", "06–08 NOV", "2026-11-06", "interlagos"),
        Etapa("F1", "Estados Unidos", "Las Vegas", "20–22 NOV", "2026-11-20", "vegas"),
        Etapa("F1", "Catar", "Lusail", "27–29 NOV", "2026-11-27", "losail"),
        Etapa("F1", "Abu Dhabi", "Yas Marina", "04–06 DEZ", "2026-12-04", "yas_marina"),

        // F2 2026
        Etapa("F2", "Austrália", "Melbourne", "06–08 MAR", "2026-03-06"),
        Etapa("F2", "Mônaco", "Monte Carlo", "04–07 JUN", "2026-06-04"),
        Etapa("F2", "Espanha", "Barcelona", "12–14 JUN", "2026-06-12"),
        Etapa("F2", "Áustria", "Spielberg", "26–28 JUN", "2026-06-26"),
        Etapa("F2", "Reino Unido", "Silverstone", "03–05 JUL", "2026-07-03"),
        Etapa("F2", "Bélgica", "Spa-Francorchamps", "17–19 JUL", "2026-07-17"),
        Etapa("F2", "Hungria", "Budapeste", "24–26 JUL", "2026-07-24"),
        Etapa("F2", "Itália", "Monza", "04–06 SET", "2026-09-04"),
        Etapa("F2", "Espanha", "Madrid", "11–13 SET", "2026-09-11"),
        Etapa("F2", "Catar", "Lusail", "27–29 NOV", "2026-11-27"),
        Etapa("F2", "Abu Dhabi", "Yas Marina", "04–06 DEZ", "2026-12-04"),

        // F3 2026
        Etapa("F3", "Austrália", "Melbourne", "06–08 MAR", "2026-03-06"),
        Etapa("F3", "Mônaco", "Monte Carlo", "04–07 JUN", "2026-06-04"),
        Etapa("F3", "Espanha", "Barcelona", "12–14 JUN", "2026-06-12"),
        Etapa("F3", "Áustria", "Spielberg", "26–28 JUN", "2026-06-26"),
        Etapa("F3", "Reino Unido", "Silverstone", "03–05 JUL", "2026-07-03"),
        Etapa("F3", "Bélgica", "Spa-Francorchamps", "17–19 JUL", "2026-07-17"),
        Etapa("F3", "Hungria", "Budapeste", "24–26 JUL", "2026-07-24"),
        Etapa("F3", "Itália", "Monza", "04–06 SET", "2026-09-04"),
        Etapa("F3", "Espanha", "Madrid", "11–13 SET", "2026-09-11")
    )

    // Etapas da F1 baixadas da API; null usa a lista fixa como reserva.
    private var etapasF1Dinamicas: List<Etapa>? = null

    // Etapas da F2 vindas do f2_calendar.json (cache/GitHub/APK).
    private var etapasF2Dinamicas: List<Etapa>? = null

    private val todasEtapas: List<Etapa>
        get() =
            (etapasF1Dinamicas
                ?: etapasFixas.filter { it.categoria == "F1" }) +
                (etapasF2Dinamicas
                    ?: etapasFixas.filter { it.categoria == "F2" }) +
                etapasFixas.filter {
                    it.categoria != "F1" && it.categoria != "F2"
                }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calendário da F2: usa o último baixado e atualiza em segundo plano.
        etapasF2Dinamicas =
            F2CalendarJson.comoEtapas(F2CalendarJson.carregar(this))
        F2CalendarJson.atualizar(this) {
            etapasF2Dinamicas =
                F2CalendarJson.comoEtapas(F2CalendarJson.carregar(this))

            if (categoriaAtual.isEmpty()) {
                atualizarProximaEtapa()
                mostrarEtapas(todasEtapas)
            }
        }

        // Calendário da F1: usa o último baixado e atualiza em segundo plano.
        etapasF1Dinamicas = F1CalendarApi.carregarCache(this)
        F1CalendarApi.atualizar(this) {
            etapasF1Dinamicas = F1CalendarApi.carregarCache(this)

            if (categoriaAtual.isEmpty()) {
                atualizarProximaEtapa()
                mostrarEtapas(todasEtapas)
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    if (categoriaAtual.isNotEmpty()) {
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

        handlerProximaEtapa.removeCallbacks(
            atualizadorProximaEtapa
        )

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
        setContentView(R.layout.activity_main)

        animarEntrada()

        listaEtapas =
            findViewById(R.id.listaEtapas)

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
        atualizarProximaEtapa()

        handlerProximaEtapa.removeCallbacks(
            atualizadorProximaEtapa
        )

        handlerProximaEtapa.post(
            atualizadorProximaEtapa
        )

        mostrarEtapas(todasEtapas)
    }

    private fun atualizarProximaEtapa() {
        val hoje = java.time.LocalDate.now()

        val proxima =
            todasEtapas
                .filter {
                    val data =
                        java.time.LocalDate.parse(
                            it.dataInicio
                        )

                    data.isAfter(hoje) ||
                        data.isEqual(hoje)
                }
                .minByOrNull {
                    java.time.LocalDate.parse(
                        it.dataInicio
                    )
                }

        if (proxima == null) return

        val titulo =
            findViewById<TextView>(
                R.id.tituloProximaEtapa
            )

        val nome =
            findViewById<TextView>(
                R.id.nomeProximaEtapa
            )

        val bandeira =
            findViewById<ImageView>(
                R.id.bandeiraProximaEtapa
            )

        val circuito =
            findViewById<TextView>(
                R.id.circuitoProximaEtapa
            )

        val data =
            findViewById<TextView>(
                R.id.dataProximaEtapa
            )

        val contador =
            findViewById<TextView>(
                R.id.contadorProximaEtapa
            )

        titulo.text =
            "PRÓXIMA ETAPA • ${proxima.categoria}"

        nome.text = proxima.pais

        val idBandeira =
            recursoBandeira(proxima.pais)

        if (idBandeira != 0) {
            bandeira.setImageResource(idBandeira)
            bandeira.visibility =
                android.view.View.VISIBLE
        } else {
            bandeira.visibility =
                android.view.View.GONE
        }

        circuito.text = proxima.circuito
        data.text = proxima.data

        val dataInicio =
            java.time.LocalDate.parse(
                proxima.dataInicio
            )

        val dias =
            java.time.temporal.ChronoUnit.DAYS.between(
                hoje,
                dataInicio
            )

        contador.text =
            when {
                dias == 0L -> "COMEÇA HOJE"
                dias == 1L -> "AMANHÃ"
                dias > 1L -> "FALTAM $dias DIAS"
                else -> "ETAPA EM ANDAMENTO"
            }

        findViewById<android.view.View>(
            R.id.painelProximaEtapa
        ).setOnClickListener {
            when (proxima.categoria) {
                "F1" -> abrirEventoF1PelaEtapa(proxima)
                "F2" -> abrirEventoF2PelaEtapa(proxima)
                "F3" -> abrirEventoF3PelaEtapa(proxima)
            }
        }
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
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    pesquisaRunnable?.let {
                        handlerPesquisa.removeCallbacks(it)
                    }

                    val texto =
                        s?.toString()
                            ?.trim()
                            ?.lowercase()
                            ?: ""

                    pesquisaRunnable = Runnable {
                        if (texto.isEmpty()) {
                            mostrarEtapas(todasEtapas)
                            return@Runnable
                        }

                        val filtradas =
                            todasEtapas.filter {
                                it.categoria.lowercase().contains(texto) ||
                                    it.pais.lowercase().contains(texto) ||
                                    it.circuito.lowercase().contains(texto) ||
                                    it.data.lowercase().contains(texto)
                            }

                        mostrarEtapas(filtradas)
                    }

                    handlerPesquisa.postDelayed(
                        pesquisaRunnable!!,
                        250
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )
    }

    private fun mostrarEtapas(etapas: List<Etapa>) {
        val hoje = java.time.LocalDate.now()

        val proximaEtapa =
            todasEtapas
                .filter {
                    val data =
                        java.time.LocalDate.parse(
                            it.dataInicio
                        )

                    data.isAfter(hoje) ||
                        data.isEqual(hoje)
                }
                .minByOrNull {
                    java.time.LocalDate.parse(
                        it.dataInicio
                    )
                }

        val etapasVisiveis =
            if (categoriaAtual.isEmpty()) {
                etapas.filter { etapa ->
                    val data =
                        java.time.LocalDate.parse(
                            etapa.dataInicio
                        )

                    data.isAfter(hoje) &&
                        etapa != proximaEtapa
                }
            } else {
                etapas
            }

        val etapasOrdenadas =
            etapasVisiveis.sortedBy {
                java.time.LocalDate.parse(
                    it.dataInicio
                )
            }

        listaEtapas.removeAllViews()

        val inflater = LayoutInflater.from(this)

        if (etapasOrdenadas.isEmpty()) {
            val vazio = TextView(this)

            vazio.text = "Nenhuma etapa encontrada."
            vazio.setTextColor(
                android.graphics.Color.WHITE
            )
            vazio.textSize = 15f
            vazio.gravity = android.view.Gravity.CENTER

            vazio.setPadding(0, 30, 0, 30)
            listaEtapas.addView(vazio)

            return
        }

        for (etapa in etapasOrdenadas) {
            val item =
                inflater.inflate(
                    R.layout.item_etapa,
                    listaEtapas,
                    false
                )

            val status =
                statusEtapa(
                    etapa,
                    etapasOrdenadas
                )

            item.findViewById<TextView>(
                R.id.itemCategoria
            ).text = etapa.categoria

            val imagemBandeira =
                item.findViewById<ImageView>(
                    R.id.itemBandeira
                )

            val idBandeira =
                recursoBandeira(etapa.pais)

            if (idBandeira != 0) {
                imagemBandeira.setImageResource(
                    idBandeira
                )
                imagemBandeira.visibility =
                    android.view.View.VISIBLE
            } else {
                imagemBandeira.visibility =
                    android.view.View.GONE
            }

            item.findViewById<TextView>(
                R.id.itemPais
            ).text = etapa.pais

            item.findViewById<TextView>(
                R.id.itemCircuito
            ).text = etapa.circuito

            item.findViewById<TextView>(
                R.id.itemData
            ).text = etapa.data

            val itemStatus =
                item.findViewById<TextView>(
                    R.id.itemStatus
                )

            itemStatus.text = status

            when (status) {
                "FINALIZADA" -> {
                    itemStatus.setTextColor(
                        android.graphics.Color.WHITE
                    )

                    itemStatus.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.rgb(
                                90, 100, 110
                            )
                        )
                }

                "PRÓXIMA" -> {
                    itemStatus.setTextColor(
                        android.graphics.Color.WHITE
                    )

                    itemStatus.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.rgb(
                                25, 150, 95
                            )
                        )
                }

                "AGENDADA" -> {
                    itemStatus.setTextColor(
                        android.graphics.Color.WHITE
                    )

                    itemStatus.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.rgb(
                                30, 70, 105
                            )
                        )
                }
            }

            item.contentDescription = status

            item.setOnClickListener {
                when (etapa.categoria) {
                    "F1" -> abrirEventoF1PelaEtapa(etapa)
                    "F2" -> abrirEventoF2PelaEtapa(etapa)
                    "F3" -> abrirEventoF3PelaEtapa(etapa)
                }
            }

            listaEtapas.addView(item)
        }
    }

    private fun statusEtapa(
        etapa: Etapa,
        etapasOrdenadas: List<Etapa>
    ): String {
        val hoje = java.time.LocalDate.now()

        val data =
            java.time.LocalDate.parse(
                etapa.dataInicio
            )

        if (data.isBefore(hoje)) {
            return "FINALIZADA"
        }

        val proximasDaCategoria =
            todasEtapas.filter {
                it.categoria == etapa.categoria
            }

        val proxima =
            proximasDaCategoria.firstOrNull {
                val dataInicio =
                    java.time.LocalDate.parse(
                        it.dataInicio
                    )

                !dataInicio.isBefore(hoje)
            }

        return if (proxima == etapa) {
            "PRÓXIMA"
        } else {
            "AGENDADA"
        }
    }

    private fun recursoBandeira(pais: String): Int {
        val arquivo =
            when (pais) {
                "Austrália" -> "flag_au"
                "Japão" -> "flag_jp"
                "Bahrein" -> "flag_bh"
                "Arábia Saudita" -> "flag_sa"
                "Estados Unidos" -> "flag_us"
                "Canadá" -> "flag_ca"
                "Mônaco" -> "flag_mc"
                "Espanha" -> "flag_es"
                "Áustria" -> "flag_at"
                "Reino Unido" -> "flag_gb"
                "Bélgica" -> "flag_be"
                "Hungria" -> "flag_hu"
                "Holanda" -> "flag_nl"
                "Itália" -> "flag_it"
                "Azerbaijão" -> "flag_az"
                "Singapura" -> "flag_sg"
                "México" -> "flag_mx"
                "Brasil" -> "flag_br"
                "Catar" -> "flag_qa"
                "China" -> "flag_cn"
                "Abu Dhabi" -> "flag_ae"
                else -> return 0
            }

        return resources.getIdentifier(
            arquivo,
            "drawable",
            packageName
        )
    }

    private fun bandeiraPais(pais: String): String {
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
            "China" -> "🇨🇳"
            "Abu Dhabi" -> "🇦🇪"
            else -> "🌐"
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
            "F1" -> logo.setImageResource(R.drawable.logo_f1)
            "F2" -> logo.setImageResource(R.drawable.logo_f2)
            "F3" -> logo.setImageResource(R.drawable.logo_f3)
            "IndyCar" -> logo.setImageResource(R.drawable.logo_indycar)
            "Formula E" -> logo.setImageResource(R.drawable.logo_formulae)
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
            categoriaAtual = ""
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

        if (categoria == "F3") {
            evento.setOnClickListener {
                abrirEventoF3()
            }
        }

        calendario.setOnClickListener {
            when (categoriaAtual) {
                "F1" -> {
                    startActivity(
                        Intent(
                            this,
                            F1CalendarActivity::class.java
                        )
                    )

                    overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }

                "F2" -> {
                    startActivity(
                        Intent(
                            this,
                            F2CalendarActivity::class.java
                        )
                    )

                    overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }

                "F3" -> {
                    startActivity(
                        Intent(
                            this,
                            F3CalendarActivity::class.java
                        )
                    )

                    overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }

                else -> {
                    evento.text = "Programação completa"
                }
            }
        }

        when (categoria) {
            "F1" -> carregarF1(evento)
            "F2" -> carregarF2(evento)
            "F3" -> carregarF3(evento)

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
                val races = buscarCorridasF1()
                val agora = java.time.Instant.now()

                var proximaCorrida: JSONObject? = null
                var proximaData: OffsetDateTime? = null

                for (i in 0 until races.length()) {
                    val race = races.getJSONObject(i)

                    val data = race.getString("date")
                    val hora = race.optString("time", "00:00:00Z")

                    val horario =
                        OffsetDateTime.parse(
                            "${data}T${hora}"
                        )

                    if (horario.toInstant().isAfter(agora)) {
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
                    ZoneId.of("America/Sao_Paulo")

                val formato =
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy - HH:mm"
                    )

                val horarioBrasilia =
                    proximaData!!.atZoneSameInstant(brasilia)

                val nome =
                    proximaCorrida!!.getString("raceName")

                val dataCorrida =
                    horarioBrasilia.format(formato)

                val qualificacao =
                    proximaCorrida!!.optJSONObject("Qualifying")

                var textoQualificacao = ""

                if (qualificacao != null) {
                    val dataQualificacao =
                        qualificacao.getString("date")

                    val horaQualificacao =
                        qualificacao.getString("time")

                    val horarioQualificacao =
                        OffsetDateTime.parse(
                            "${dataQualificacao}T${horaQualificacao}"
                        )

                    val horarioBrasiliaQualificacao =
                        horarioQualificacao.atZoneSameInstant(
                            brasilia
                        )

                    textoQualificacao =
                        "🔵 Classificação\n" +
                        "📅 ${
                            horarioBrasiliaQualificacao.format(formato)
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
        startActivity(
            Intent(
                this,
                F1Activity::class.java
            )
        )

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun abrirEventoF1PelaEtapa(etapa: Etapa) {
        val intent =
            Intent(
                this,
                F1Activity::class.java
            )

        intent.putExtra(
            "CIRCUIT_ID_F1",
            etapa.circuitId
        )

        startActivity(intent)

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun carregarF2(evento: TextView) {
        val hoje = java.time.LocalDate.now()

        var proximo: F2Event? = null

        for (item in F2CalendarJson.carregar(this)) {
            val inicio =
                java.time.LocalDate.parse(
                    item.inicio,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )

            if (!inicio.isBefore(hoje)) {
                proximo = item
                break
            }
        }

        if (proximo == null) {
            evento.text = "Temporada 2026 encerrada"
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
        val hoje = java.time.LocalDate.now()

        var proximo: F2Event? = null

        for (item in F2CalendarJson.carregar(this)) {
            val inicio =
                java.time.LocalDate.parse(
                    item.inicio,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )

            if (!inicio.isBefore(hoje)) {
                proximo = item
                break
            }
        }

        if (proximo == null) return

        abrirEventoF2Item(proximo)
    }

    private fun abrirEventoF2PelaEtapa(etapa: Etapa) {
        var encontrado: F2Event? = null

        for (item in F2CalendarJson.carregar(this)) {
            if (
                item.circuito.equals(
                    etapa.circuito,
                    ignoreCase = true
                )
            ) {
                encontrado = item
                break
            }
        }

        if (encontrado != null) {
            abrirEventoF2Item(encontrado)
        } else {
            abrirCategoria("F2")
        }
    }

    private fun abrirEventoF2Item(evento: F2Event) {
        val intent =
            Intent(
                this,
                F2Activity::class.java
            )

        intent.putExtra("ETAPA", evento.etapa)
        startActivity(intent)

        overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }

    private fun carregarF3(evento: TextView) {
        val hoje = java.time.LocalDate.now()

        var proximo: F3Event? = null

        for (item in F3Calendar.eventos) {
            val inicio =
                java.time.LocalDate.parse(
                    item.inicio,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )

            if (!inicio.isBefore(hoje)) {
                proximo = item
                break
            }
        }

        if (proximo == null) {
            evento.text = "Temporada 2026 encerrada"
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
        val hoje = java.time.LocalDate.now()

        var proximo: F3Event? = null

        for (item in F3Calendar.eventos) {
            val inicio =
                java.time.LocalDate.parse(
                    item.inicio,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )

            if (!inicio.isBefore(hoje)) {
                proximo = item
                break
            }
        }

        if (proximo == null) return

        abrirEventoF3Item(proximo)
    }

    private fun abrirEventoF3PelaEtapa(etapa: Etapa) {
        var encontrado: F3Event? = null

        for (item in F3Calendar.eventos) {
            if (
                item.circuito.equals(
                    etapa.circuito,
                    ignoreCase = true
                )
            ) {
                encontrado = item
                break
            }
        }

        if (encontrado != null) {
            abrirEventoF3Item(encontrado)
        } else {
            abrirCategoria("F3")
        }
    }

    private fun abrirEventoF3Item(evento: F3Event) {
        val intent =
            Intent(
                this,
                F3Activity::class.java
            )

        intent.putExtra("ETAPA", evento.etapa)
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
            url.openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        connection.setRequestProperty(
            "User-Agent",
            "CalendarioMotorsport/1.0"
        )

        val resposta =
            connection.inputStream
                .bufferedReader()
                .use { it.readText() }

        connection.disconnect()

        return JSONObject(resposta)
            .getJSONObject("MRData")
            .getJSONObject("RaceTable")
            .getJSONArray("Races")
    }
}
