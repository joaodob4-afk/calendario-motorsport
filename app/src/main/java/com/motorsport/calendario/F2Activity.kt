package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class F2Activity : AppCompatActivity() {

    private val preto = Color.BLACK
    private val verde = Color.rgb(57, 255, 20)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(175, 175, 175)
    private val fundoCard = Color.rgb(15, 20, 15)
    private val verdeEscuro = Color.rgb(20, 75, 20)
    private val linhaCinza = Color.rgb(55, 55, 55)

    private val brasilia =
        ZoneId.of("America/Sao_Paulo")

    private val localePt =
        Locale("pt", "BR")

    private lateinit var conteudo: LinearLayout

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

        val etapa = intent.getIntExtra("ETAPA", -1)

        val evento = F2Calendar.eventos.find {
            it.etapa == etapa
        }

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(preto)
            setPadding(dp(18), dp(12), dp(18), dp(20))
        }

        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val voltar = ImageView(this).apply {
            val recurso = resources.getIdentifier(
                "back_button",
                "drawable",
                packageName
            )

            if (recurso != 0) {
                setImageResource(recurso)
            } else {
                setImageResource(
                    android.R.drawable.ic_media_previous
                )
                setColorFilter(verde)
            }

            contentDescription = "Voltar"
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(4), dp(4), dp(12), dp(4))

            setOnClickListener {
                voltarParaTelaAnterior()
            }
        }

        cabecalho.addView(
            voltar,
            LinearLayout.LayoutParams(dp(42), dp(42))
        )

        val tituloTela = TextView(this).apply {
            text = "FÓRMULA 2"
            textSize = 23f
            setTextColor(branco)
            typeface = Typeface.create(
                "sans-serif-black",
                Typeface.NORMAL
            )
            letterSpacing = 0.06f
        }

        cabecalho.addView(
            tituloTela,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        raiz.addView(cabecalho)

        val detalhe = View(this).apply {
            setBackgroundColor(verde)
        }

        raiz.addView(
            detalhe,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(2)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(20)
            }
        )

        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, dp(20))
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            isVerticalFadingEdgeEnabled = false

            addView(
                conteudo,
                ScrollView.LayoutParams(
                    ScrollView.LayoutParams.MATCH_PARENT,
                    ScrollView.LayoutParams.WRAP_CONTENT
                )
            )
        }

        raiz.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(raiz)

        if (evento == null) {
            adicionarTexto(
                conteudo,
                "ETAPA NÃO ENCONTRADA",
                17f,
                verde,
                true
            )

            adicionarTexto(
                conteudo,
                "Não foi possível encontrar os dados desta etapa.",
                14f,
                cinza,
                false
            ).setPadding(0, dp(12), 0, dp(20))
        } else {
            mostrarEvento(conteudo, evento)
        }
    }

    private fun voltarParaTelaAnterior() {
        finish()

        overridePendingTransition(
            android.R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }

    private fun mostrarEvento(
        layout: LinearLayout,
        evento: F2Event
    ) {
        adicionarTexto(
            layout,
            "PRÓXIMA ETAPA",
            12f,
            verde,
            true
        ).apply {
            letterSpacing = 0.16f
            setPadding(0, 0, 0, dp(5))
        }

        adicionarTexto(
            layout,
            "ETAPA ${evento.etapa}",
            28f,
            branco,
            true
        ).setPadding(0, dp(3), 0, dp(12))

        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(18))

            background = criarFundoCard(
                Color.rgb(10, 25, 10),
                verde,
                2
            )
        }

        adicionarTexto(
            cabecalho,
            evento.circuito,
            20f,
            branco,
            true
        )

        val localLinha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }

        val bandeira = TextView(this).apply {
            text = obterBandeira(evento.pais)
            textSize = 25f
            gravity = Gravity.CENTER
        }

        localLinha.addView(
            bandeira,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dp(10)
            }
        )

        val pais = TextView(this).apply {
            text = evento.pais
            textSize = 14f
            setTextColor(cinza)
        }

        localLinha.addView(
            pais,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        cabecalho.addView(localLinha)

        val separador = View(this).apply {
            setBackgroundColor(verdeEscuro)
        }

        cabecalho.addView(
            separador,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(15)
                bottomMargin = dp(12)
            }
        )

        adicionarTexto(
            cabecalho,
            "${evento.inicio}  —  ${evento.fim}",
            14f,
            branco,
            true
        )

        layout.addView(
            cabecalho,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(22)
            }
        )

        adicionarTexto(
            layout,
            "PROGRAMAÇÃO",
            15f,
            verde,
            true
        ).apply {
            letterSpacing = 0.1f
            setPadding(0, 0, 0, dp(12))
        }

        for (sessao in evento.sessoes) {
            adicionarSessao(layout, sessao)
        }
    }

    private fun adicionarSessao(
        layout: LinearLayout,
        sessao: F2Session
    ) {
        val dataHora = if (
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

        val nome = obterNomeSessao(sessao.nome)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(15), dp(14), dp(15))

            val eCorrida =
                sessao.nome.contains(
                    "corrida",
                    ignoreCase = true
                ) ||
                sessao.nome.contains(
                    "race",
                    ignoreCase = true
                )

            background = criarFundoCard(
                if (eCorrida) {
                    Color.rgb(15, 45, 15)
                } else {
                    fundoCard
                },
                if (eCorrida) verde else verdeEscuro,
                if (eCorrida) 2 else 1
            )
        }

        val nomeSessao = TextView(this).apply {
            text = nome
            textSize = 13f
            setTextColor(
                if (
                    sessao.nome.contains(
                        "corrida",
                        ignoreCase = true
                    ) ||
                    sessao.nome.contains(
                        "race",
                        ignoreCase = true
                    )
                ) verde else branco
            )

            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
        }

        card.addView(
            nomeSessao,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val horario = TextView(this).apply {
            text = dataHora?.format(
                DateTimeFormatter.ofPattern("HH:mm")
            ) ?: "--:--"

            textSize = 17f
            setTextColor(verde)
            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
            gravity = Gravity.END
        }

        card.addView(
            horario,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(8)
            }
        )

        layout.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(5)
            }
        )

        val dataTexto = TextView(this).apply {
            text = if (dataHora != null) {
                dataHora.format(
                    DateTimeFormatter.ofPattern(
                        "EEEE, dd/MM/yyyy",
                        localePt
                    )
                ).uppercase(localePt)
            } else {
                "${sessao.data}  •  A CONFIRMAR"
            }

            textSize = 11f
            setTextColor(cinza)
            setPadding(dp(4), 0, dp(4), dp(12))
        }

        layout.addView(dataTexto)
    }

    private fun obterNomeSessao(
        nomeOriginal: String
    ): String {
        val nome = nomeOriginal.lowercase(localePt)

        return when {
            nome.contains("treino") ||
                nome.contains("practice") ->
                "TREINO  •  $nomeOriginal"

            nome.contains("classificação") ||
                nome.contains("qualifying") ||
                nome.contains("qualificacao") ->
                "CLASSIFICAÇÃO  •  $nomeOriginal"

            nome.contains("sprint") ->
                "SPRINT  •  $nomeOriginal"

            nome.contains("corrida") ||
                nome.contains("race") ->
                "CORRIDA  •  $nomeOriginal"

            else -> nomeOriginal
        }
    }

    private fun obterBandeira(pais: String): String {
        return when (pais.lowercase(localePt).trim()) {
            "austrália", "australia" -> "🇦🇺"
            "mônaco", "monaco" -> "🇲🇨"
            "reino unido", "united kingdom", "uk" -> "🇬🇧"
            "hungria", "hungría", "hungary" -> "🇭🇺"
            "bélgica", "belgica", "belgium" -> "🇧🇪"
            "itália", "italia", "italy" -> "🇮🇹"
            "áustria", "austria" -> "🇦🇹"
            "países baixos", "paises baixos", "netherlands" -> "🇳🇱"
            "azerbaijão", "azerbaijan" -> "🇦🇿"
            "catar", "qatar" -> "🇶🇦"
            "emirados árabes unidos",
            "united arab emirates", "uae" -> "🇦🇪"
            "bahrein", "bahrain" -> "🇧🇭"
            "arábia saudita", "arabia saudita",
            "saudi arabia" -> "🇸🇦"
            "japão", "japao", "japan" -> "🇯🇵"
            "espanha", "spain" -> "🇪🇸"
            "canadá", "canada" -> "🇨🇦"
            "estados unidos", "united states", "usa" -> "🇺🇸"
            "singapura", "singapore" -> "🇸🇬"
            "méxico", "mexico" -> "🇲🇽"
            "brasil", "brazil" -> "🇧🇷"
            else -> "🏳️"
        }
    }

    private fun adicionarTexto(
        layout: LinearLayout,
        texto: String,
        tamanho: Float,
        cor: Int,
        negrito: Boolean
    ): TextView {
        val view = TextView(this).apply {
            text = texto
            textSize = tamanho
            setTextColor(cor)

            if (negrito) {
                typeface = Typeface.create(
                    "sans-serif",
                    Typeface.BOLD
                )
            }
        }

        layout.addView(
            view,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return view
    }

    private fun criarFundoCard(
        corFundo: Int,
        corBorda: Int,
        espessura: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(9).toFloat()
            setColor(corFundo)
            setStroke(dp(espessura), corBorda)
        }
    }

    private fun dp(valor: Int): Int {
        return (
            valor * resources.displayMetrics.density
        ).toInt()
    }
}