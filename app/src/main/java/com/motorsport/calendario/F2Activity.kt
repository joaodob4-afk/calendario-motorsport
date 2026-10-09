package com.motorsport.calendario

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class F2Activity : AppCompatActivity() {

    private val preto = Color.rgb(5, 5, 5)
    private val verde = Color.rgb(57, 255, 20)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(165, 165, 165)
    private val cardEscuro = Color.rgb(18, 18, 18)

    private lateinit var raiz: LinearLayout
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

        raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(preto)
            setPadding(dp(18), dp(12), dp(18), dp(20))
        }

        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val botaoVoltar = TextView(this).apply {
            text = "‹"
            textSize = 38f
            setTextColor(verde)
            gravity = Gravity.CENTER
            includeFontPadding = false
            setPadding(dp(4), 0, dp(16), dp(4))
            contentDescription = "Voltar"
            setOnClickListener {
                voltarParaTelaAnterior()
            }
        }

        cabecalho.addView(
            botaoVoltar,
            LinearLayout.LayoutParams(
                dp(42),
                dp(48)
            )
        )

        val titulo = TextView(this).apply {
            text = "FÓRMULA 2"
            textSize = 23f
            setTextColor(branco)
            typeface = Typeface.create(
                "sans-serif-condensed",
                Typeface.BOLD
            )
            letterSpacing = 0.12f
        }

        cabecalho.addView(
            titulo,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        raiz.addView(
            cabecalho,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val linhaVerde = View(this).apply {
            setBackgroundColor(verde)
        }

        val margemLinha = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(2)
        )
        margemLinha.topMargin = dp(4)
        margemLinha.bottomMargin = dp(16)

        raiz.addView(linhaVerde, margemLinha)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            isVerticalFadingEdgeEnabled = false

            addView(
                conteudoContainer(),
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
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
                "Etapa não encontrada.",
                18f,
                branco,
                true
            )
        } else {
            mostrarEvento(evento)
        }
    }

    private fun conteudoContainer(): LinearLayout {
        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        return conteudo
    }

    private fun voltarParaTelaAnterior() {
        finish()
        overridePendingTransition(
            android.R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }

    private fun mostrarEvento(evento: F2Event) {

        adicionarTexto(
            conteudo,
            "PRÓXIMA ETAPA",
            12f,
            verde,
            true
        )

        val numeroEtapa = TextView(this).apply {
            text = "ETAPA ${evento.etapa}"
            textSize = 28f
            setTextColor(branco)
            typeface = Typeface.create(
                "sans-serif-condensed",
                Typeface.BOLD
            )
            letterSpacing = 0.06f
        }

        val margemNumero = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        margemNumero.topMargin = dp(5)
        margemNumero.bottomMargin = dp(14)

        conteudo.addView(numeroEtapa, margemNumero)

        val cardEvento = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = criarFundoCard(
                cardEscuro,
                verde,
                14
            )
        }

        adicionarTexto(
            cardEvento,
            evento.circuito,
            23f,
            branco,
            true
        )

        val localizacao = TextView(this).apply {
            text = "${obterBandeira(evento.pais)}  ${evento.pais}"
            textSize = 15f
            setTextColor(cinza)
        }

        val margemLocalizacao = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        margemLocalizacao.topMargin = dp(10)

        cardEvento.addView(localizacao, margemLocalizacao)

        val datas = TextView(this).apply {
            text = "${formatarData(evento.inicio)} — ${formatarData(evento.fim)}"
            textSize = 14f
            setTextColor(verde)
        }

        val margemDatas = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        margemDatas.topMargin = dp(12)

        cardEvento.addView(datas, margemDatas)

        val margemCard = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        margemCard.bottomMargin = dp(24)

        conteudo.addView(cardEvento, margemCard)

        adicionarTexto(
            conteudo,
            "PROGRAMAÇÃO",
            17f,
            verde,
            true
        )

        val margemProgramacao = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        margemProgramacao.bottomMargin = dp(12)

        conteudo.getChildAt(conteudo.childCount - 1)
            .layoutParams = margemProgramacao

        if (evento.sessoes.isEmpty()) {
            adicionarTexto(
                conteudo,
                "Horários ainda não disponíveis.",
                14f,
                cinza,
                false
            )
        } else {
            evento.sessoes.forEach { sessao ->
                adicionarSessao(
                    sessao.nome,
                    sessao.data,
                    sessao.horario
                )
            }
        }
    }

    private fun adicionarSessao(
        nome: String,
        data: String,
        horario: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = criarFundoCard(
                cardEscuro,
                Color.rgb(45, 45, 45),
                10
            )
        }

        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val nomeSessao = TextView(this).apply {
            text = obterNomeSessao(nome)
            textSize = 15f
            setTextColor(branco)
            typeface = Typeface.DEFAULT_BOLD
        }

        linha.addView(
            nomeSessao,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val horaSessao = TextView(this).apply {
            text = horario
            textSize = 15f
            setTextColor(verde)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
        }

        linha.addView(
            horaSessao,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        card.addView(
            linha,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        if (data.isNotBlank()) {
            val dataSessao = TextView(this).apply {
                text = formatarData(data)
                textSize = 12f
                setTextColor(cinza)
            }

            val margemData = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            margemData.topMargin = dp(7)

            card.addView(dataSessao, margemData)
        }

        val margemCard = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        margemCard.bottomMargin = dp(10)

        conteudo.addView(card, margemCard)
    }

    private fun obterNomeSessao(nome: String): String {
        val texto = nome.trim()

        return when (texto.lowercase(Locale.ROOT)) {
            "practice" -> "TREINO LIVRE"
            "practice session" -> "TREINO LIVRE"
            "qualifying" -> "CLASSIFICAÇÃO"
            "sprint qualifying" -> "CLASSIFICAÇÃO DA SPRINT"
            "sprint race" -> "CORRIDA SPRINT"
            "feature race" -> "CORRIDA PRINCIPAL"
            "race" -> "CORRIDA"
            else -> texto.uppercase(Locale.getDefault())
        }
    }

    private fun obterBandeira(pais: String): String {
        return when (pais.trim().lowercase(Locale.ROOT)) {
            "australia", "austrália" -> "🇦🇺"
            "bahrain", "barein" -> "🇧🇭"
            "saudi arabia", "arábia saudita" -> "🇸🇦"
            "italy", "itália" -> "🇮🇹"
            "monaco", "mônaco" -> "🇲🇨"
            "spain", "espanha" -> "🇪🇸"
            "canada", "canadá" -> "🇨🇦"
            "austria", "áustria" -> "🇦🇹"
            "united kingdom", "reino unido", "great britain" -> "🇬🇧"
            "hungary", "hungria", "hungria" -> "🇭🇺"
            "belgium", "bélgica" -> "🇧🇪"
            "netherlands", "holanda", "países baixos" -> "🇳🇱"
            "azerbaijan", "azerbaijão" -> "🇦🇿"
            "singapore", "singapura" -> "🇸🇬"
            "united states", "estados unidos", "usa" -> "🇺🇸"
            "mexico", "méxico" -> "🇲🇽"
            "brazil", "brasil" -> "🇧🇷"
            "qatar" -> "🇶🇦"
            "uae", "united arab emirates", "emirados árabes unidos" -> "🇦🇪"
            "japan", "japão" -> "🇯🇵"
            else -> "🏁"
        }
    }

    private fun formatarData(data: String): String {
        if (data.isBlank()) return ""

        val formatos = listOf(
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "dd/MM/yyyy"
        )

        for (formato in formatos) {
            try {
                val entrada = java.text.SimpleDateFormat(
                    formato,
                    Locale.getDefault()
                )
                entrada.isLenient = false

                val dataConvertida = entrada.parse(data)

                if (dataConvertida != null) {
                    val saida = java.text.SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    )
                    return saida.format(dataConvertida)
                }
            } catch (_: Exception) {
                // Tenta o próximo formato.
            }
        }

        return data
    }

    private fun adicionarTexto(
        layout: LinearLayout,
        texto: String,
        tamanho: Float,
        cor: Int,
        negrito: Boolean
    ) {
        val textoView = TextView(this).apply {
            text = texto
            textSize = tamanho
            setTextColor(cor)

            if (negrito) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }

        layout.addView(
            textoView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun criarFundoCard(
        corFundo: Int,
        corBorda: Int,
        raio: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(corFundo)
            setCornerRadius(dp(raio).toFloat())
            setStroke(dp(1), corBorda)
        }
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}