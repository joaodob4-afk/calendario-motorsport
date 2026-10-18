package com.motorsport.calendario

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

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
    const val EXTRA_NA_HORA = "na_hora_de_comecar"

    private const val ARQUIVO = "avisos_agendados"
    private const val CHAVE_CODIGOS = "codigos"

    private val trava = Any()

    /** Verdadeiro enquanto há um reagendamento esperando para rodar. */
    private val agendamentoPendente = AtomicBoolean(false)

    /** Espera que junta chamadas seguidas (vários calendários terminam de baixar juntos). */
    private const val ESPERA_PARA_JUNTAR_CHAMADAS_MS = 1500L

    /**
     * Para chamar de telas: lê os calendários fora da thread principal. Chamadas seguidas
     * viram uma só, feita depois de uma pequena espera e já com os dados mais novos.
     */
    fun reagendarEmSegundoPlano(context: Context) {
        if (!agendamentoPendente.compareAndSet(false, true)) return

        val app = context.applicationContext

        Thread {
            try {
                Thread.sleep(ESPERA_PARA_JUNTAR_CHAMADAS_MS)
                // Libera antes de rodar: um pedido que chegar durante o trabalho agenda outra rodada.
                agendamentoPendente.set(false)
                reagendar(app)
            } catch (e: Exception) {
                agendamentoPendente.set(false)
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

            val avisos = PlanoDeAvisos.calcularTodos(
                ColetorDeSessoes.coletar(app),
                Instant.now(),
                minutos,
                avisarNaHora = PreferenciasDeAviso.avisarNaHora(app),
                modoDaCategoria = { PreferenciasDeAviso.modoDaCategoria(app, it) }
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
                    .putExtra(EXTRA_NA_HORA, aviso.naHoraDeComecar)

                val pendente = PendingIntent.getBroadcast(
                    app,
                    aviso.codigoDoAlarme,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    agendarAlarme(alarmes, aviso.dispararEm.toEpochMilli(), pendente)
                    agendados.add(aviso.codigoDoAlarme.toString())
                } catch (e: Exception) {
                    registrarFalha("AgendadorDeAvisos.agendar", e)
                }
            }

            guardarCodigos(app, agendados)
        }
    }

    /**
     * Alarme exato e que dispara mesmo em modo de economia (Doze). Se o Android não permitir
     * alarmes exatos (Android 12/12L com a permissão desligada), cai para o alarme comum.
     */
    private fun agendarAlarme(alarmes: AlarmManager, quandoMs: Long, pendente: PendingIntent) {
        val exatoPermitido = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmes.canScheduleExactAlarms()

        if (exatoPermitido) {
            try {
                alarmes.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quandoMs, pendente)
                return
            } catch (e: SecurityException) {
                registrarFalha("AgendadorDeAvisos.alarmeExato", e)
            }
        }

        alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quandoMs, pendente)
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
