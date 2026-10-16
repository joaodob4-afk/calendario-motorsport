package com.motorsport.calendario

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Recebe dois tipos de chamada:
 *  - o alarme de uma sessão: mostra a notificação;
 *  - reinício do aparelho, atualização do app ou mudança de hora/fuso: refaz os alarmes,
 *    que o Android apaga nesses casos.
 *
 * Depois de mostrar um aviso, os alarmes seguintes também são refeitos. Assim a fila se
 * renova sozinha, mesmo que o app fique semanas sem ser aberto.
 */
class AvisoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pendente = goAsync()

        Thread {
            try {
                if (intent.action == AgendadorDeAvisos.ACAO_AVISO) {
                    mostrarAviso(app, intent)
                }
                AgendadorDeAvisos.reagendar(app)
            } catch (e: Exception) {
                registrarFalha("AvisoReceiver.onReceive", e)
            } finally {
                pendente.finish()
            }
        }.start()
    }

    private fun mostrarAviso(context: Context, intent: Intent) {
        val categoria = intent.getStringExtra(AgendadorDeAvisos.EXTRA_CATEGORIA).orEmpty()
        val local = intent.getStringExtra(AgendadorDeAvisos.EXTRA_LOCAL).orEmpty()
        val nome = intent.getStringExtra(AgendadorDeAvisos.EXTRA_NOME).orEmpty()
        val inicioMs = intent.getLongExtra(AgendadorDeAvisos.EXTRA_INICIO_MS, 0L)
        val codigo = intent.getIntExtra(AgendadorDeAvisos.EXTRA_CODIGO, 0)

        if (inicioMs == 0L || nome.isBlank()) return

        val restanteMs = inicioMs - System.currentTimeMillis()
        if (!PlanoDeAvisos.aindaVale(restanteMs)) return

        val minutos = Math.round(restanteMs / 60_000.0)

        val titulo = if (categoria.isBlank()) nome else "$categoria · $nome"
        val antecedencia = PlanoDeAvisos.textoDeAntecedencia(minutos)
        val texto = if (local.isBlank()) antecedencia else "$antecedencia · $local"

        Notificacoes.mostrar(context, codigo, titulo, texto)
    }
}
