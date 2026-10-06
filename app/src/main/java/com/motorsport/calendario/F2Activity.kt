package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class F2Activity : AppCompatActivity() {

    private val handler =
        Handler(Looper.getMainLooper())

    private var contadorView:
            TextView? = null

    private var proximoHorario:
            LocalDateTime? = null

    private val atualizarContador =
        object : Runnable {

            override fun run() {

                atualizarTextoContador()

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

        val etapaNumero =
            intent.getIntExtra(
                "ETAPA",
                1
            )

        val evento =
            F2Calendar.eventos.find {
                it.etapa == etapaNumero
            }

        if (evento == null) {
            finish()
            return
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
                    Color.rgb(11, 11, 15)
                )
            }

        val titulo =
            TextView(this).apply {

                text =
                    "FÓRMULA 2\n\n" +
                    "PROGRAMAÇÃO DA ETAPA"

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

        conteudo.addView(titulo)

        val etapa =
            TextView(this).apply {

                text =
                    "ETAPA ${evento.etapa}\n\n" +
                    "${evento.circuito}\n" +
                    "🇺🇳 ${evento.pais}"

                textSize = 21f

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
                    22
                )

                setBackgroundColor(
                    Color.rgb(36, 36, 43)
                )
            }

        val parametrosEtapa =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosEtapa.setMargins(
            0,
            0,
            0,
            12
        )

        conteudo.addView(
            etapa,
            parametrosEtapa
        )

        val periodo =
            TextView(this).apply {

                text =
                    "📅 ${evento.inicio} → ${evento.fim}"

                textSize = 15f

                setTextColor(
                    Color.GRAY
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    8,
                    0,
                    24
                )
            }

        conteudo.addView(periodo)

        val separador =
            TextView(this).apply {

                text = "PROGRAMAÇÃO"

                textSize = 13f

                setTextColor(
                    Color.rgb(119, 119, 127)
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                letterSpacing = 0.10f

                setPadding(
                    0,
                    8,
                    0,
                    10
                )
            }

        conteudo.addView(separador)

        val agora =
            LocalDateTime.now()

        val formato =
            DateTimeFormatter.ofPattern(
                "dd/MM/yyyy HH:mm"
            )

        var proximaSessao:
                F2Session? = null

        for (sessao in evento.sessoes) {

            if (
                sessao.horario ==
                "A confirmar"
            ) {
                continue
            }

            try {

                val horario =
                    LocalDateTime.parse(
                        "${sessao.data} ${sessao.horario}",
                        formato
                    )

                if (
                    horario.isAfter(agora) &&
                    (
                        proximoHorario == null ||
                        horario.isBefore(
                            proximoHorario
                        )
                    )
                ) {

                    proximaSessao =
                        sessao

                    proximoHorario =
                        horario
                }

            } catch (_: Exception) {
            }
        }

        for (sessao in evento.sessoes) {

            val emoji =
                when {

                    sessao.horario ==
                            "A confirmar" ->
                        "⚪"

                    sessao.nome.contains(
                        "Treino",
                        ignoreCase = true
                    ) ->
                        "🟢"

                    sessao.nome.contains(
                        "Classificação",
                        ignoreCase = true
                    ) ->
                        "🔵"

                    sessao.nome.contains(
                        "Sprint",
                        ignoreCase = true
                    ) ->
                        "🟡"

                    sessao.nome.contains(
                        "Feature",
                        ignoreCase = true
                    ) ->
                        "🔴"

                    else ->
                        "⚪"
                }

            val eProxima =
                sessao == proximaSessao

            adicionarSessao(
                conteudo,
                sessao,
                emoji,
                eProxima
            )
        }

        val voltar =
            Button(this).apply {

                text = "VOLTAR"

                textSize = 15f

                setOnClickListener {
                    finish()
                }
            }

        val parametrosVoltar =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametrosVoltar.setMargins(
            0,
            24,
            0,
            20
        )

        conteudo.addView(
            voltar,
            parametrosVoltar
        )

        val scrollView =
            ScrollView(this).apply {
                addView(conteudo)
            }

        setContentView(scrollView)

        handler.post(
            atualizarContador
        )
    }

    private fun adicionarSessao(
        layout: LinearLayout,
        sessao: F2Session,
        emoji: String,
        eProxima: Boolean
    ) {

        val sessaoView =
            TextView(this).apply {

                text =
                    "$emoji  ${sessao.nome}\n\n" +
                    "📅 ${sessao.data}\n" +
                    "🕐 ${sessao.horario}"

                textSize = 17f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setPadding(
                    20,
                    20,
                    20,
                    20
                )

                setBackgroundColor(
                    Color.rgb(36, 36, 43)
                )
            }

        if (eProxima) {

            contadorView =
                TextView(this).apply {

                    textSize = 15f

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
                        10,
                        16,
                        16
                    )

                    setBackgroundColor(
                        Color.rgb(48, 48, 56)
                    )
                }

            layout.addView(
                sessaoView
            )

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

        } else {

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
                sessaoView,
                parametros
            )
        }
    }

    private fun atualizarTextoContador() {

        val view =
            contadorView ?: return

        val horario =
            proximoHorario ?: return

        val agora =
            LocalDateTime.now()

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
                "🏁 A sessão está começando!"

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

                "⏳  COMEÇA EM  " +
                "${dias}d ${horas}h ${minutos}min"

            } else if (horas > 0) {

                "⏳  COMEÇA EM  " +
                "${horas}h ${minutos}min"

            } else {

                "⏳  COMEÇA EM  " +
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