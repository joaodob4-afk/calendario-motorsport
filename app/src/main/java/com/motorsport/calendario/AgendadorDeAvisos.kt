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
    const val EXTRA_EVENTO = "evento"
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

            // Não apague a fila atual antes de conseguir montar a nova. Se a leitura dos
            // calendários ou o cálculo falhar, os alarmes já registrados continuam válidos.
            val codigosAnteriores = lerCodigos(app)

            val minutos = PreferenciasDeAviso.minutosSelecionados(app)
            val naHora = PreferenciasDeAviso.avisarNaHora(app)

            // Sem nenhuma antecedência e sem o aviso da hora de começar, não há o que agendar.
            if ((minutos.isEmpty() && !naHora) || !Notificacoes.permitidas(app)) {
                cancelarCodigos(app, alarmes, codigosAnteriores)
                guardarCodigos(app, emptySet())
                return
            }

            val coleta = ColetorDeSessoes.coletarComResultado(app)
            if (coleta.houveFalha) {
                // Continua agendando as categorias lidas corretamente, mas não remove alarmes
                // anteriores: eles podem pertencer justamente a uma categoria com erro.
                registrarFalha(
                    "AgendadorDeAvisos.coletaIncompleta",
                    IllegalStateException("Falha ao ler um ou mais calendários; alarmes anteriores serão preservados")
                )
            }

            val agora = Instant.now()

            val avisos = PlanoDeAvisos.comAlarmeNoFuturo(
                PlanoDeAvisos.calcularTodos(
                    coleta.sessoes,
                    agora,
                    minutos,
                    avisarNaHora = naHora,
                    modoDaCategoria = { PreferenciasDeAviso.modoDaCategoria(app, it) }
                ),
                agora
            )

            val agendados = mutableSetOf<String>()
            var houveFalha = false

            // Primeiro registre a nova fila. Só depois remova os alarmes antigos; assim uma
            // falha pontual do AlarmManager não deixa o app sem nenhum aviso futuro.
            for (aviso in avisos) {
                val sessao = aviso.sessao
                val intent = intentBase(app)
                    .putExtra(EXTRA_CATEGORIA, sessao.categoria)
                    .putExtra(EXTRA_LOCAL, sessao.local)
                    .putExtra(EXTRA_NOME, sessao.nome)
                    .putExtra(EXTRA_EVENTO, sessao.evento)
                    .putExtra(EXTRA_INICIO_MS, sessao.inicio.toInstant().toEpochMilli())
                    // Id da notificação = id do alarme: cada aviso (1h, 30 min, início...) vira uma
                    // notificação própria, em vez de substituir a anterior da mesma sessão.
                    .putExtra(EXTRA_CODIGO, aviso.codigoDoAlarme)
                    .putExtra(EXTRA_NA_HORA, aviso.naHoraDeComecar)

                val pendente = PendingIntent.getBroadcast(
                    app,
                    aviso.codigoDoAlarme,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    agendarAlarme(alarmes, PlanoDeAvisos.momentoDoAlarme(aviso).toEpochMilli(), pendente)
                    agendados.add(aviso.codigoDoAlarme.toString())
                } catch (e: Exception) {
                    houveFalha = true
                    registrarFalha("AgendadorDeAvisos.agendar", e)
                }
            }

            if (houveFalha || coleta.houveFalha) {
                // Mantém alarmes antigos em caso de falha parcial e registra os novos que deram certo.
                guardarCodigos(app, codigosAnteriores + agendados)
                return
            }

            cancelarCodigos(app, alarmes, codigosAnteriores - agendados)
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

    private fun cancelarCodigos(context: Context, alarmes: AlarmManager, codigos: Set<String>) {
        for (codigo in codigos) {
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