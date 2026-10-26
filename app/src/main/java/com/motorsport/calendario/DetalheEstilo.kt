package com.motorsport.calendario

import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * Visual das telas de detalhe de corrida (F1, F2, F3, IndyCar, Fórmula E, Moto e WEC), igual
 * ao da tela inicial: fundo cinza-escuro, cartões neutros e a cor da categoria como destaque.
 *
 * Cada tela cria um [DetalheEstilo] com o nome da categoria e usa as cores e os blocos daqui,
 * então um ajuste de visual feito neste arquivo vale para todas elas.
 */
internal class DetalheEstilo(private val tela: Activity, val categoria: String) {

    val fundo: Int = tela.getColor(R.color.fundo_tela)
    val superficie: Int = tela.getColor(R.color.superficie)
    val borda: Int = tela.getColor(R.color.borda_suave)
    val textoSuave: Int = tela.getColor(R.color.texto_suave)
    val textoPrincipal: Int = tela.getColor(R.color.texto_principal)

    /** Cor da categoria; categorias fora da lista usam o verde padrão. */
    val cor: Int = Categorias.info(categoria)?.let { Color.parseColor(it.cor) }
        ?: tela.getColor(R.color.acento)

    /**
     * Cor da categoria para texto, linhas e detalhes sobre o fundo da tela. No tema claro as cores
     * vivas (amarelo, verde-água, cinza-claro) sumiriam sobre o fundo claro, então escurecem; no
     * escuro continuam como são. Fundos preenchidos com texto escuro por cima usam [cor].
     */
    val corDeDestaque: Int = run {
        val noturno = (tela.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        if (noturno) {
            cor
        } else {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(cor, hsl)
            hsl[2] = minOf(hsl[2], 0.38f)
            ColorUtils.HSLToColor(hsl)
        }
    }

    /** Fundo do cartão da corrida principal: a superfície com um tom da cor da categoria. */
    val fundoCorrida: Int = ColorUtils.blendARGB(superficie, cor, 0.20f)

    private fun dp(valor: Float): Int = (valor * tela.resources.displayMetrics.density).toInt()

    /**
     * Barra do topo: seta de voltar e o nome da categoria. Ela só ocupa a largura toda e encosta
     * no topo se a tela que a contém não tiver recuo (ver o padding da raiz nas telas de detalhe).
     */
    fun cabecalho(titulo: String, descricaoVoltar: String, aoVoltar: () -> Unit): View {
        val linha = LinearLayout(tela).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(tela.getColor(R.color.azul_marinho))
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
        }

        // Botão redondo (40dp visíveis numa área de toque de 48dp), igual ao da tela inicial.
        val voltar = ImageView(tela).apply {
            setImageResource(R.drawable.ic_seta_voltar)
            background = tela.getDrawable(R.drawable.bg_botao_icone)
            contentDescription = descricaoVoltar
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
            isClickable = true
            isFocusable = true
            setOnClickListener { aoVoltar() }
            comEfeitoDeToque(escala = 0.9f)
        }
        linha.addView(voltar, LinearLayout.LayoutParams(dp(48f), dp(48f)))

        val texto = TextView(tela).apply {
            text = titulo
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            letterSpacing = 0.06f
        }
        linha.addView(
            texto,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(8f) }
        )

