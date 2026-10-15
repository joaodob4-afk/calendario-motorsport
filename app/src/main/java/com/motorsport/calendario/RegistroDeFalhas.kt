package com.motorsport.calendario

import android.util.Log

private const val TAG = "Motorsport"

/**
 * Registra no Logcat uma falha tratada (rede, leitura de arquivo, JSON...), em vez de
 * engoli-la em silêncio. Não muda o comportamento do app: só deixa o motivo visível.
 */
internal fun registrarFalha(origem: String, erro: Throwable) {
    Log.w(TAG, "$origem falhou: ${erro.javaClass.simpleName}: ${erro.message}")
}

/**
 * Para dados inválidos ou ausentes que o app descarta de propósito (uma data mal formatada
 * num item, por exemplo). Usa nível de depuração para não poluir o Logcat.
 */
internal fun registrarDescarte(origem: String, erro: Throwable) {
    Log.d(TAG, "$origem descartou: ${erro.javaClass.simpleName}: ${erro.message}")
}
