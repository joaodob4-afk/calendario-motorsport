package com.motorsport.calendario

import android.content.Context
import org.json.JSONObject

object F2CalendarJson {

    fun carregar(context: Context): List<F2Event> {
        return try {
            val json = context.assets
                .open("f2_calendar.json")
                .bufferedReader()
                .use { it.readText() }

            val etapas = JSONObject(json).getJSONArray("etapas")
            val eventosJson = mutableListOf<F2Event>()

            for (i in 0 until etapas.length()) {
                val etapa = etapas.getJSONObject(i)
                val sessoesJson = etapa.getJSONArray("sessoes")
                val sessoes = mutableListOf<F2Session>()

                for (j in 0 until sessoesJson.length()) {
                    val sessao = sessoesJson.getJSONObject(j)

                    sessoes.add(
                        F2Session(
                            nome = sessao.getString("nome"),
                            data = sessao.getString("data"),
                            horario = sessao.optString(
                                "horario",
                                "A confirmar"
                            )
                        )
                    )
                }

                eventosJson.add(
                    F2Event(
                        etapa = etapa.getInt("etapa"),
                        circuito = etapa.getString("circuito"),
                        pais = etapa.getString("pais"),
                        inicio = etapa.getString("inicio"),
                        fim = etapa.getString("fim"),
                        sessoes = sessoes
                    )
                )
            }

            val eventosCombinados =
                F2Calendar.eventos
                    .filterNot { local ->
                        eventosJson.any {
                            it.etapa == local.etapa
                        }
                    } + eventosJson

            eventosCombinados.sortedBy { it.etapa }

        } catch (_: Exception) {
            F2Calendar.eventos
        }
    }
}