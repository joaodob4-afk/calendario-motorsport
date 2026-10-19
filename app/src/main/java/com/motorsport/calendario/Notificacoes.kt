package com.motorsport.calendario

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** Canal, permissão e exibição das notificações do app. */
internal object Notificacoes {

    const val CANAL_AVISOS = "avisos_sessoes"
    const val CODIGO_PEDIDO_PERMISSAO = 4101

    private const val CODIGO_TESTE = 1

    private const val ARQUIVO_PERMISSAO = "permissao_notificacoes"
    private const val CHAVE_JA_PEDIU = "ja_pediu"

    /** Falso se o usuário (ou o Android 13+, antes do primeiro pedido) bloqueou as notificações. */
    fun permitidas(context: Context): Boolean =
        NotificationManagerCompat.from(context.applicationContext).areNotificationsEnabled()

    /** A partir do Android 13 é preciso pedir permissão em tempo de uso. */
    fun precisaPedirPermissao(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    /**
     * Pede a permissão. Depois que o usuário nega duas vezes, o Android deixa de mostrar a
     * caixa de pedido e o toque não faria nada; nesse caso abrimos as configurações do app.
     */
    fun pedirPermissao(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val arquivo = activity.getSharedPreferences(ARQUIVO_PERMISSAO, Context.MODE_PRIVATE)
        val jaPediu = arquivo.getBoolean(CHAVE_JA_PEDIU, false)

        val negadoParaSempre = jaPediu &&
            !activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

        if (negadoParaSempre) {
            abrirConfiguracoes(activity)
            return
        }

        arquivo.edit().putBoolean(CHAVE_JA_PEDIU, true).apply()

        activity.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            CODIGO_PEDIDO_PERMISSAO
        )
    }

    /** Abre a tela de notificações do app, para quem negou a permissão. */
    fun abrirConfiguracoes(activity: Activity) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${activity.packageName}"))
        }

        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            registrarFalha("Notificacoes.abrirConfiguracoes", e)
        }
    }

    /**
     * Abre a lista de apps sem restrição de bateria. Em vários aparelhos (Xiaomi, Samsung,
     * Huawei, Motorola...) a economia de bateria encerra o app e apaga os alarmes.
     */
    fun abrirConfiguracoesDeBateria(activity: Activity) {
        val tentativas = listOf(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${activity.packageName}"))
        )

        for (intent in tentativas) {
            try {
                activity.startActivity(intent)
                return
            } catch (e: Exception) {
                registrarFalha("Notificacoes.abrirConfiguracoesDeBateria", e)
            }
        }
    }

    private fun criarCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val canal = NotificationChannel(
            CANAL_AVISOS,
            context.getString(R.string.canal_nome),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.canal_descricao)
        }

        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(canal)
    }

    fun mostrar(context: Context, codigo: Int, titulo: String, texto: String) {
        val app = context.applicationContext
        if (!permitidas(app)) return

        criarCanal(app)

        val abrirApp = PendingIntent.getActivity(
            app,
            0,
            Intent(app, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacao = NotificationCompat.Builder(app, CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setColor(app.getColor(R.color.acento))
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(abrirApp)
            .build()

        try {
            NotificationManagerCompat.from(app).notify(codigo, notificacao)
        } catch (e: SecurityException) {
            registrarFalha("Notificacoes.mostrar", e)
        }
    }

    /** Notificação de exemplo, para o usuário conferir que os avisos chegam. */
    fun mostrarTeste(context: Context) {
        mostrar(
            context,
            CODIGO_TESTE,
            context.getString(R.string.teste_titulo),
            context.getString(R.string.teste_texto)
        )
    }
}
