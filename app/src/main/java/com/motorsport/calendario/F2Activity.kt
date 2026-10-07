package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class F2Activity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var proximoHorario: LocalDateTime? = null

    private val formatoData =
        DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")

    override fun onCreate(savedInstanceState: Bundle?) {
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

        val layoutPrincipal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(7, 26, 45))
            setPadding(20, 20, 20, 20)
        }

        layoutPrincipal.addView(criarBotaoVoltar())

        val titulo = TextView(this).apply {
            text = "FÓRMULA 2"
            setTextColor(Color.WHITE)
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 28)
        }

        layoutPrincipal.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(
            conteudo,
            ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
            )
        )

        layoutPrincipal.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(layoutPrincipal)

        mostrarEvento(conteudo)

        atualizarTextoContador()
    }

    private fun criarBotaoVoltar(): TextView {
        return TextView(this).apply {
            text = "‹  VOLTAR"
            setTextColor(Color.rgb(143, 166, 186))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
            setPadding(0, 0, 0, 18)

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

    private fun mostrarEvento(layout: LinearLayout) {
        val etapaSelecionada =
            intent.getIntExtra("ETAPA", -1)

        val eventoSelecionado =
            if (etapaSelecionada > 0) {
                F2Calendar.eventos.firstOrNull {
                    it.etapa == etapaSelecionada
                }
            } else {
                F2Calendar.eventos.firstOrNull {
                    val inicio = LocalDateTime.parse(
                        it.inicio,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    )

                    inicio.isAfter(LocalDateTime.now())
                }
            }

        if (eventoSelecionado == null) {
            val texto = TextView(this).apply {
                text = "Nenhum evento encontrado."
                setTextColor(Color.WHITE)
                textSize = 17f
                gravity = Gravity.CENTER
                setPadding(0, 30, 0, 30)
            }

            layout.addView(texto)
            return
        }

        val cabecalho = TextView(this).apply {
            text =
                "🏁 ETAPA ${eventoSelecionado.etapa}\n\n" +
                "${eventoSelecionado.circuito}\n" +
                "${obterBandeira(eventoSelecionado.pais)} ${eventoSelecionado.pais}"

            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(20, 22, 20, 22)
            setBackgroundResource(R.drawable.rounded_card)
        }

        layout.addView(
            cabecalho,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        )

        adicionarSessao(
            layout,
            "🟢 TREINO",
            eventoSelecionado.treino
        )

        adicionarSessao(
            layout,
            "🔵 CLASSIFICAÇÃO",
            eventoSelecionado.classificacao
        )

        adicionarSessao(
            layout,
            "🔴 CORRIDA 1",
            eventoSelecionado.corrida1
        )

        adicionarSessao(
            layout,
            "🔴 CORRIDA 2",
            eventoSelecionado.corrida2
        )
    }

    private fun adicionarSessao(
        layout: LinearLayout,
        titulo: String,
        horarioTexto: String
    ) {
        try {
            val horario =
                LocalDateTime.parse(
                    horarioTexto,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                )

            if (horario.isAfter(
                    LocalDateTime.now()
                )
            ) {
                if (proximoHorario == null ||
                    horario.isBefore(proximoHorario)
                ) {
                    proximoHorario = horario
                }
            }

            val card = TextView(this).apply {
                text =
                    "$titulo\n\n${horario.format(formatoData)}"

                setTextColor(Color.WHITE)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(20, 20, 20, 20)
                setBackgroundResource(R.drawable.rounded_card)
            }

            layout.addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
            )

        } catch (_: Exception) {
        }
    }

    private fun atualizarTextoContador() {
        val horario = proximoHorario ?: return

        val agora = LocalDateTime.now()

        val duracao =
            Duration.between(agora, horario)

        if (duracao.isNegative || duracao.isZero) {
            handler.postDelayed(
                { atualizarTextoContador() },
                60000
            )
            return
        }

        handler.postDelayed(
            { atualizarTextoContador() },
            60000
        )
    }

    private fun obterBandeira(pais: String): String {
        return when (pais.lowercase()) {
            "bahrain" -> "🇧🇭"
            "saudi arabia" -> "🇸🇦"
            "australia" -> "🇦🇺"
            "japan" -> "🇯🇵"
            "china" -> "🇨🇳"
            "italy" -> "🇮🇹"
            "monaco" -> "🇲🇨"
            "spain" -> "🇪🇸"
            "canada" -> "🇨🇦"
            "austria" -> "🇦🇹"
            "united kingdom" -> "🇬🇧"
            "uk" -> "🇬🇧"
            "hungary" -> "🇭🇺"
            "belgium" -> "🇧🇪"
            "netherlands" -> "🇳🇱"
            "azerbaijan" -> "🇦🇿"
            "singapore" -> "🇸🇬"
            "qatar" -> "🇶🇦"
            "uae" -> "🇦🇪"
            "united arab emirates" -> "🇦🇪"
            "brazil" -> "🇧🇷"
            else -> "🌎"
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}