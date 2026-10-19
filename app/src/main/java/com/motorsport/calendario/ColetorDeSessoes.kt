package com.motorsport.calendario

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.time.OffsetDateTime

/**
 * Junta as sessões com horário confirmado de todas as categorias, usando os mesmos dados
 * que as telas usam (cache baixado ou JSON do APK). Não acessa a internet.
 */
internal object ColetorDeSessoes {

    // Mesmo arquivo gravado por F1CalendarApi.
    private const val ARQUIVO_CACHE_F1 = "f1_races_cache.json"

    private val camposF1 = listOf(
        "FirstPractice" to "Treino Livre 1",
        "SecondPractice" to "Treino Livre 2",
        "ThirdPractice" to "Treino Livre 3",
        "SprintQualifying" to "Classificação Sprint",
        "SprintShootout" to "Classificação Sprint",
        "Sprint" to "Corrida Sprint",
        "Qualifying" to "Classificação"
    )

    fun coletar(context: Context): List<SessaoParaAviso> =
        protegido("F1") { sessoesDaF1(context) } +
            protegido("F2") {
                F2CalendarJson.carregar(context).flatMap { evento ->
                    evento.sessoes.mapNotNull {
                        sessao("F2", evento.circuito, it.nome, it.data, it.horario, NomesDeEventos.nome("F2", evento.circuito))
                    }
                }
            } +
            protegido("F3") {
                F3CalendarJson.carregar(context).flatMap { evento ->
                    evento.sessoes.mapNotNull {
                        sessao("F3", evento.circuito, it.nome, it.data, it.horario, NomesDeEventos.nome("F3", evento.circuito))
                    }
                }
            } +
            protegido("IndyCar") {
                IndyCarCalendarJson.carregar(context).flatMap { evento ->
                    evento.sessoes.mapNotNull {
                        sessao("IndyCar", evento.circuito, it.nome, it.data, it.horario, evento.nome)
                    }
                }
            } +
            protegido("FormulaE") {
                FormulaECalendarJson.carregar(context).flatMap { evento ->
                    evento.sessoes.mapNotNull {
                        sessao("Formula E", evento.circuito, it.nome, it.data, it.horario, NomesDeEventos.nome("Formula E", evento.circuito))
                    }
                }
            } +
            protegido("Moto") {
                ClasseMoto.values().flatMap { classe ->
                    MotoCalendarJson.carregar(context, classe).flatMap { evento ->
                        evento.sessoes.mapNotNull {
                            sessao(classe.rotulo, evento.circuito, it.nome, it.data, it.horario, NomesDeEventos.nome(classe.rotulo, evento.circuito))
                        }
                    }
                }
            }

    private fun sessao(
        categoria: String,
        local: String,
        nome: String,
        data: String,
        horario: String,
        evento: String?
    ): SessaoParaAviso? {
        val inicio = PlanoDeAvisos.inicioDaSessao(data, horario) ?: return null
        return SessaoParaAviso(categoria, local, nome, inicio, evento.orEmpty())
    }

    /** Um problema em uma categoria não pode impedir os avisos das outras. */
    private fun protegido(origem: String, bloco: () -> List<SessaoParaAviso>): List<SessaoParaAviso> =
        try {
            bloco()
        } catch (e: Exception) {
            registrarFalha("ColetorDeSessoes.$origem", e)
            emptyList()
        }

    private fun sessoesDaF1(context: Context): List<SessaoParaAviso> {
        val arquivo = File(context.applicationContext.filesDir, ARQUIVO_CACHE_F1)
        if (!arquivo.isFile) return emptyList()
        return sessoesDaF1(arquivo.readText(Charsets.UTF_8))
    }

    /** Lê o JSON da API da F1 (Jolpica). Sessão sem data ou sem hora com fuso é ignorada. */
    internal fun sessoesDaF1(json: String): List<SessaoParaAviso> {
        val corridas = JSONObject(json)
            .getJSONObject("MRData")
            .getJSONObject("RaceTable")
            .getJSONArray("Races")

        val resultado = mutableListOf<SessaoParaAviso>()

        for (i in 0 until corridas.length()) {
            val corrida = corridas.optJSONObject(i) ?: continue

            val local = corrida.optJSONObject("Circuit")
                ?.optJSONObject("Location")
                ?.optString("locality", "")
                .orEmpty()
                .ifBlank { corrida.optString("raceName", "") }

            val circuito = corrida.optJSONObject("Circuit")
            val evento = NomesDeEventos.nome(
                "F1",
                circuito?.optJSONObject("Location")?.optString("locality", "").orEmpty(),
                circuito?.optString("circuitId", "").orEmpty(),
                circuito?.optJSONObject("Location")?.optString("country", "").orEmpty()
            ) ?: corrida.optString("raceName", "")

            for ((campo, nome) in camposF1) {
                val sessao = corrida.optJSONObject(campo) ?: continue
                sessaoDaF1(local, evento, nome, sessao)?.let { resultado.add(it) }
            }

            sessaoDaF1(local, evento, "Corrida", corrida)?.let { resultado.add(it) }
        }

        return resultado
    }

    private fun sessaoDaF1(local: String, evento: String, nome: String, objeto: JSONObject): SessaoParaAviso? {
        val data = objeto.optString("date", "")
        val hora = objeto.optString("time", "")

        if (data.isBlank() || hora.isBlank()) return null

        return try {
            val inicio = OffsetDateTime.parse("${data}T$hora")
                .atZoneSameInstant(PlanoDeAvisos.BRASILIA)

            SessaoParaAviso("F1", local, nome, inicio, evento)
        } catch (e: Exception) {
            registrarDescarte("ColetorDeSessoes.sessaoDaF1", e)
            null
        }
    }
}
