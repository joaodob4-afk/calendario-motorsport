package com.motorsport.calendario

import android.app.Application

/** Aplica o tema escolhido assim que o app abre, antes de qualquer tela aparecer. */
class AppMotorsport : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenciasDeTema.aplicar(this)
    }
}
