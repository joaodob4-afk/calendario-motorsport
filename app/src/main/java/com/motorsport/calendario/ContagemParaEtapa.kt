package com.motorsport.calendario

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal enum class UnidadeDeContagem { DIAS, SEMANAS, MESES }

/**
 * Quanto falta para uma etapa começar, no formato do card: até 6 dias, o número de dias;
 * a partir de 7, semanas ("1 semana", "2 semanas"...); a partir de 1 mês de calendário, meses.
 */
internal data class ContagemParaEtapa(val unidade: UnidadeDeContagem, val quantidade: Int) {

    companion object {
        /** [inicio] deve ser depois de [hoje]; se não for, conta como 1 dia. */
        fun entre(hoje: LocalDate, inicio: LocalDate): ContagemParaEtapa {
            val dias = ChronoUnit.DAYS.between(hoje, inicio).toInt().coerceAtLeast(1)
            val meses = ChronoUnit.MONTHS.between(hoje, inicio).toInt()

            return when {
                dias <= 6 -> ContagemParaEtapa(UnidadeDeContagem.DIAS, dias)
                meses >= 1 -> ContagemParaEtapa(UnidadeDeContagem.MESES, meses)
                else -> ContagemParaEtapa(UnidadeDeContagem.SEMANAS, dias / 7)
            }
        }
    }
}
