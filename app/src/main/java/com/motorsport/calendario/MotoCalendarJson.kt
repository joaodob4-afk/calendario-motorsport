package com.motorsport.calendario

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * Calendário de MotoGP, Moto2 e Moto3: JSON embutido no APK (funciona offline) e atualizado
 * pela internet, com a regra de [FonteDeCalendario]. O formato é o mesmo da F2 e da F3.
 */
internal object MotoCalendarJson {

    private const val URL_BASE =
        "https://raw.githubusercontent.com/joaodob4-afk/calendario-motorsport/main/app/src/main/assets/"

    private val formatoData = DateTimeFormatter
        .ofPattern("dd/MM/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    private val fontes = ClasseMoto.values().associateWith { classe ->
        FonteDeCalendario(
            nome = "MotoCalendarJson.${classe.rotulo}",
            urlJson = URL_BASE + classe.arquivoAsset,
            arquivoAsset = classe.arquivoAsset,
            arquivoCache = classe.arquivoCache
        ) { interpretar(it, classe) }
    }

    /** Cache baixado ou JSON do APK, o que for mais recente. Lista vazia se nenhum for válido. */
    fun carregar(context: Context, classe: ClasseMoto): List<MotoEvent> =
        fontes.getValue(classe).carregar(context)
            .map {
                // WEC: as corridas duram horas (Le Mans, 24h) e passam do dia da largada.
                if (classe == ClasseMoto.WEC) it.noFusoDoAparelhoMantendoDatas() else it.noFusoDoAparelho()
            }
            .sortedBy { it.etapa }

    /** Atualiza em segundo plano; um JSON inválido nunca substitui o cache atual. */
    fun atualizar(context: Context, classe: ClasseMoto, aoTerminar: (() -> Unit)? = null) =
        fontes.getValue(classe).atualizar(context, aoTerminar)

    /** Etapas das três classes, para a lista da tela inicial. */
    fun etapasDeTodas(context: Context): List<MainActivity.Etapa> =
        ClasseMoto.values().flatMap { comoEtapas(carregar(context, it), it) }

    fun comoEtapas(eventos: List<MotoEvent>, classe: ClasseMoto): List<MainActivity.Etapa> {
        val lista = mutableListOf<MainActivity.Etapa>()

        for (evento in eventos) {
            try {
                val inicio = LocalDate.parse(evento.inicio, formatoData)
                val fim = LocalDate.parse(evento.fim, formatoData)

                if (fim.isBefore(inicio)) continue

                lista.add(
                    MainActivity.Etapa(
                        categoria = classe.rotulo,
                        pais = evento.pais,
                        circuito = evento.circuito,
                        data = F1CalendarApi.formatarPeriodo(inicio, fim),
                        dataInicio = inicio.toString(),
                        dataFim = fim.toString(),
                        nomeEvento = evento.nomeEvento
                            .takeUnless { it.isBlank() || it == evento.circuito }
                            ?: NomesDeEventos.nome(classe.rotulo, evento.circuito, pais = evento.pais).orEmpty(),
                        local = evento.cidade
                    )
                )
            } catch (e: Exception) {
                registrarDescarte("MotoCalendarJson.comoEtapas", e)
                // Ignora somente a etapa cuja data é inválida.
            }
        }

        return lista.sortedBy { it.dataInicio }
    }

    /**
     * Rejeita calendários de outra categoria, vazios, com etapas repetidas, datas inválidas,
     * intervalos invertidos e sessões malformadas. Etapa sem sessões é aceita: os horários
     * costumam ser publicados perto da corrida e a tela avisa que ainda não estão disponíveis.
     */
    internal fun interpretar(json: String, classe: ClasseMoto): List<MotoEvent> {
        return try {
            val raiz = JSONObject(json)
            if (!raiz.optString("categoria").trim().equals(classe.rotulo, ignoreCase = true)) {
                return emptyList()
            }

            val etapas = raiz.getJSONArray("etapas")
            if (etapas.length() < 1) return emptyList()

            val eventos = mutableListOf<MotoEvent>()
            val numeros = mutableSetOf<Int>()

            for (i in 0 until etapas.length()) {
                val etapa = etapas.getJSONObject(i)

                val numero = etapa.getInt("etapa")
                val circuito = etapa.getString("circuito").trim()
                val nomeEvento = etapa.optString("nomeEvento", circuito).trim().ifBlank { circuito }
                val cidade = etapa.optString("cidade", circuito).trim().ifBlank { circuito }
                val pais = etapa.getString("pais").trim()
                val inicioTexto = etapa.getString("inicio")
                val fimTexto = etapa.getString("fim")

                if (numero <= 0) return emptyList()
                if (!numeros.add(numero)) return emptyList()
                if (circuito.isBlank() || pais.isBlank()) return emptyList()

                val inicio = LocalDate.parse(inicioTexto, formatoData)
                val fim = LocalDate.parse(fimTexto, formatoData)
                if (fim.isBefore(inicio)) return emptyList()

                val sessoes = mutableListOf<MotoSession>()
                val sessoesJson = etapa.optJSONArray("sessoes")

                if (sessoesJson != null) {
                    for (j in 0 until sessoesJson.length()) {
                        val sessao = sessoesJson.getJSONObject(j)

                        val nome = sessao.getString("nome").trim()
                        val data = sessao.getString("data")
                        val horario = sessao.optString("horario", "A confirmar")
                            .ifBlank { "A confirmar" }

                        if (nome.isBlank()) return emptyList()

                        // A data da sessão precisa ser válida.
                        LocalDate.parse(data, formatoData)

                        sessoes.add(MotoSession(nome = nome, data = data, horario = horario))
                    }
                }

                eventos.add(
                    MotoEvent(
                        etapa = numero,
                        nomeEvento = nomeEvento,
                        circuito = circuito,
                        cidade = cidade,
                        pais = pais,
                        inicio = inicioTexto,
                        fim = fimTexto,
                        sessoes = sessoes
                    )
                )
            }

            eventos.sortedBy { it.etapa }
        } catch (e: Exception) {
            registrarDescarte("MotoCalendarJson.interpretar", e)
            emptyList()
        }
    }
}
