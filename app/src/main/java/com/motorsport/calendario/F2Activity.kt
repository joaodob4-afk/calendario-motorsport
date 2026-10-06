package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class F2Activity : AppCompatActivity() {

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

        overridePendingTransition(
            R.anim.fade_in,
            0
        )

        val etapa =
            intent.getIntExtra(
                "ETAPA",
                -1
            )

        val evento =
            F2Calendar.eventos.find {
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

        val titulo =
            TextView(this).apply {

                text =
                    if (evento != null) {
                        "FÓRMULA 2\n\n" +
                        "ETAPA ${evento.etapa}"
                    } else {
                        "FÓRMULA 2"
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

        val voltar =
            TextView(this).apply {

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

                setPadding(
                    0,
                    24,
                    0,
                    20
                )

                setOnClickListener {
                    finish()
                }
            }

        conteudo.addView(
            voltar
        )

        val scrollView =
            ScrollView(this).apply {
                addView(conteudo)
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

    private fun mostrarEvento(
        layout: LinearLayout,
        evento: F2Event
    ) {

        val cabecalho =
            TextView(this).apply {

                text =
                    "🏁 ${evento.circuito}\n\n" +
                    "📍 ${evento.pais}\n\n" +
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
        sessao: F2Session
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

        val card =
            TextView(this).apply {

                text =
                    if (dataHora != null) {

                        "${sessao.nome}\n\n" +
                        "📅 " +
                        dataHora.format(
                            DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy - HH:mm"
                            )
                        )

                    } else {

                        "${sessao.nome}\n\n" +
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
                LocalDateTime.now()
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