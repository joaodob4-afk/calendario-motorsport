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

class MotoActivity : AppCompatActivity() {

    // Visual igual ao da tela inicial: a cor de destaque é a da categoria (ver DetalheEstilo).
    private val estilo by lazy { DetalheEstilo(this, classe.rotulo) }
    private val preto: Int get() = estilo.fundo
    private val verde: Int get() = estilo.cor
    private val branco = Color.WHITE
    private val cinza: Int get() = estilo.textoSuave
    private val linhaCinza: Int get() = estilo.borda
    private val fundoCard: Int get() = estilo.superficie
    private val verdeEscuro: Int get() = estilo.borda
    private val fundoCorrida: Int get() = estilo.fundoCorrida

    private val idioma: Locale by lazy { Idioma.locale(this) }

    private lateinit var conteudo: LinearLayout

    private var etapaEhProxima = true

    /** Classe (MotoGP, Moto2 ou Moto3) recebida pela Intent. */
    private var classe = ClasseMoto.MOTOGP

    override fun onCreate(savedInstanceState: Bundle?) {
        // As telas de cada etapa ainda só têm o visual escuro: ficam escuras mesmo com o tema claro.
        delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        super.onCreate(savedInstanceState)

        classe = ClasseMoto.doIntent(intent)

        // Atualiza os detalhes assim que o calendário novo for salvo.
        MotoCalendarJson.atualizar(this, classe) {
            if (!isFinishing && !isDestroyed) {
                carregarEvento()
            }
        }

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

        carregarEvento()
    }

    private fun carregarEvento() {
        if (!::conteudo.isInitialized || isFinishing || isDestroyed) return

        conteudo.removeAllViews()

        // Carrega o JSON local e mantém a etapa selecionada pelo usuário.
        val eventos = MotoCalendarJson.carregar(this, classe)
        val proximaEtapa = obterProximaEtapa(eventos)
        val etapaEscolhida = intent.getIntExtra("ETAPA", -1)
        val evento = eventos.find { it.etapa == etapaEscolhida }
            ?: proximaEtapa

        etapaEhProxima = evento == null || evento == proximaEtapa

        if (evento != null) {
            mostrarEvento(evento)
        } else {
            adicionarTexto(
                getString(R.string.calendario_indisponivel),
                17f,
                verde,
                true
            )
            adicionarTexto(
                getString(R.string.sem_proxima_etapa_calendario),
                14f,
                cinza,
                false
            )
        }
    }

    private fun obterProximaEtapa(
        eventos: List<MotoEvent>
    ): MotoEvent? {
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

        raiz.addView(
            estilo.cabecalho(classe.tituloGrande, getString(R.string.cd_voltar)) {
                voltar()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
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
        aplicarInsetsDasBarras()

        // A janela e as barras do sistema seguem a cor do fundo, sem faixa mais escura.
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(preto))
        window.statusBarColor = preto
        window.navigationBarColor = preto
    }

    private fun voltar() {
        finish()

        overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }

    private fun mostrarEvento(evento: MotoEvent) {
        conteudo.removeAllViews()

        val nomeAutodromo = Autodromos.nome(evento.circuito)

        val inicio = formatarData(evento.inicio)
        val fim = formatarData(evento.fim)

        val intervalo = if (
            inicio.isNotBlank() && fim.isNotBlank()
        ) {
            "$inicio – $fim"
        } else {
            inicio.ifBlank { fim }
        }

        conteudo.addView(
            estilo.heroi(
                eyebrow = if (etapaEhProxima) getString(R.string.etapa_proxima) else getString(R.string.etapa),
                nome = NomesDeEventos.nome(classe.rotulo, evento.circuito)?.let { Idioma.evento(this, it) }
                    ?: classe.tituloGrande,
                circuito = nomeAutodromo.ifBlank { evento.circuito },
                bandeiraRes = obterRecursoBandeira(evento.pais),
                local = "${Idioma.lugar(this, evento.circuito)}, ${Idioma.lugar(this, evento.pais)}",
                periodo = intervalo.uppercase(idioma),
                contador = estilo.contador(converterData(evento.inicio), converterData(evento.fim))
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(18)
            }
        )

        adicionarTexto(
            getString(R.string.programacao),
            15f,
            verde,
            true
        ).apply {
            letterSpacing = 0.1f
            setPadding(0, dp(3), 0, dp(12))
        }

        if (evento.sessoes.isEmpty()) {
            adicionarTexto(
                getString(R.string.horarios_indisponiveis),
                14f,
                cinza,
                false
            )
            return
        }

        val sessoesOrdenadas = evento.sessoes.map { it.noFusoDoAparelho() }.sortedWith(
            compareBy<MotoSession> {
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
            text = data.uppercase(idioma)
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

    private fun adicionarSessao(sessao: MotoSession) {
        val nomeOriginal = sessao.nome.trim()
        val nomeNormalizado = nomeOriginal.lowercase(idioma)

        val corridaPrincipal =
            nomeNormalizado.contains("feature") ||
            nomeNormalizado.contains("corrida principal") ||
            nomeNormalizado == "corrida" ||
            nomeNormalizado == "race"

        val sprint = nomeNormalizado.contains("sprint")

        // Os nomes já vêm prontos no JSON (Treino Livre 1, Classificação 2, Warm-up...).
        val nomeExibido = Idioma.sessao(this, nomeOriginal).uppercase(idioma)

        val cartao = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))

            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()

                setColor(
                    if (corridaPrincipal) {
                        fundoCorrida
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
            .let {
                if (it.isBlank() || it.equals("a confirmar", ignoreCase = true)) {
                    getString(R.string.a_confirmar)
                } else {
                    Hora.deTexto(this, it)
                }
            }

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

    private fun obterRecursoBandeira(pais: String): Int = Bandeiras.recurso(this, pais)


    private fun formatarData(data: String): String {
        if (data.isBlank()) return ""

        val convertida = converterData(data) ?: return data

        return SimpleDateFormat(getString(R.string.formato_dia_mes), idioma).format(convertida)
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
                    idioma
                )
                parser.isLenient = false

                val resultado = parser.parse(data)

                if (resultado != null) {
                    return resultado
                }
            } catch (e: Exception) {
                registrarDescarte("MotoActivity.converterData", e)
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
        } catch (e: Exception) {
            registrarDescarte("MotoActivity.extrairMinutos", e)
            Int.MAX_VALUE
        }
    }

    private fun dp(valor: Int): Int {
        return (
            valor * resources.displayMetrics.density
        ).toInt()
    }
}