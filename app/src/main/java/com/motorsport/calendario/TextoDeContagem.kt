package com.motorsport.calendario

import android.content.Context
import java.time.LocalDate

/**
 * "HOJE", "3 DIAS", "1 SEMANA", "2 MESES": quanto falta para uma etapa começar. É o mesmo texto
 * nos cards da tela inicial e nas telas de detalhe.
 */
internal object TextoDeContagem {

    fun de(context: Context, inicio: LocalDate, hoje: LocalDate): String {
        if (!inicio.isAfter(hoje)) return context.getString(R.string.contador_hoje)

        val contagem = ContagemParaEtapa.entre(hoje, inicio)
        val n = contagem.quantidade

        val recurso = when (contagem.unidade) {
            UnidadeDeContagem.DIAS -> if (n == 1) R.string.contagem_dia else R.string.contagem_dias
            UnidadeDeContagem.SEMANAS -> if (n == 1) R.string.contagem_semana else R.string.contagem_semanas
            UnidadeDeContagem.MESES -> if (n == 1) R.string.contagem_mes else R.string.contagem_meses
        }

        return context.getString(recurso, n)
    }
}