        return linha
    }

    /**
     * Cartão de destaque do topo da página: selo e letreiro da categoria, nome da corrida,
     * circuito, bandeira e local, período e contagem regressiva.
     * [circuito] pode ser vazio; [bandeiraRes] 0 esconde a bandeira.
     */
    fun heroi(
        eyebrow: String,
        nome: String,
        circuito: String,
        bandeiraRes: Int,
        local: String,
        periodo: String,
        contador: String
    ): View {
        val card = LayoutInflater.from(tela).inflate(R.layout.item_destaque, null, false)

        // O layout é o mesmo da tela inicial, mas aqui o cartão não é clicável.
        card.isClickable = false
        card.isFocusable = false

        // Na tela inicial o espaço flexível iguala a altura dos cards do carrossel. Aqui ele
        // esticaria o cartão até o fim da tela, então vira um espaço fixo.
        card.findViewById<View>(R.id.destaqueEspaco).apply {
            layoutParams = (layoutParams as LinearLayout.LayoutParams).apply {
                height = dp(22f)
                weight = 0f
            }
        }
        card.minimumHeight = 0

        val raio = dp(20f).toFloat()

        card.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(ColorUtils.blendARGB(superficie, cor, 0.22f), fundo)
        ).apply {
            cornerRadius = raio
            setStroke(dp(1f).coerceAtLeast(1), borda)
        }
        card.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, raio)
            }
        }
        card.clipToOutline = true

        val rotulo = Categorias.rotuloDoSelo(categoria)

        card.findViewById<TextView>(R.id.destaqueBadge).apply {
            text = rotulo
            setTextColor(Color.parseColor("#0B0D0F"))
            background = GradientDrawable().apply {
                setColor(cor)
                cornerRadius = dp(9f).toFloat()
            }
        }

        card.findViewById<TextView>(R.id.destaqueMarca).apply {
            text = rotulo
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                when {
                    rotulo.length <= 3 -> 110f
                    rotulo.length <= 6 -> 68f
                    else -> 44f
                }
            )
            setTextColor(
                Color.argb(38, Color.red(cor), Color.green(cor), Color.blue(cor))
            )
        }

        card.findViewById<TextView>(R.id.destaqueTitulo).text = eyebrow

        card.findViewById<TextView>(R.id.destaqueNome).apply {
            text = nome
            // Nomes longos (ex.: IndyCar) usam letra menor e podem ocupar mais linhas.
            textSize = if (nome.length > 26) 20f else 24f
            maxLines = 5
        }

        card.findViewById<TextView>(R.id.destaqueCircuito).apply {
            if (circuito.isNotBlank()) {
                text = circuito
                visibility = View.VISIBLE
            }
        }

        val bandeira = card.findViewById<ImageView>(R.id.destaqueBandeira)
        if (bandeiraRes != 0) {
            bandeira.setImageResource(bandeiraRes)
            bandeira.visibility = View.VISIBLE
            val r = dp(4f).toFloat()
            bandeira.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, r)
                }
            }
            bandeira.clipToOutline = true
            bandeira.foreground = tela.getDrawable(R.drawable.borda_bandeira)
        }

        card.findViewById<TextView>(R.id.destaqueLocal).text = local
        card.findViewById<TextView>(R.id.destaqueData).apply {
            text = periodo
            // Letra menor e até duas linhas: a data nunca fica escondida pelo selo da contagem.
            textSize = 14f
            letterSpacing = 0.02f
        }

        card.findViewById<TextView>(R.id.destaqueStatus).apply {
            text = contador
            setTextColor(Color.parseColor("#0B0D0F"))
            background = GradientDrawable().apply {
                setColor(cor)
                cornerRadius = dp(10f).toFloat()
            }
        }

        card.contentDescription = "$categoria, $nome, $local, $periodo, $contador"

        return card
    }

    /** Texto do selo do cartão: HOJE, AMANHÃ, "N DIAS", EM ANDAMENTO ou FINALIZADA. */
    fun contador(inicio: LocalDate?, fim: LocalDate?): String {
        if (inicio == null) return tela.getString(R.string.status_agendada)

        val hoje = LocalDate.now()
        if ((fim ?: inicio).isBefore(hoje)) return tela.getString(R.string.status_finalizada)

        return if (inicio.isBefore(hoje)) {
            tela.getString(R.string.status_em_andamento)
        } else {
            TextoDeContagem.de(tela, inicio, hoje)
        }
    }

    /** Mesmo que [contador], para telas que guardam as datas como [Date]. */
    fun contador(inicio: Date?, fim: Date?): String {
        val zona = ZoneId.systemDefault()
        return contador(
            inicio?.toInstant()?.atZone(zona)?.toLocalDate(),
            fim?.toInstant()?.atZone(zona)?.toLocalDate()
        )
    }
}
