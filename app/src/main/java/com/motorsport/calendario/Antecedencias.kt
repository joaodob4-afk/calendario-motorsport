package com.motorsport.calendario

import android.content.Context

/** "15 min", "1 hora", "1h30", "2 h", "1 dia": texto curto de um botão de antecedência. */
internal fun rotuloCurtoDaAntecedencia(context: Context, minutos: Int): String =
    when {
        minutos == 60 -> context.getString(R.string.antecedencia_chip_hora)
        minutos == 1440 -> context.getString(R.string.antecedencia_chip_dia)
        minutos == 90 -> "1h30"
        minutos > 60 && minutos % 60 == 0 -> context.getString(R.string.antecedencia_chip_horas, minutos / 60)
        else -> "$minutos min"
    }

/** "30 minutos antes da sessão" / "2 horas antes da sessão": texto lido pelo leitor de tela. */
internal fun descricaoDaAntecedencia(context: Context, minutos: Int): String =
    when {
        minutos == 60 -> context.getString(R.string.antecedencia_hora)
        minutos == 1440 -> context.getString(R.string.antecedencia_dia)
        minutos > 60 && minutos % 60 == 0 -> context.getString(R.string.antecedencia_horas, minutos / 60)
        else -> context.getString(R.string.antecedencia_minutos, minutos)
    }
