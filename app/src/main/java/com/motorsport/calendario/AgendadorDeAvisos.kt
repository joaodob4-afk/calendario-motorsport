package com.motorsport.calendario

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant

/**
 * Agenda no Android um alarme para cada sessão próxima, de acordo com a escolha feita
 * em [PreferenciasDeAviso]. Cada chamada de [reagendar] cancela os alarmes anteriores e
 * recria a lista inteira, então ela pode ser chamada sempre que algo mudar.
 */
internal object AgendadorDeAvisos {

    const val ACAO_AVISO = "com.motorsport.calendario.AVISO_SESSAO"
    const val EXTRA_CATEGORIA = "categoria"
    const val EXTRA_LOCAL = "local"
    const val EXTRA_NOME = "nome"
    const val EXTRA_INICIO_MS = "inicio_ms"
    const val EXTRA_CODIGO = "codigo"

    private const val ARQUIVO = "avisos_agendados"
    private const val CHAVE_CODIGOS = "codigos"

    private val trava = Any()

    /** Para chamar de telas: lê os calendários fora da thread principal. */
    fun reagendarEmSegundoPlano(context: Context) {
        val app = context.applicationContext

        Thread {
            try {
                reagendar(app)
            } catch (e: Exception) {
                registrarFalha("AgendadorDeAvisos.reagendar", e)
            }
        }.start()
    }

    fun reagendar(context: Context) {
        synchronized(trava) {
            val app = context.applicationContext
            val alarmes = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            cancelarAnteriores(app, alarmes)

            val minutos = PreferenciasDeAviso.minutosAntes(app)
            if (minutos == PreferenciasDeAviso.DESATIVADO || !Notificacoes.permitidas(app)) {
                guardarCodigos(app, emptySet())
                return
            }

            val avisos = PlanoDeAvisos.calcular(
                ColetorDeSessoes.coletar(app),
                Instant.now(),
                minutos,
                apenasCorridas = PreferenciasDeAviso.apenasCorridas(app)
            )

            val agendados = mutableSetOf<String>()

            for (aviso in avisos) {
                val sessao = aviso.sessao
                val intent = intentBase(app)
                    .putExtra(EXTRA_CATEGORIA, sessao.categoria)
                    .putExtra(EXTRA_LOCAL, sessao.local)
                    .putExtra(EXTRA_NOME, sessao.nome)
                    .putExtra(EXTRA_INICIO_MS, sessao.inicio.toInstant().toEpochMilli())
                    .putExtra(EXTRA_CODIGO, sessao.codigo)

                val pendente = PendingIntent.getBroadcast(
                    app,
                    sessao.codigo,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    // Dispara mesmo com o aparelho em economia de bateria. Pode atrasar
                    // alguns minutos, o que não atrapalha um aviso de 15 a 60 minutos.
                    alarmes.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        aviso.dispararEm.toEpochMilli(),
                        pendente
                    )
                    agendados.add(sessao.codigo.toString())
                } catch (e: Exception) {
                    registrarFalha("AgendadorDeAvisos.agendar", e)
                }
            }

            guardarCodigos(app, agendados)
        }
    }

    private fun intentBase(context: Context): Intent =
        Intent(context, AvisoReceiver::class.java).setAction(ACAO_AVISO)

    private fun cancelarAnteriores(context: Context, alarmes: AlarmManager) {
        for (codigo in lerCodigos(context)) {
            val numero = codigo.toIntOrNull() ?: continue

            val pendente = PendingIntent.getBroadcast(
                context,
                numero,
                intentBase(context),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: continue

            alarmes.cancel(pendente)
            pendente.cancel()
        }
    }

    private fun lerCodigos(context: Context): Set<String> =
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .getStringSet(CHAVE_CODIGOS, emptySet())
            .orEmpty()
            .toSet()

    private fun guardarCodigos(context: Context, codigos: Set<String>) {
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(CHAVE_CODIGOS, codigos)
            .apply()
    }
}
