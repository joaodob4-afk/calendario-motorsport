package com.motorsport.calendario

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

data class IndyCarSession(
    val nome: String,
    val data: String,
    val horario: String
)

data class IndyCarEvent(
    val etapa: Int,
    val nome: String,
    val circuito: String,
    val local: String,
    val pais: String,
    val inicio: String,
    val fim: String,
    val sessoes: List<IndyCarSession>
)

object IndyCarCalendarJson {

    private const val URL_JSON =
        "https://raw.githubusercontent.com/joaodob4-afk/" +
        "calendario-motorsport/main/app/src/main/assets/indycar_calendar.json"
    private const val MINIMO_ETAPAS_VALIDAS = 1
    private const val ARQUIVO_CACHE = "indycar_calendar_cache.json"

    private val formatoData: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT)

    private val fonte = FonteDeCalendario(
        nome = "IndyCarCalendarJson",
        urlJson = URL_JSON,
        arquivoAsset = "indycar_calendar.json",
        arquivoCache = ARQUIVO_CACHE,
        validarIndiceConfirmado = { texto -> JSONObject(texto).optBoolean("indice_confirmado", false) }
    ) { interpretar(it) }

    /** Cache baixado ou JSON do APK, o que for mais recente. Lista vazia se nenhum for válido. */
    fun carregar(context: Context): List<IndyCarEvent> =
        fonte.carregar(context).map { it.noFusoDoAparelho() }.sortedBy { it.etapa }

    /** Atualiza em segundo plano; um JSON inválido nunca substitui o cache atual. */
    fun atualizar(context: Context, aoTerminar: (() -> Unit)? = null) =
        fonte.atualizar(context, aoTerminar)

    /** Valida e grava no cache. Lança exceção se o calendário for inválido ou a gravação falhar. */
    internal fun gravarCacheSeValido(context: Context, texto: String) {
        interpretar(texto) // lança exceção se o calendário for inválido
        if (!fonte.gravarCache(context, texto)) {
            throw java.io.IOException("Não foi possível gravar o cache da IndyCar")
        }
    }

    /** Gravação atômica; o parâmetro [escrever] permite simular falhas nos testes. */
    internal fun gravarAtomicamente(
        atomic: AtomicFile,
        texto: String,
        escrever: (FileOutputStream, ByteArray) -> Unit = { saida, bytes -> saida.write(bytes) }
    ) = fonte.gravarAtomicamente(atomic, texto, escrever)

    /** Converte os eventos para o formato usado pela tela inicial. */
    fun comoEtapas(
        eventos: List<IndyCarEvent>
    ): List<MainActivity.Etapa> {
        val lista = mutableListOf<MainActivity.Etapa>()

        for (evento in eventos) {
            try {
                val inicio = LocalDate.parse(evento.inicio, formatoData)
                val fim = LocalDate.parse(evento.fim, formatoData)

                if (fim.isBefore(inicio)) continue

                val pais =
                    if (evento.pais == "Emirados Árabes Unidos") {
                        "Abu Dhabi"
                    } else {
                        evento.pais
                    }

                lista.add(
                    MainActivity.Etapa(
                        categoria = "IndyCar",
                        pais = pais,
                        circuito = evento.circuito,
                        data = F1CalendarApi.formatarPeriodo(inicio, fim),
                        dataInicio = inicio.toString(),
                        dataFim = fim.toString(),
                        nomeEvento = evento.nome,
                        local = evento.local
                    )
                )
            } catch (e: Exception) {
                registrarDescarte("IndyCarCalendarJson.comoEtapas", e)
                // Ignora apenas a etapa que não puder ser convertida.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }
    /**
     * Interpreta e valida o calendário inteiro.
     * Lança uma exceção se encontrar dados inválidos.
     */
    private fun interpretar(json: String): List<IndyCarEvent> {
        val raiz = JSONObject(json)

        require(raiz.optInt("temporada", 0) > 0) {
            "Temporada ausente ou inválida."
        }

        require(
            raiz.optString("categoria")
                .equals("IndyCar", ignoreCase = true)
        ) {
            "Categoria inválida."
        }

        val etapas = raiz.optJSONArray("etapas")
            ?: throw IllegalArgumentException("Lista de etapas ausente.")

        require(etapas.length() >= MINIMO_ETAPAS_VALIDAS) {
    "Calendário incompleto: apenas ${etapas.length()} etapas."
}
        val eventos = mutableListOf<IndyCarEvent>()
        val numerosEtapas = mutableSetOf<Int>()

        for (i in 0 until etapas.length()) {
            val etapaJson = etapas.getJSONObject(i)

            val numero = etapaJson.getInt("etapa")

            require(numero > 0) {
                "Número de etapa inválido."
            }

            require(numerosEtapas.add(numero)) {
                "Número de etapa duplicado: $numero."
            }

            val circuito = textoObrigatorio(etapaJson, "circuito")
            val nome = textoOpcional(etapaJson, "nome")
                .ifBlank { circuito }
            val local = textoOpcional(etapaJson, "local")
            val pais = textoObrigatorio(etapaJson, "pais")
            val inicioTexto = textoObrigatorio(etapaJson, "inicio")
            val fimTexto = textoObrigatorio(etapaJson, "fim")

            val inicio = analisarData(inicioTexto)
            val fim = analisarData(fimTexto)

            require(!fim.isBefore(inicio)) {
                "Data final anterior à inicial na etapa $numero."
            }

            val sessoesJson = etapaJson.optJSONArray("sessoes")
                ?: throw IllegalArgumentException(
                    "Sessões ausentes na etapa $numero."
                )

            require(sessoesJson.length() > 0) {
                "Etapa $numero sem sessões."
            }

            val sessoes = mutableListOf<IndyCarSession>()

            for (j in 0 until sessoesJson.length()) {
                val sessaoJson = sessoesJson.getJSONObject(j)

                val nomeSessao = textoObrigatorio(
                    sessaoJson,
                    "nome"
                )

                val dataSessaoTexto = textoObrigatorio(
                    sessaoJson,
                    "data"
                )

                val dataSessao = analisarData(dataSessaoTexto)

                require(
                    !dataSessao.isBefore(inicio) &&
                        !dataSessao.isAfter(fim)
                ) {
                    "Data de sessão fora do período da etapa $numero."
                }

                val horario = textoOpcional(
                    sessaoJson,
                    "horario"
                ).ifBlank { "A confirmar" }

                require(horarioValido(horario)) {
                    "Horário inválido na etapa $numero: $horario."
                }

                sessoes.add(
                    IndyCarSession(
                        nome = nomeSessao,
                        data = dataSessaoTexto,
                        horario = horario
                    )
                )
            }

            eventos.add(
                IndyCarEvent(
                    etapa = numero,
                    nome = nome,
                    circuito = circuito,
                    local = local,
                    pais = pais,
                    inicio = inicioTexto,
                    fim = fimTexto,
                    sessoes = sessoes
                )
            )
        }

        return eventos
    }

    private fun textoObrigatorio(
        objeto: JSONObject,
        campo: String
    ): String {
        val valor = objeto.optString(campo, "").trim()

        require(valor.isNotEmpty() && valor != "null") {
            "Campo obrigatório ausente ou vazio: $campo."
        }

        return valor
    }

    private fun textoOpcional(
        objeto: JSONObject,
        campo: String
    ): String {
        if (!objeto.has(campo) || objeto.isNull(campo)) {
            return ""
        }

        return objeto.optString(campo, "").trim()
    }

    private fun analisarData(texto: String): LocalDate {
        return LocalDate.parse(texto, formatoData)
    }

    private fun horarioValido(horario: String): Boolean {
        if (
            horario.equals("A confirmar", ignoreCase = true) ||
            horario.equals("TBA", ignoreCase = true)
        ) {
            return true
        }

        return Regex("""^(?:[01]\d|2[0-3]):[0-5]\d$""")
            .matches(horario)
    }
}