package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class F3Activity : AppCompatActivity() {

    private val handler =
        Handler(Looper.getMainLooper())

    private var contadorView: TextView? = null

    private var proximoHorario:
            LocalDateTime? = null

    private val atualizarContador =
        object : Runnable {

            override fun run() {

                atualizarContadorTela()

                handler.postDelayed(
                    this,
                    60_000
                )
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        // Baixa o calendário F3 mais recente em segundo plano.
        F3CalendarJson.atualizar(this)

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

        val etapa =
            intent.getIntExtra(
                "ETAPA",
                -1
            )

        val evento =
            F3CalendarJson.carregar(this).find {
                it.etapa == etapa
            }

        val conteudo =
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

        val voltarTopo =
            criarBotaoVoltar()

        val parametrosVoltarTopo =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosVoltarTopo.gravity =
            Gravity.START

        parametrosVoltarTopo.setMargins(
            0,
            0,
            0,
            10
        )

        conteudo.addView(
            voltarTopo,
            parametrosVoltarTopo
        )

        val titulo =
            TextView(this).apply {

                text =
                    if (evento != null) {
                        "FÓRMULA 3\n\n" +
                        "ETAPA ${evento.etapa}"
                    } else {
                        "FÓRMULA 3"
                    }

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

        conteudo.addView(
            titulo
        )

        if (evento == null) {

            val erro =
                TextView(this).apply {

                    text =
                        "Etapa não encontrada."

                    textSize = 17f

                    setTextColor(
                        Color.LTGRAY
                    )

                    gravity =
                        Gravity.CENTER

                    setPadding(
                        0,
                        30,
                        0,
                        30
                    )
                }

            conteudo.addView(
                erro
            )

        } else {

            mostrarEvento(
                conteudo,
                evento
            )
        }

        adicionarBotaoVoltarFundo(
            conteudo
        )

        val scrollView =
            ScrollView(this).apply {

                isVerticalScrollBarEnabled = false

                addView(
                    conteudo,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }

        setContentView(
            scrollView
        )

        if (evento != null) {

            handler.post(
                atualizarContador
            )
        }
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
                Gravity.START

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

    private fun mostrarEvento(
        layout: LinearLayout,
        evento: F3Event
    ) {

        val bandeira =
            obterBandeira(
                evento.pais
            )

        val nomeAutodromo = Autodromos.nome(evento.circuito)

        val linhaAutodromo =
            if (nomeAutodromo.isNotBlank() &&
                !nomeAutodromo.equals(evento.circuito, ignoreCase = true)
            ) {
                "$nomeAutodromo\n"
            } else {
                ""
            }

        val cabecalho =
            TextView(this).apply {

                text =
                    "🏁 ${evento.circuito}\n" +
                    linhaAutodromo +
                    "\n" +
                    "$bandeira ${evento.pais}\n\n" +
                    "📅 ${evento.inicio} — ${evento.fim}"

                textSize = 19f

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
                    16,
                    22,
                    16,
                    26
                )

                background =
                    getDrawable(
                        R.drawable.rounded_card
                    )
            }

        val parametrosCabecalho =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosCabecalho.setMargins(
            0,
            0,
            0,
            20
        )

        layout.addView(
            cabecalho,
            parametrosCabecalho
        )

        for (
            sessao in evento.sessoes
        ) {

            adicionarSessao(
                layout,
                sessao
            )
        }
    }

    private fun adicionarSessao(
        layout: LinearLayout,
        sessao: F3Session
    ) {

        val dataHora =
            if (
                sessao.horario.equals(
                    "A confirmar",
                    ignoreCase = true
                )
            ) {
                null
            } else {

                try {

                    LocalDateTime.parse(
                        "${sessao.data} ${sessao.horario}",
                        DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy HH:mm"
                        )
                    )

                } catch (_: Exception) {
                    null
                }
            }

        val nomeSessao =
            obterNomeSessao(
                sessao.nome
            )

        val card =
            TextView(this).apply {

                text =
                    if (dataHora != null) {

                        "$nomeSessao\n\n" +
                        "📅 " +
                        dataHora.format(
                            DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy - HH:mm"
                            )
                        )

                    } else {

                        "$nomeSessao\n\n" +
                        "📅 A confirmar"
                    }

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

        if (
            dataHora != null &&
            dataHora.isAfter(
                LocalDateTime.now(
                    ZoneId.of(
                        "America/Sao_Paulo"
                    )
                )
            ) &&
            (
                proximoHorario == null ||
                dataHora.isBefore(
                    proximoHorario
                )
            )
        ) {

            proximoHorario =
                dataHora

            contadorView =
                TextView(this).apply {

                    textSize = 16f

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
                        16,
                        12,
                        16,
                        16
                    )

                    background =
                        getDrawable(
                            R.drawable.rounded_card
                        )
                }

            val parametrosContador =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            parametrosContador.setMargins(
                0,
                0,
                0,
                6
            )

            layout.addView(
                contadorView,
                parametrosContador
            )
        }
    }

    private fun adicionarBotaoVoltarFundo(
        layout: LinearLayout
    ) {

        val espaco =
            Space(this)

        val parametrosEspaco =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            )

        parametrosEspaco.weight =
            1f

        layout.addView(
            espaco,
            parametrosEspaco
        )

        val voltarFundo =
            criarBotaoVoltar()

        val parametrosVoltarFundo =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosVoltarFundo.gravity =
            Gravity.START

        parametrosVoltarFundo.setMargins(
            0,
            20,
            0,
            10
        )

        layout.addView(
            voltarFundo,
            parametrosVoltarFundo
        )
    }

    private fun obterNomeSessao(
        nomeOriginal: String
    ): String {

        val nome =
            nomeOriginal.lowercase()

        return when {

            nome.contains("treino") ||
            nome.contains("practice") -> {

                "🟢  $nomeOriginal"
            }

            nome.contains("classificação") ||
            nome.contains("qualifying") ||
            nome.contains("qualificacao") -> {

                "🔵  $nomeOriginal"
            }

            nome.contains("sprint") -> {

                "🟡  $nomeOriginal"
            }

            nome.contains("corrida") ||
            nome.contains("race") -> {

                "🔴  $nomeOriginal"
            }

            else -> {

                nomeOriginal
            }
        }
    }

    private fun obterBandeira(
        pais: String
    ): String {

        return when (
            pais.lowercase()
                .trim()
        ) {

            "austrália",
            "australia" -> "🇦🇺"

            "mônaco",
            "monaco" -> "🇲🇨"

            "espanha",
            "spain" -> "🇪🇸"

            "áustria",
            "austria" -> "🇦🇹"

            "reino unido",
            "united kingdom",
            "uk" -> "🇬🇧"

            "bélgica",
            "belgica",
            "belgium" -> "🇧🇪"

            "hungria",
            "hungría",
            "hungary" -> "🇭🇺"

            "itália",
            "italia",
            "italy" -> "🇮🇹"

            else -> "🏳️"
        }
    }

    private fun atualizarContadorTela() {

        val view =
            contadorView ?: return

        val horario =
            proximoHorario ?: return

        val agora =
            LocalDateTime.now(
                ZoneId.of(
                    "America/Sao_Paulo"
                )
            )

        val duracao =
            Duration.between(
                agora,
                horario
            )

        if (
            duracao.isZero ||
            duracao.isNegative
        ) {

            view.text =
                "🏁 A próxima sessão está começando!"

            return
        }

        val totalMinutos =
            duracao.toMinutes()

        val dias =
            totalMinutos / 1440

        val horas =
            (totalMinutos % 1440) / 60

        val minutos =
            totalMinutos % 60

        view.text =
            if (dias > 0) {

                "⏳  PRÓXIMA SESSÃO EM  " +
                "${dias}d ${horas}h ${minutos}min"

            } else if (horas > 0) {

                "⏳  PRÓXIMA SESSÃO EM  " +
                "${horas}h ${minutos}min"

            } else {

                "⏳  PRÓXIMA SESSÃO EM  " +
                "${minutos}min"
            }
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            atualizarContador
        )

        super.onDestroy()
    }
}