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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class F2Activity : AppCompatActivity() {

    private val preto = Color.BLACK
    private val verde = Color.rgb(57, 255, 20)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(175, 175, 175)
    private val linhaCinza = Color.rgb(55, 55, 55)
    private val fundoCard = Color.rgb(15, 20, 15)
    private val verdeEscuro = Color.rgb(20, 75, 20)

    private val localePt = Locale("pt", "BR")

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
                    voltar()
                }
            }
        )

        montarTela()

        // Carrega o JSON e usa o calendário local como reserva.
        val eventos = F2CalendarJson.carregar(this)
        val evento = obterProximaEtapa(eventos)

        if (evento != null) {
            mostrarEvento(evento)
        } else {
            adicionarTexto(
                "CALENDÁRIO INDISPONÍVEL",
                17f,
                verde,
                true
            )

            adicionarTexto(
                "Não foi possível encontrar uma próxima etapa no calendário.",
                14f,
                cinza,
                false
            )
        }
    }

    private fun obterProximaEtapa(
        eventos: List<F2Event>
    ): F2Event? {
        val hoje = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.US
        ).format(Date())

        val dataHoje = converterData(hoje) ?: return null

        return eventos
            .filter { evento ->
                val dataFim = converterData(evento.fim)
                dataFim != null && !dataFim.before(dataHoje)
            }
            .sortedBy { evento ->
                converterData(evento.inicio)?.time
                    ?: Long.MAX_VALUE
            }
            .firstOrNull()
    }

    private fun montarTela() {
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
                voltar()
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
                bottomMargin = dp(14)
            }
        )

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            isVerticalFadingEdgeEnabled = false
        }

        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, dp(20))
        }

        scrollView.addView(conteudo)

        raiz.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(raiz)
    }

    private fun voltar() {
        finish()

        overridePendingTransition(
            android.R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }

    private fun mostrarEvento(evento: F2Event) {
        conteudo.removeAllViews()

        adicionarTexto(
            "PRÓXIMA ETAPA",
            12f,
            verde,
            true
        ).letterSpacing = 0.16f

        adicionarTexto(
            "FÓRMULA 2",
            28f,
            branco,
            true
        ).setPadding(0, dp(5), 0, dp(5))

        adicionarTexto(
            evento.circuito,
            12f,
            cinza,
            false
        ).setPadding(0, 0, 0, dp(12))

        val localLinha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val bandeira = ImageView(this).apply {
            val id = obterRecursoBandeira(evento.pais)

            if (id != 0) {
                setImageResource(id)
                scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                visibility = View.GONE
            }
        }

        localLinha.addView(
            bandeira,
            LinearLayout.LayoutParams(dp(34), dp(23)).apply {
                rightMargin = dp(9)
            }
        )

        val localTexto = TextView(this).apply {
            text = evento.pais
            textSize = 13f
            setTextColor(cinza)
        }

        localLinha.addView(
            localTexto,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        conteudo.addView(
            localLinha,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        )

        val inicio = formatarData(evento.inicio)
        val fim = formatarData(evento.fim)

        val intervalo = if (
            inicio.isNotBlank() && fim.isNotBlank()
        ) {
            "$inicio – $fim"
        } else {
            inicio.ifBlank { fim }
        }

        adicionarTexto(
            intervalo.uppercase(localePt),
            16f,
            branco,
            true
        ).setPadding(0, dp(1), 0, dp(15))

        adicionarSeparador()

        adicionarTexto(
            "PROGRAMAÇÃO",
            15f,
            verde,
            true
        ).apply {
            letterSpacing = 0.1f
            setPadding(0, dp(3), 0, dp(12))
        }

        if (evento.sessoes.isEmpty()) {
            adicionarTexto(
                "Os horários desta etapa ainda não estão disponíveis.",
                14f,
                cinza,
                false
            )
            return
        }

        val sessoesOrdenadas = evento.sessoes.sortedWith(
            compareBy<F2Session> {
                converterData(it.data)?.time ?: Long.MAX_VALUE
            }.thenBy {
                extrairMinutos(it.horario)
            }
        )

        var dataAtual: String? = null

        for (sessao in sessoesOrdenadas) {
            val dataSessao = formatarData(sessao.data)
                .ifBlank { sessao.data }

            if (dataSessao != dataAtual) {
                adicionarGrupoDia(dataSessao)
                dataAtual = dataSessao
            }

            adicionarSessao(sessao)
        }
    }

    private fun adicionarSeparador() {
        val separador = View(this).apply {
            setBackgroundColor(linhaCinza)
        }

        conteudo.addView(
            separador,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                bottomMargin = dp(16)
            }
        )
    }

    private fun adicionarGrupoDia(data: String) {
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val marcador = View(this).apply {
            setBackgroundColor(verde)
        }

        linha.addView(
            marcador,
            LinearLayout.LayoutParams(dp(3), dp(20)).apply {
                rightMargin = dp(9)
            }
        )

        val texto = TextView(this).apply {
            text = data.uppercase(localePt)
            textSize = 12f
            setTextColor(branco)
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.BOLD
            )
        }

        linha.addView(
            texto,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        conteudo.addView(
            linha,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(7)
                bottomMargin = dp(9)
            }
        )
    }

    private fun adicionarSessao(sessao: F2Session) {
        val nomeOriginal = sessao.nome.trim()
        val nomeNormalizado = nomeOriginal.lowercase(localePt)

        val corridaPrincipal =
            nomeNormalizado.contains("feature") ||
            nomeNormalizado.contains("corrida principal") ||
            nomeNormalizado == "corrida" ||
            nomeNormalizado == "race"

        val sprint = nomeNormalizado.contains("sprint")

        val nomeExibido = when {
            nomeNormalizado.contains("feature") ->
                "CORRIDA PRINCIPAL"

            nomeNormalizado.contains("practice") ||
            nomeNormalizado.contains("treino") ->
                "TREINO LIVRE"

            nomeNormalizado.contains("qualifying") ||
            nomeNormalizado.contains("classificação") ->
                "CLASSIFICAÇÃO"

            sprint ->
                "CORRIDA SPRINT"

            nomeNormalizado.contains("corrida") ||
            nomeNormalizado == "race" ->
                "CORRIDA"

            else -> nomeOriginal.uppercase(localePt)
        }

        val cartao = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))

            background = GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()

                setColor(
                    if (corridaPrincipal) {
                        Color.rgb(15, 45, 15)
                    } else {
                        fundoCard
                    }
                )

                setStroke(
                    dp(if (corridaPrincipal) 2 else 1),
                    if (corridaPrincipal) verde else verdeEscuro
                )
            }
        }

        val nome = TextView(this).apply {
            text = nomeExibido
            textSize = 13f
            setTextColor(
                if (corridaPrincipal) verde else branco
            )
            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
            letterSpacing = 0.02f
        }

        cartao.addView(
            nome,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val horarioTexto = sessao.horario
            .trim()
            .ifBlank { "A CONFIRMAR" }

        val horario = TextView(this).apply {
            text = horarioTexto
            textSize = 14f
            setTextColor(
                if (corridaPrincipal) verde else branco
            )
            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
            gravity = Gravity.END
        }

        cartao.addView(
            horario,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(10)
            }
        )

        conteudo.addView(
            cartao,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        )
    }

    private fun adicionarTexto(
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

        conteudo.addView(
            view,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return view
    }

    private fun obterRecursoBandeira(pais: String): Int {
        val nome = when (pais.trim().lowercase(localePt)) {
            "bahrain", "bahrein" -> "flag_bh"
            "saudi arabia", "arábia saudita" -> "flag_sa"
            "australia", "austrália" -> "flag_au"
            "japan", "japão" -> "flag_jp"
            "china" -> "flag_cn"
            "united states", "estados unidos", "usa" -> "flag_us"
            "canada", "canadá" -> "flag_ca"
            "monaco", "mônaco" -> "flag_mc"
            "spain", "espanha" -> "flag_es"
            "austria", "áustria" -> "flag_at"
            "united kingdom", "great britain", "reino unido" -> "flag_gb"
            "belgium", "bélgica" -> "flag_be"
            "hungary", "hungria" -> "flag_hu"
            "netherlands", "holanda", "países baixos" -> "flag_nl"
            "italy", "itália" -> "flag_it"
            "azerbaijan", "azerbaijão" -> "flag_az"
            "singapore", "singapura" -> "flag_sg"
            "mexico", "méxico" -> "flag_mx"
            "brazil", "brasil" -> "flag_br"
            "united arab emirates", "emirados árabes unidos" -> "flag_ae"
            "qatar" -> "flag_qa"
            "portugal" -> "flag_pt"
            "france", "frança" -> "flag_fr"
            "germany", "alemanha" -> "flag_de"
            else -> ""
        }

        if (nome.isBlank()) return 0

        return resources.getIdentifier(
            nome,
            "drawable",
            packageName
        )
    }

    private fun formatarData(data: String): String {
        if (data.isBlank()) return ""

        val convertida = converterData(data) ?: return data

        return SimpleDateFormat(
            "dd MMM",
            localePt
        ).format(convertida)
    }

    private fun converterData(data: String): Date? {
        val formatos = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy"
        )

        for (formato in formatos) {
            try {
                val parser = SimpleDateFormat(
                    formato,
                    localePt
                )
                parser.isLenient = false

                val resultado = parser.parse(data)

                if (resultado != null) {
                    return resultado
                }
            } catch (_: Exception) {
                // Tenta o próximo formato.
            }
        }

        return null
    }

    private fun extrairMinutos(horario: String): Int {
        val partes = horario.trim().split(":")

        if (partes.size < 2) return Int.MAX_VALUE

        return try {
            partes[0].toInt() * 60 + partes[1].take(2).toInt()
        } catch (_: Exception) {
            Int.MAX_VALUE
        }
    }

    private fun dp(valor: Int): Int {
        return (
            valor * resources.displayMetrics.density
        ).toInt()
    }
}
