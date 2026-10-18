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

        val naHora = intent.getBooleanExtra(AgendadorDeAvisos.EXTRA_NA_HORA, false)

        val restanteMs = inicioMs - System.currentTimeMillis()
        if (!PlanoDeAvisos.aindaVale(restanteMs)) return

        // No aviso da hora de começar o texto é sempre "Começando agora", mesmo que o
        // Android tenha atrasado o alarme em alguns segundos.
        val minutos = if (naHora) 0L else Math.round(restanteMs / 60_000.0)

        val nomeExibido = nomeParaExibir(context, nome)
        val titulo = if (categoria.isBlank()) nomeExibido else "$categoria · $nomeExibido"
        val antecedencia = textoDeAntecedencia(context, minutos)
        val texto = if (local.isBlank()) antecedencia else "$antecedencia · $local"

        Notificacoes.mostrar(context, codigo, titulo, texto)
    }

    /** Em inglês, traduz o nome da sessão só para exibir. */
    private fun nomeParaExibir(context: Context, nome: String): String =
        if (context.resources.getBoolean(R.bool.traduzir_sessoes)) {
            TraducaoDeSessoes.paraIngles(nome)
        } else {
            nome
        }

    /** "Começa em 30 min" / "Starts in 30 min", conforme o idioma do aparelho. */
    private fun textoDeAntecedencia(context: Context, minutos: Long): String {
        if (minutos <= 0L) return context.getString(R.string.aviso_comecando_agora)

        if (minutos < 60L) return context.getString(R.string.aviso_em_min, minutos.toInt())

        val horas = (minutos / 60L).toInt()
        val resto = (minutos % 60L).toInt()

        return if (resto == 0) {
            context.getString(R.string.aviso_em_h, horas)
        } else {
            context.getString(R.string.aviso_em_h_min, horas, resto)
        }
    }
}
