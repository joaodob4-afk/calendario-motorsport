package com.motorsport.calendario

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** As três escolhas de aparência. [chave] é o que fica salvo no aparelho. */
internal enum class ModoDeTema(val chave: String, val modoDoAppCompat: Int) {
    SISTEMA("sistema", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    CLARO("claro", AppCompatDelegate.MODE_NIGHT_NO),
    ESCURO("escuro", AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        /** Valor desconhecido ou ausente volta ao padrão do sistema. */
        fun deChave(chave: String?): ModoDeTema =
            values().firstOrNull { it.chave == chave } ?: SISTEMA
    }
}

/** Guarda e aplica o tema escolhido nas configurações (padrão: o do sistema). */
internal object PreferenciasDeTema {

    private const val ARQUIVO = "preferencias_tema"
    private const val CHAVE = "tema"

    fun atual(context: Context): ModoDeTema =
        ModoDeTema.deChave(
            context.applicationContext
                .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
                .getString(CHAVE, null)
        )

    /** Chamado ao abrir o app, antes de qualquer tela. */
    fun aplicar(context: Context) {
        AppCompatDelegate.setDefaultNightMode(atual(context).modoDoAppCompat)
    }

    /** Salva a escolha e aplica na hora: as telas abertas se refazem com as novas cores. */
    fun definir(context: Context, modo: ModoDeTema) {
        context.applicationContext
            .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .edit()
            .putString(CHAVE, modo.chave)
            .apply()

        AppCompatDelegate.setDefaultNightMode(modo.modoDoAppCompat)
    }
}
