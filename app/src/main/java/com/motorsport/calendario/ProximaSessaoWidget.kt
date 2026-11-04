package com.motorsport.calendario

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Widget da tela inicial com a próxima sessão confirmada de todas as categorias. */
class ProximaSessaoWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val resultado = goAsync()
        Thread {
            try {
                atualizarAgora(context.applicationContext, appWidgetManager, appWidgetIds)
            } finally {
                resultado.finish()
            }
        }.start()
    }

    companion object {
        /** Atualiza os widgets existentes ao abrir o app ou depois de carregar novos calendários. */
        fun atualizar(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, ProximaSessaoWidget::class.java))
            if (ids.isEmpty()) return

            Thread {
                atualizarAgora(app, manager, ids)
            }.start()
        }

        /** Seleção pura, compartilhada com os testes: ignora sessões que já começaram. */
        internal fun selecionarProxima(
            sessoes: List<SessaoParaAviso>,
            agora: Instant
        ): SessaoParaAviso? =
            sessoes.asSequence()
                .filter { it.inicio.toInstant().isAfter(agora) }
                .minByOrNull { it.inicio.toInstant() }

        private fun atualizarAgora(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray
        ) {
            val agora = Instant.now()
            val proxima = try {
                selecionarProxima(ColetorDeSessoes.coletar(context), agora)
            } catch (e: Exception) {
                registrarFalha("ProximaSessaoWidget.atualizar", e)
                null
            }

            val abrirApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_proxima_sessao)
            views.setOnClickPendingIntent(R.id.widgetProximaSessao, abrirApp)
            views.setTextViewText(
                R.id.widgetTitulo,
                context.getString(R.string.widget_proxima_sessao_titulo)
            )

            if (proxima == null) {
                views.setTextViewText(
                    R.id.widgetEvento,
                    context.getString(R.string.widget_proxima_sessao_vazia)
                )
                views.setTextViewText(
                    R.id.widgetSessao,
                    context.getString(R.string.widget_proxima_sessao_vazia_dica)
                )
                views.setTextViewText(R.id.widgetData, "")
                views.setTextViewText(R.id.widgetContagem, "")
                views.setViewVisibility(R.id.widgetCategoria, android.view.View.GONE)
            } else {
                val local = proxima.inicio.withZoneSameInstant(ZoneId.systemDefault())
                val locale = Idioma.locale(context)
                val data = DateTimeFormatter.ofPattern("EEE, dd MMM", locale).format(local)
                val hora = Hora.deZonedDateTime(context, local)
                val evento = Idioma.evento(context, proxima.evento).ifBlank {
                    Idioma.lugar(context, proxima.local)
                }
                val nomeSessao = Idioma.sessao(context, proxima.nome)

                views.setViewVisibility(R.id.widgetCategoria, android.view.View.VISIBLE)
                views.setTextViewText(R.id.widgetCategoria, proxima.categoria.uppercase(locale))
                views.setTextViewText(R.id.widgetEvento, evento)
                views.setTextViewText(R.id.widgetSessao, nomeSessao)
                views.setTextViewText(R.id.widgetData, "$data · $hora")
                views.setTextViewText(
                    R.id.widgetContagem,
                    context.getString(
                        R.string.widget_comeca_em,
                        textoDaContagem(context, Duration.between(agora, proxima.inicio.toInstant()))
                    )
                )
            }

            views.setContentDescription(
                R.id.widgetProximaSessao,
                if (proxima == null) {
                    context.getString(R.string.widget_proxima_sessao_vazia)
                } else {
                    "${proxima.categoria}, ${proxima.evento}, ${proxima.nome}"
                }
            )
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun textoDaContagem(context: Context, duracao: Duration): String {
            val minutosTotais = duracao.toMinutes().coerceAtLeast(1)
            val dias = minutosTotais / (24 * 60)
            val horas = (minutosTotais % (24 * 60)) / 60
            val minutos = minutosTotais % 60

            return when {
                dias > 0 -> context.getString(R.string.widget_contagem_dias_horas, dias, horas)
                horas > 0 -> context.getString(R.string.widget_contagem_horas_minutos, horas, minutos)
                else -> context.getString(R.string.widget_contagem_minutos, minutos)
            }
        }
    }
}
